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
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.CompletableFuture;
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
    private final SimulationEngine simulationEngine;

    /**
     * Esegue una simulazione diretta senza salvarla nel database
     */
    public Map<String, Object> runSimulationDirect(SimulationRequest request) {
        log.info("Esecuzione diretta simulazione");

        // Validazione
        validateSimulationRequest(request);

        // Crea simulazione temporanea (non salvata)
        Simulation tempSimulation = new Simulation();
        tempSimulation.setName(request.getName());
        tempSimulation.setInitialAmount(request.getInitialAmount());
        tempSimulation.setMonthlyAmount(request.getMonthlyAmount());
        tempSimulation.setInvestmentPeriod(request.getInvestmentPeriod());
        tempSimulation.setFrequency(Simulation.Frequency.valueOf(request.getFrequency().toUpperCase()));
        tempSimulation.setStrategy(Simulation.Strategy.valueOf(request.getStrategy().toUpperCase()));
        tempSimulation.setRiskTolerance(Simulation.RiskTolerance.valueOf(request.getRiskTolerance().toUpperCase()));
        tempSimulation.setRebalanceFrequency(Simulation.RebalanceFrequency.valueOf(request.getRebalanceFrequency().toUpperCase()));
        tempSimulation.setAutomaticRebalance(request.getAutomaticRebalance());
        tempSimulation.setStopLoss(request.getStopLoss());
        tempSimulation.setTakeProfitTarget(request.getTakeProfitTarget());

        // Crea allocazioni temporanee
        List<SimulationAllocation> tempAllocations = new ArrayList<>();
        for (Map.Entry<String, Double> entry : request.getEtfAllocation().entrySet()) {
            if (entry.getValue() > 0) {
                ETF etf = etfRepository.findById(entry.getKey())
                        .orElseThrow(() -> new ResourceNotFoundException("ETF non trovato: " + entry.getKey()));

                SimulationAllocation allocation = new SimulationAllocation();
                allocation.setEtf(etf);
                allocation.setPercentage(entry.getValue());
                tempAllocations.add(allocation);
            }
        }

        // Esegui simulazione usando il motore
        List<SimulationData> simulationData = simulationEngine.runSimulationWithAllocations(tempSimulation, tempAllocations);

        // Calcola risultati
        SimulationResults results = simulationEngine.calculateResults(simulationData);

        Map<String, Object> response = new HashMap<>();
        response.put("simulationData", simulationData.stream()
                .map(data -> {
                    Map<String, Object> dataPoint = new HashMap<>();
                    dataPoint.put("month", data.getMonth());
                    dataPoint.put("totalValue", data.getTotalValue());
                    dataPoint.put("totalInvested", data.getTotalInvested());
                    dataPoint.put("monthlyReturn", data.getMonthlyReturn());
                    dataPoint.put("cumulativeReturn", data.getCumulativeReturn());
                    dataPoint.put("monthlyInvestment", data.getMonthlyInvestment());
                    dataPoint.put("inflationAdjustedValue", data.getInflationAdjustedValue());
                    return dataPoint;
                })
                .collect(Collectors.toList()));

        // Crea la mappa dei risultati usando HashMap invece di Map.of()
        Map<String, Object> resultsMap = new HashMap<>();
        resultsMap.put("finalValue", results.getFinalValue());
        resultsMap.put("totalInvested", results.getTotalInvested());
        resultsMap.put("cumulativeReturn", results.getCumulativeReturn());
        resultsMap.put("volatility", results.getVolatility());
        resultsMap.put("maxDrawdown", results.getMaxDrawdown());
        resultsMap.put("sharpeRatio", results.getSharpeRatio());
        resultsMap.put("winRate", results.getWinRate());
        resultsMap.put("annualizedReturn", calculateAnnualizedReturn(simulationData));
        resultsMap.put("bestMonth", calculateBestMonth(simulationData));
        resultsMap.put("worstMonth", calculateWorstMonth(simulationData));
        resultsMap.put("consistency", calculateConsistency(simulationData));
        resultsMap.put("calmarRatio", calculateCalmarRatio(results));

        response.put("results", resultsMap);

        return response;
    }

    /**
     * Crea e salva una nuova simulazione
     */
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

        log.info("Simulazione creata con ID: {}", savedSimulation.getId());

        // Esegui simulazione automaticamente
        runSimulation(savedSimulation.getId());

        return convertToResponse(savedSimulation);
    }

    /**
     * Esegue una simulazione già salvata
     */
    @Async("simulationExecutor")
    public CompletableFuture<Void> runSimulation(Long simulationId) {
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
            return CompletableFuture.completedFuture(null);

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

    /**
     * Recupera una simulazione per ID
     */
    @Transactional(readOnly = true)
    public SimulationResponse getSimulationById(Long id) {
        log.info("Recupero simulazione con ID: {}", id);

        Simulation simulation = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata con ID: " + id));

        return convertToResponse(simulation);
    }

    /**
     * Recupera tutte le simulazioni
     */
    @Transactional(readOnly = true)
    public List<SimulationResponse> getAllSimulations() {
        log.info("Recupero tutte le simulazioni");

        return simulationRepository.findAll().stream()
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Elimina una simulazione
     */
    public void deleteSimulation(Long id) {
        log.info("Eliminazione simulazione con ID: {}", id);

        Simulation simulation = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        // Elimina dati correlati
        allocationRepository.deleteBySimulation(simulation);
        dataRepository.deleteBySimulation(simulation);

        simulationRepository.delete(simulation);
        log.info("Simulazione eliminata con successo");
    }

    /**
     * Clona una simulazione esistente
     */
    public SimulationResponse cloneSimulation(Long id, String newName) {
        log.info("Clonazione simulazione ID: {} con nome: {}", id, newName);

        Simulation original = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione originale non trovata"));

        // Crea copia
        Simulation clone = new Simulation();
        clone.setUser(original.getUser());
        clone.setName(newName != null ? newName : original.getName() + " - Copia");
        clone.setInitialAmount(original.getInitialAmount());
        clone.setMonthlyAmount(original.getMonthlyAmount());
        clone.setInvestmentPeriod(original.getInvestmentPeriod());
        clone.setFrequency(original.getFrequency());
        clone.setStrategy(original.getStrategy());
        clone.setRiskTolerance(original.getRiskTolerance());
        clone.setRebalanceFrequency(original.getRebalanceFrequency());
        clone.setAutomaticRebalance(original.getAutomaticRebalance());
        clone.setStopLoss(original.getStopLoss());
        clone.setTakeProfitTarget(original.getTakeProfitTarget());
        clone.setStatus(Simulation.SimulationStatus.PENDING);

        Simulation savedClone = simulationRepository.save(clone);

        // Copia allocazioni
        List<SimulationAllocation> originalAllocations = allocationRepository.findBySimulation(original);
        List<SimulationAllocation> clonedAllocations = originalAllocations.stream()
                .map(alloc -> {
                    SimulationAllocation newAlloc = new SimulationAllocation();
                    newAlloc.setSimulation(savedClone);
                    newAlloc.setEtf(alloc.getEtf());
                    newAlloc.setPercentage(alloc.getPercentage());
                    return newAlloc;
                })
                .collect(Collectors.toList());

        allocationRepository.saveAll(clonedAllocations);

        return convertToResponse(savedClone);
    }

    /**
     * Confronta multiple simulazioni
     */
    public Map<String, Object> compareSimulations(List<Long> simulationIds) {
        log.info("Confronto simulazioni: {}", simulationIds);

        List<Simulation> simulations = simulationRepository.findAllById(simulationIds);

        if (simulations.size() != simulationIds.size()) {
            throw new BadRequestException("Alcune simulazioni non sono state trovate");
        }

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("simulations", simulations.stream().map(this::convertToResponse).collect(Collectors.toList()));
        comparison.put("performance_comparison", comparePerformance(simulations));
        comparison.put("risk_comparison", compareRisk(simulations));
        comparison.put("strategy_analysis", analyzeStrategies(simulations));
        comparison.put("best_performer", findBestPerformer(simulations));

        return comparison;
    }

    // ==================== METODI PRIVATI DI SUPPORTO ====================

    /**
     * Valida i parametri della richiesta di simulazione
     */
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

        // Validazione importi
        if (request.getInitialAmount() < 0) {
            throw new BadRequestException("L'importo iniziale non può essere negativo");
        }

        if (request.getMonthlyAmount() < 0) {
            throw new BadRequestException("L'importo mensile non può essere negativo");
        }

        if (request.getInvestmentPeriod() < 1) {
            throw new BadRequestException("Il periodo di investimento deve essere almeno 1 mese");
        }
    }

    /**
     * Salva le allocazioni ETF per una simulazione
     */
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

    /**
     * Calcola il rendimento annualizzato
     */
    private double calculateAnnualizedReturn(List<SimulationData> data) {
        if (data.isEmpty()) return 0.0;

        SimulationData first = data.get(0);
        SimulationData last = data.get(data.size() - 1);
        double years = data.size() / 12.0;

        if (years == 0 || first.getTotalInvested() == 0) return 0.0;

        return (Math.pow(last.getTotalValue() / first.getTotalInvested(), 1.0 / years) - 1) * 100;
    }

    /**
     * Trova il miglior mese
     */
    private double calculateBestMonth(List<SimulationData> data) {
        return data.stream()
                .mapToDouble(SimulationData::getMonthlyReturn)
                .max()
                .orElse(0.0);
    }

    /**
     * Trova il peggior mese
     */
    private double calculateWorstMonth(List<SimulationData> data) {
        return data.stream()
                .mapToDouble(SimulationData::getMonthlyReturn)
                .min()
                .orElse(0.0);
    }

    /**
     * Calcola la consistenza dei rendimenti
     */
    private double calculateConsistency(List<SimulationData> data) {
        if (data.size() < 2) return 0.0;

        long positiveMonths = data.stream()
                .skip(1)
                .filter(d -> d.getMonthlyReturn() > 0)
                .count();

        return (double) positiveMonths / (data.size() - 1);
    }

    /**
     * Calcola il Calmar Ratio
     */
    private double calculateCalmarRatio(SimulationResults results) {
        if (results.getMaxDrawdown() == 0) return 0.0;

        double annualizedReturn = results.getCumulativeReturn() / 5; // Assumendo 5 anni
        return annualizedReturn / Math.abs(results.getMaxDrawdown());
    }

    /**
     * Confronta le performance delle simulazioni
     */
    private Map<String, Object> comparePerformance(List<Simulation> simulations) {
        Map<String, Object> comparison = new HashMap<>();

        simulations.forEach(sim -> {
            Map<String, Object> performance = new HashMap<>();
            performance.put("total_return", sim.getCumulativeReturn());
            performance.put("sharpe_ratio", sim.getSharpeRatio());
            performance.put("max_drawdown", sim.getMaxDrawdown());
            performance.put("final_value", sim.getFinalValue());

            comparison.put("simulation_" + sim.getId(), performance);
        });

        return comparison;
    }

    /**
     * Confronta i rischi delle simulazioni
     */
    private Map<String, Object> compareRisk(List<Simulation> simulations) {
        Map<String, Object> riskComparison = new HashMap<>();

        simulations.forEach(sim -> {
            Map<String, Object> risk = new HashMap<>();
            risk.put("volatility", sim.getVolatility());
            risk.put("max_drawdown", sim.getMaxDrawdown());
            risk.put("risk_tolerance", sim.getRiskTolerance().name());
            risk.put("win_rate", sim.getWinRate());

            riskComparison.put("simulation_" + sim.getId(), risk);
        });

        return riskComparison;
    }

    /**
     * Analizza le strategie utilizzate
     */
    private Map<String, Object> analyzeStrategies(List<Simulation> simulations) {
        Map<String, List<Simulation>> byStrategy = simulations.stream()
                .collect(Collectors.groupingBy(s -> s.getStrategy().name()));

        Map<String, Object> analysis = new HashMap<>();

        byStrategy.forEach((strategy, sims) -> {
            Map<String, Object> strategyStats = new HashMap<>();
            strategyStats.put("count", sims.size());
            strategyStats.put("avg_return", sims.stream()
                    .filter(s -> s.getCumulativeReturn() != null)
                    .mapToDouble(Simulation::getCumulativeReturn)
                    .average()
                    .orElse(0.0));
            strategyStats.put("avg_volatility", sims.stream()
                    .filter(s -> s.getVolatility() != null)
                    .mapToDouble(Simulation::getVolatility)
                    .average()
                    .orElse(0.0));

            analysis.put(strategy, strategyStats);
        });

        return analysis;
    }

    /**
     * Trova la simulazione con le migliori performance
     */
    private Map<String, Object> findBestPerformer(List<Simulation> simulations) {
        Optional<Simulation> best = simulations.stream()
                .filter(s -> s.getSharpeRatio() != null)
                .max(Comparator.comparing(Simulation::getSharpeRatio));

        if (best.isPresent()) {
            Map<String, Object> bestPerformer = new HashMap<>();
            bestPerformer.put("simulation_id", best.get().getId());
            bestPerformer.put("name", best.get().getName());
            bestPerformer.put("strategy", best.get().getStrategy().name());
            bestPerformer.put("sharpe_ratio", best.get().getSharpeRatio());
            bestPerformer.put("total_return", best.get().getCumulativeReturn());

            return bestPerformer;
        }

        return new HashMap<>();
    }

    /**
     * Converte una Simulation entity in SimulationResponse DTO
     */
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
