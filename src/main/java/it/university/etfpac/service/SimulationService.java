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

    @Async("simulationExecutor")
    public CompletableFuture<Map<String, Object>> runAdvancedSimulation(Long simulationId) {
        log.info("Avvio simulazione avanzata per ID: {}", simulationId);

        try {
            // Esegui simulazione base
            runSimulation(simulationId).get();

            // Calcola analisi avanzate
            Simulation simulation = simulationRepository.findById(simulationId)
                    .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

            List<SimulationData> data = dataRepository.findBySimulationOrderByMonth(simulation);

            Map<String, Object> advancedResults = new HashMap<>();
            advancedResults.put("simulation_id", simulationId);
            advancedResults.put("basic_results", convertToResponse(simulation));
            advancedResults.put("risk_metrics", calculateAdvancedRiskMetrics(data));
            advancedResults.put("performance_attribution", calculatePerformanceAttribution(simulation));
            advancedResults.put("scenario_analysis", performScenarioAnalysis(simulation));
            advancedResults.put("market_correlation", calculateMarketCorrelation(data));

            return CompletableFuture.completedFuture(advancedResults);

        } catch (Exception e) {
            log.error("Errore simulazione avanzata ID: {}", simulationId, e);
            throw new SimulationException("Errore nella simulazione avanzata", e);
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

    @Transactional(readOnly = true)
    public Map<String, Object> getSimulationStatus(Long simulationId) {
        log.info("Recupero stato simulazione ID: {}", simulationId);

        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        Map<String, Object> status = new HashMap<>();
        status.put("simulation_id", simulationId);
        status.put("status", simulation.getStatus().name().toLowerCase());
        status.put("progress_percentage", calculateProgress(simulation));
        status.put("estimated_completion", estimateCompletion(simulation));
        status.put("last_updated", simulation.getUpdatedAt());

        return status;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getDetailedResults(Long simulationId) {
        log.info("Recupero risultati dettagliati per simulazione ID: {}", simulationId);

        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        if (simulation.getStatus() != Simulation.SimulationStatus.COMPLETED) {
            throw new BadRequestException("La simulazione non è ancora completata");
        }

        List<SimulationData> data = dataRepository.findBySimulationOrderByMonth(simulation);
        List<SimulationAllocation> allocations = allocationRepository.findBySimulation(simulation);

        Map<String, Object> results = new HashMap<>();
        results.put("simulation", convertToResponse(simulation));
        results.put("time_series_data", data);
        results.put("allocations", allocations);
        results.put("risk_metrics", calculateAdvancedRiskMetrics(data));
        results.put("performance_metrics", calculatePerformanceMetrics(data));
        results.put("monthly_analysis", calculateMonthlyAnalysis(data));

        return results;
    }

    public SimulationResponse updateSimulation(Long id, SimulationRequest request) {
        log.info("Aggiornamento simulazione ID: {}", id);

        Simulation simulation = simulationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        if (simulation.getStatus() == Simulation.SimulationStatus.RUNNING) {
            throw new BadRequestException("Impossibile modificare una simulazione in esecuzione");
        }

        // Aggiorna parametri
        simulation.setName(request.getName());
        simulation.setInitialAmount(request.getInitialAmount());
        simulation.setMonthlyAmount(request.getMonthlyAmount());
        simulation.setInvestmentPeriod(request.getInvestmentPeriod());
        simulation.setStrategy(Simulation.Strategy.valueOf(request.getStrategy().toUpperCase()));

        // Se modificata, reset dei risultati
        if (simulation.getStatus() == Simulation.SimulationStatus.COMPLETED) {
            simulation.setStatus(Simulation.SimulationStatus.PENDING);
            simulation.setFinalValue(null);
            simulation.setTotalInvested(null);
            simulation.setCumulativeReturn(null);
        }

        Simulation updated = simulationRepository.save(simulation);
        return convertToResponse(updated);
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

    @Transactional(readOnly = true)
    public Map<String, Object> getUserStatistics(Long userId) {
        log.info("Calcolo statistiche per utente ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        List<Simulation> simulations = simulationRepository.findByUserOrderByCreatedAtDesc(user);
        List<Simulation> completedSims = simulations.stream()
                .filter(s -> s.getStatus() == Simulation.SimulationStatus.COMPLETED)
                .collect(Collectors.toList());

        Map<String, Object> stats = new HashMap<>();
        stats.put("total_simulations", simulations.size());
        stats.put("completed_simulations", completedSims.size());
        stats.put("avg_return", calculateAverageReturn(completedSims));
        stats.put("best_simulation", findBestSimulation(completedSims));
        stats.put("total_invested", calculateTotalInvested(completedSims));
        stats.put("strategies_used", getStrategiesUsed(simulations));

        return stats;
    }

    @Transactional(readOnly = true)
    public Map<String, Object> exportSimulation(Long simulationId, String format) {
        log.info("Export simulazione ID: {} in formato: {}", simulationId, format);

        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        List<SimulationData> data = dataRepository.findBySimulationOrderByMonth(simulation);

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("simulation_info", convertToResponse(simulation));
        exportData.put("time_series_data", data);
        exportData.put("export_timestamp", LocalDateTime.now());
        exportData.put("format", format);
        exportData.put("version", "1.0");

        return exportData;
    }

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

    public Map<String, Object> saveAsTemplate(Long simulationId, String templateName) {
        log.info("Salvataggio simulazione ID: {} come template: {}", simulationId, templateName);

        Simulation simulation = simulationRepository.findById(simulationId)
                .orElseThrow(() -> new ResourceNotFoundException("Simulazione non trovata"));

        Map<String, Object> template = new HashMap<>();
        template.put("template_name", templateName);
        template.put("template_id", UUID.randomUUID().toString());
        template.put("original_simulation_id", simulationId);
        template.put("parameters", extractSimulationParameters(simulation));
        template.put("allocations", extractAllocations(simulation));
        template.put("created_at", LocalDateTime.now());

        return template;
    }

    // Metodi di supporto privati
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

    private int calculateProgress(Simulation simulation) {
        if (simulation.getStatus() == Simulation.SimulationStatus.COMPLETED) {
            return 100;
        } else if (simulation.getStatus() == Simulation.SimulationStatus.RUNNING) {
            // Simula progresso basato su timestamp
            long elapsed = java.time.Duration.between(simulation.getUpdatedAt(), LocalDateTime.now()).toSeconds();
            return Math.min(95, (int) (elapsed / 2)); // ~2 secondi per 1% di progresso
        }
        return 0;
    }

    private String estimateCompletion(Simulation simulation) {
        if (simulation.getStatus() == Simulation.SimulationStatus.RUNNING) {
            int progress = calculateProgress(simulation);
            int remainingSeconds = (100 - progress) * 2;
            return String.format("%d secondi", remainingSeconds);
        }
        return "N/A";
    }

    private Map<String, Object> calculateAdvancedRiskMetrics(List<SimulationData> data) {
        if (data.isEmpty()) return new HashMap<>();

        List<Double> returns = data.stream()
                .skip(1)
                .map(SimulationData::getMonthlyReturn)
                .collect(Collectors.toList());

        Map<String, Object> metrics = new HashMap<>();
        metrics.put("value_at_risk_95", calculateVaR(returns, 0.95));
        metrics.put("expected_shortfall", calculateExpectedShortfall(returns, 0.95));
        metrics.put("downside_deviation", calculateDownsideDeviation(returns));
        metrics.put("maximum_consecutive_losses", calculateMaxConsecutiveLosses(returns));
        metrics.put("skewness", calculateSkewness(returns));
        metrics.put("kurtosis", calculateKurtosis(returns));

        return metrics;
    }

    private Map<String, Object> calculatePerformanceAttribution(Simulation simulation) {
        List<SimulationAllocation> allocations = allocationRepository.findBySimulation(simulation);

        Map<String, Object> attribution = new HashMap<>();
        for (SimulationAllocation allocation : allocations) {
            ETF etf = allocation.getEtf();
            double contribution = (allocation.getPercentage() / 100.0) *
                    (simulation.getCumulativeReturn() != null ? simulation.getCumulativeReturn() : 0.0) *
                    (0.8 + Math.random() * 0.4); // Simula variazione

            Map<String, Object> etfContribution = new HashMap<>();
            etfContribution.put("etf_name", etf.getName());
            etfContribution.put("allocation_percentage", allocation.getPercentage());
            etfContribution.put("contribution_to_return", contribution);
            etfContribution.put("active_return", (etf.getOneYear() - 8.0)); // vs benchmark 8%

            attribution.put(etf.getId(), etfContribution);
        }

        return attribution;
    }

    private Map<String, Object> performScenarioAnalysis(Simulation simulation) {
        Map<String, Object> scenarios = new HashMap<>();

        // Scenario Bull Market
        scenarios.put("bull_market", createScenario("Bull Market", simulation, 1.3));

        // Scenario Bear Market
        scenarios.put("bear_market", createScenario("Bear Market", simulation, 0.6));

        // Scenario Recessione
        scenarios.put("recession", createScenario("Recession", simulation, 0.4));

        // Scenario Inflazione Alta
        scenarios.put("high_inflation", createScenario("High Inflation", simulation, 0.8));

        return scenarios;
    }

    private Map<String, Object> createScenario(String name, Simulation simulation, double multiplier) {
        Map<String, Object> scenario = new HashMap<>();
        scenario.put("scenario_name", name);
        scenario.put("expected_return", (simulation.getCumulativeReturn() != null ? simulation.getCumulativeReturn() : 0.0) * multiplier);
        scenario.put("expected_volatility", (simulation.getVolatility() != null ? simulation.getVolatility() : 0.0) * multiplier);
        scenario.put("probability", 0.15 + Math.random() * 0.1); // 15-25%

        return scenario;
    }

    private Map<String, Object> calculateMarketCorrelation(List<SimulationData> data) {
        Map<String, Object> correlation = new HashMap<>();

        // Simula correlazioni con indici principali
        correlation.put("sp500_correlation", 0.85 + (Math.random() - 0.5) * 0.2);
        correlation.put("europe_correlation", 0.72 + (Math.random() - 0.5) * 0.2);
        correlation.put("bonds_correlation", -0.15 + (Math.random() - 0.5) * 0.2);
        correlation.put("commodities_correlation", 0.35 + (Math.random() - 0.5) * 0.2);

        return correlation;
    }

    private Map<String, Object> calculatePerformanceMetrics(List<SimulationData> data) {
        Map<String, Object> metrics = new HashMap<>();

        if (!data.isEmpty()) {
            SimulationData lastPoint = data.get(data.size() - 1);
            List<Double> returns = data.stream()
                    .skip(1)
                    .map(SimulationData::getMonthlyReturn)
                    .collect(Collectors.toList());

            metrics.put("total_return", lastPoint.getCumulativeReturn());
            metrics.put("annualized_return", calculateAnnualizedReturn(returns));
            metrics.put("best_month", returns.stream().mapToDouble(Double::doubleValue).max().orElse(0.0));
            metrics.put("worst_month", returns.stream().mapToDouble(Double::doubleValue).min().orElse(0.0));
            metrics.put("positive_months", returns.stream().mapToLong(r -> r > 0 ? 1 : 0).sum());
            metrics.put("negative_months", returns.stream().mapToLong(r -> r < 0 ? 1 : 0).sum());
        }

        return metrics;
    }

    private Map<String, Object> calculateMonthlyAnalysis(List<SimulationData> data) {
        Map<String, Object> analysis = new HashMap<>();

        // Analisi per mese dell'anno
        Map<Integer, List<Double>> monthlyReturns = new HashMap<>();
        for (int month = 1; month <= 12; month++) {
            monthlyReturns.put(month, new ArrayList<>());
        }

        // Raggruppa rendimenti per mese (simulato)
        for (int i = 1; i < data.size(); i++) {
            int month = (i % 12) + 1;
            monthlyReturns.get(month).add(data.get(i).getMonthlyReturn());
        }

        // Calcola statistiche per mese
        Map<String, Object> monthlyStats = new HashMap<>();
        for (Map.Entry<Integer, List<Double>> entry : monthlyReturns.entrySet()) {
            if (!entry.getValue().isEmpty()) {
                Map<String, Object> monthStats = new HashMap<>();
                monthStats.put("avg_return", entry.getValue().stream().mapToDouble(Double::doubleValue).average().orElse(0.0));
                monthStats.put("volatility", calculateVolatility(entry.getValue()));
                monthStats.put("win_rate", entry.getValue().stream().mapToLong(r -> r > 0 ? 1 : 0).sum() / (double) entry.getValue().size());
                monthlyStats.put("month_" + entry.getKey(), monthStats);
            }
        }

        analysis.put("monthly_statistics", monthlyStats);
        return analysis;
    }

    // Metodi per calcoli finanziari specifici
    private double calculateVaR(List<Double> returns, double confidence) {
        if (returns.isEmpty()) return 0.0;
        List<Double> sorted = returns.stream().sorted().collect(Collectors.toList());
        int index = (int) ((1 - confidence) * sorted.size());
        return sorted.get(Math.max(0, index));
    }

    private double calculateExpectedShortfall(List<Double> returns, double confidence) {
        if (returns.isEmpty()) return 0.0;
        List<Double> sorted = returns.stream().sorted().collect(Collectors.toList());
        int cutoff = (int) ((1 - confidence) * sorted.size());
        return sorted.subList(0, cutoff + 1).stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private double calculateDownsideDeviation(List<Double> returns) {
        if (returns.isEmpty()) return 0.0;
        List<Double> negativeReturns = returns.stream().filter(r -> r < 0).collect(Collectors.toList());
        return calculateVolatility(negativeReturns);
    }

    private int calculateMaxConsecutiveLosses(List<Double> returns) {
        int maxConsecutive = 0;
        int current = 0;

        for (Double return_ : returns) {
            if (return_ < 0) {
                current++;
                maxConsecutive = Math.max(maxConsecutive, current);
            } else {
                current = 0;
            }
        }

        return maxConsecutive;
    }

    private double calculateSkewness(List<Double> returns) {
        if (returns.size() < 3) return 0.0;

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) return 0.0;

        return returns.stream()
                .mapToDouble(r -> Math.pow((r - mean) / stdDev, 3))
                .average()
                .orElse(0.0);
    }

    private double calculateKurtosis(List<Double> returns) {
        if (returns.size() < 4) return 0.0;

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0);
        double stdDev = Math.sqrt(variance);

        if (stdDev == 0) return 0.0;

        return returns.stream()
                .mapToDouble(r -> Math.pow((r - mean) / stdDev, 4))
                .average()
                .orElse(0.0) - 3.0; // Excess kurtosis
    }

    private double calculateAnnualizedReturn(List<Double> monthlyReturns) {
        if (monthlyReturns.isEmpty()) return 0.0;
        double avgMonthlyReturn = monthlyReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        return Math.pow(1 + avgMonthlyReturn / 100, 12) - 1;
    }

    private double calculateVolatility(List<Double> returns) {
        if (returns.size() < 2) return 0.0;
        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream().mapToDouble(r -> Math.pow(r - mean, 2)).average().orElse(0.0);
        return Math.sqrt(variance);
    }

    // Metodi per statistiche utente
    private double calculateAverageReturn(List<Simulation> simulations) {
        return simulations.stream()
                .filter(s -> s.getCumulativeReturn() != null)
                .mapToDouble(Simulation::getCumulativeReturn)
                .average()
                .orElse(0.0);
    }

    private Map<String, Object> findBestSimulation(List<Simulation> simulations) {
        Optional<Simulation> best = simulations.stream()
                .filter(s -> s.getCumulativeReturn() != null)
                .max(Comparator.comparing(Simulation::getCumulativeReturn));

        if (best.isPresent()) {
            Map<String, Object> bestSim = new HashMap<>();
            bestSim.put("simulation_id", best.get().getId());
            bestSim.put("name", best.get().getName());
            bestSim.put("return", best.get().getCumulativeReturn());
            return bestSim;
        }

        return new HashMap<>();
    }

    private double calculateTotalInvested(List<Simulation> simulations) {
        return simulations.stream()
                .filter(s -> s.getTotalInvested() != null)
                .mapToDouble(Simulation::getTotalInvested)
                .sum();
    }

    private List<String> getStrategiesUsed(List<Simulation> simulations) {
        return simulations.stream()
                .map(s -> s.getStrategy().name().toLowerCase())
                .distinct()
                .collect(Collectors.toList());
    }

    // Metodi per confronto simulazioni
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

    private Map<String, Object> compareRisk(List<Simulation> simulations) {
        Map<String, Object> riskComparison = new HashMap<>();

        simulations.forEach(sim -> {
            Map<String, Object> risk = new HashMap<>();
            risk.put("volatility", sim.getVolatility());
            risk.put("max_drawdown", sim.getMaxDrawdown());
            risk.put("risk_tolerance", sim.getRiskTolerance().name());

            riskComparison.put("simulation_" + sim.getId(), risk);
        });

        return riskComparison;
    }

    private Map<String, Object> analyzeStrategies(List<Simulation> simulations) {
        Map<String, List<Simulation>> byStrategy = simulations.stream()
                .collect(Collectors.groupingBy(s -> s.getStrategy().name()));

        Map<String, Object> analysis = new HashMap<>();

        byStrategy.forEach((strategy, sims) -> {
            Map<String, Object> strategyStats = new HashMap<>();
            strategyStats.put("count", sims.size());
            strategyStats.put("avg_return", calculateAverageReturn(sims));
            strategyStats.put("avg_volatility", sims.stream()
                    .filter(s -> s.getVolatility() != null)
                    .mapToDouble(Simulation::getVolatility)
                    .average()
                    .orElse(0.0));

            analysis.put(strategy, strategyStats);
        });

        return analysis;
    }

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

    private Map<String, Object> extractSimulationParameters(Simulation simulation) {
        Map<String, Object> params = new HashMap<>();
        params.put("initial_amount", simulation.getInitialAmount());
        params.put("monthly_amount", simulation.getMonthlyAmount());
        params.put("investment_period", simulation.getInvestmentPeriod());
        params.put("strategy", simulation.getStrategy().name());
        params.put("frequency", simulation.getFrequency().name());
        params.put("risk_tolerance", simulation.getRiskTolerance().name());
        params.put("rebalance_frequency", simulation.getRebalanceFrequency().name());

        return params;
    }

    private Map<String, Double> extractAllocations(Simulation simulation) {
        List<SimulationAllocation> allocations = allocationRepository.findBySimulation(simulation);
        return allocations.stream()
                .collect(Collectors.toMap(
                        alloc -> alloc.getEtf().getId(),
                        SimulationAllocation::getPercentage
                ));
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