package it.university.etfpac.service;

import it.university.etfpac.dto.request.SimulationRequest;
import it.university.etfpac.dto.response.SimulationResponse;
import it.university.etfpac.entity.*;
import it.university.etfpac.exception.BadRequestException;
import it.university.etfpac.exception.ResourceNotFoundException;
import it.university.etfpac.exception.SimulationException;
import it.university.etfpac.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class SimulationService {

    private final SimulationRepository simulationRepository;
    private final UserRepository userRepository;
    private final ETFRepository etfRepository;
    private final SimulationAllocationRepository allocationRepository;
    private final SimulationDataRepository dataRepository;
    private final UserService userService;
    private final SimulationEngine simulationEngine;

    public SimulationResponse createSimulation(SimulationRequest request) {
        log.info("Creazione nuova simulazione: {}", request.getName());

        // Validazione
        validateSimulationRequest(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        // Creazione simulazione
        Simulation simulation = new Simulation();
        simulation.setUser(user);
        simulation.setName(request.getName());
        simulation.setInitialAmount(request.getInitialAmount());
        simulation.setMonthlyAmount(request.getMonthlyAmount());
        simulation.setInvestmentPeriod(request.getInvestmentPeriod());
        simulation.setFrequency(Simulation.Frequency.valueOf(request.getFrequency().toUpperCase()));
        simulation.setStrategy(Simulation.Strategy.valueOf(request.getStrategy().toUpperCase()));
        simulation.setRiskTolerance(Simulation.RiskTolerance.valueOf(request.getRiskTolerance().toUpperCase()));
        simulation.setRebalanceFrequency(Simulation.RebalanceFrequency.valueOf(request.getRebalanceFrequency().toUpperCase()));
        simulation.setAutomaticRebalance(request.getAutomaticRebalance());
        simulation.setStopLoss(request.getStopLoss());
        simulation.setTakeProfitTarget(request.getTakeProfitTarget());
        simulation.setStatus(Simulation.SimulationStatus.PENDING);

        Simulation savedSimulation = simulationRepository.save(simulation);

        // Salva allocazioni
        saveAllocations(savedSimulation, request.getEtfAllocation());

        // Incrementa contatore simulazioni attive
        userService.incrementActiveSimulations(user.getId());

        log.info("Simulazione creata con ID: {}", savedSimulation.getId());

        return convertToResponse(savedSimulation);
    }

    @Async("simulationExecutor")
    public void runSimulation(Long simulationId) {
        log.info("Avvio simulazione asincrona con ID: {}", simulationId);

        try {
            Simulation simulation = simulationRepository.findById(simulationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

            simulation.setStatus(Simulation.SimulationStatus.RUNNING);
            simulationRepository.save(simulation);

            // Esegui simulazione
            List<SimulationData> simulationData = simulationEngine.runSimulation(simulation);

            // Salva dati simulazione
            dataRepository.saveAll(simulationData);

            // Calcola metriche finali
            SimulationResults results = simulationEngine.calculateResults(simulationData);

            // Aggiorna simulazione con risultati
            simulation.setFinalValue(results.getFinalValue());
            simulation.setTotalInvested(results.getTotalInvested());
            simulation.setCumulativeReturn(results.getCumulativeReturn());
            simulation.setVolatility(results.getVolatility());
            simulation.setMaxDrawdown(results.getMaxDrawdown());
            simulation.setSharpeRatio(results.getSharpeRatio());
            simulation.setWinRate(results.getWinRate());
            simulation.setStatus(Simulation.SimulationStatus.COMPLETED);

            simulationRepository.save(simulation);

            log.info("Simulazione completata con successo per ID: {}", simulationId);

        } catch (Exception e) {
            log.error("Errore durante l'esecuzione della simulazione ID: {}", simulationId, e);

            Simulation simulation = simulationRepository.findById(simulationId).orElse(null);
            if (simulation != null) {
                simulation.setStatus(Simulation.SimulationStatus.FAILED);
                simulationRepository.save(simulation);
            }

            throw new SimulationException("Errore nell'esecuzione della simulazione", e);
        }
    }

    @Transactional(readOnly = true)
    public SimulationResponse getSimulationById(Long id) {
        log.info("Recupero simulazione con ID: {}", id);

        Simulation simulation = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata con ID: " + id));

        return convertToResponse(simulation);
    }

    @Transactional(readOnly = true)
    public List<SimulationResponse> getUserSimulations(Long userId) {
        log.info("Recupero simulazioni per utente ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return simulationRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<SimulationResponse> getUserSimulationsPaged(Long userId, Pageable pageable) {
        log.info("Recupero simulazioni paginate per utente ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return simulationRepository.findByUser(user, pageable)
                .map(this::convertToResponse);
    }

    public void deleteSimulation(Long id) {
        log.info("Eliminazione simulazione con ID: {}", id);

        Simulation simulation = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        // Elimina dati correlati
        allocationRepository.deleteBySimulation(simulation);
        dataRepository.deleteBySimulation(simulation);

        // Decrementa contatore simulazioni attive
        userService.decrementActiveSimulations(simulation.getUser().getId());

        simulationRepository.delete(simulation);
        log.info("Simulazione eliminata con successo");
    }

    @Transactional(readOnly = true)
    public List<SimulationResponse> getBestPerformingSimulations(Long userId) {
        log.info("Recupero migliori simulazioni per utente ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return simulationRepository.findBestPerformingSimulations(user).stream()
                .limit(5)
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    private void validateSimulationRequest(SimulationRequest request) {
        // Validazione allocazioni
        double totalAllocation = request.getEtfAllocation().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (Math.abs(totalAllocation - 100.0) > 0.01) {
            throw new BadRequestException("La somma delle allocazioni deve essere 100%");
        }

        // Validazione ETF esistenti
        for (String etfId : request.getEtfAllocation().keySet()) {
            if (!etfRepository.existsById(etfId)) {
                throw new BadRequestException("ETF non trovato: " + etfId);
            }
        }
    }

    private void saveAllocations(Simulation simulation, Map<String, Double> allocations) {
        List<SimulationAllocation> allocationEntities = new ArrayList<>();

        for (Map.Entry<String, Double> entry : allocations.entrySet()) {
            if (entry.getValue() > 0) {
                ETF etf = etfRepository.findById(entry.getKey())
                        .orElseThrow(() -> new ResourceNotFoundException("ETF non trovato: " + entry.getKey()));

                SimulationAllocation allocation = new SimulationAllocation();
                allocation.setSimulation(simulation);
                allocation.setEtf(etf);
                allocation.setPercentage(entry.getValue());

                allocationEntities.add(allocation);
            }
        }

        allocationRepository.saveAll(allocationEntities);
    }

    private SimulationResponse convertToResponse(Simulation simulation) {
        // Recupera allocazioni
        List<SimulationAllocation> allocations = allocationRepository.findBySimulation(simulation);
        Map<String, Double> etfAllocation = allocations.stream()
                .collect(Collectors.toMap(
                        alloc -> alloc.getEtf().getId(),
                        SimulationAllocation::getPercentage
                ));

        // Recupera dati simulazione se completata
        List<SimulationResponse.SimulationDataPoint> simulationData = null;
        if (simulation.getStatus() == Simulation.SimulationStatus.COMPLETED) {
            List<SimulationData> dataPoints = dataRepository.findBySimulationOrderByMonth(simulation);
            simulationData = dataPoints.stream()
                    .map(data -> SimulationResponse.SimulationDataPoint.builder()
                            .month(data.getMonth())
                            .totalValue(data.getTotalValue())
                            .totalInvested(data.getTotalInvested())
                            .monthlyInvestment(data.getMonthlyInvestment())
                            .monthlyReturn(data.getMonthlyReturn())
                            .cumulativeReturn(data.getCumulativeReturn())
                            .inflationAdjustedValue(data.getInflationAdjustedValue())
                            .sharpeRatio(data.getSharpeRatio())
                            .build())
                    .collect(Collectors.toList());
        }

        return SimulationResponse.builder()
                .id(simulation.getId())
                .name(simulation.getName())
                .initialAmount(simulation.getInitialAmount())
                .monthlyAmount(simulation.getMonthlyAmount())
                .investmentPeriod(simulation.getInvestmentPeriod())
                .frequency(simulation.getFrequency().name().toLowerCase())
                .strategy(simulation.getStrategy().name().toLowerCase())
                .etfAllocation(etfAllocation)
                .riskTolerance(simulation.getRiskTolerance().name().toLowerCase())
                .rebalanceFrequency(simulation.getRebalanceFrequency().name().toLowerCase())
                .automaticRebalance(simulation.getAutomaticRebalance())
                .stopLoss(simulation.getStopLoss())
                .takeProfitTarget(simulation.getTakeProfitTarget())
                .status(simulation.getStatus().name().toLowerCase())
                .finalValue(simulation.getFinalValue())
                .totalInvested(simulation.getTotalInvested())
                .cumulativeReturn(simulation.getCumulativeReturn())
                .volatility(simulation.getVolatility())
                .maxDrawdown(simulation.getMaxDrawdown())
                .sharpeRatio(simulation.getSharpeRatio())
                .winRate(simulation.getWinRate())
                .simulationData(simulationData)
                .createdAt(simulation.getCreatedAt())
                .updatedAt(simulation.getUpdatedAt())
                .build();
    }
}