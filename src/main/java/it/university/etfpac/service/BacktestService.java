package it.university.etfpac.service;

import it.university.etfpac.dto.request.BacktestRequest;
import it.university.etfpac.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class BacktestService {

    private final ETFRepository etfRepository;
    private final UserRepository userRepository;
    private final SimulationEngine simulationEngine;

    public Map<String, Object> runBacktest(BacktestRequest request) {
        log.info("Esecuzione backtest: {} strategia {} periodo {}",
                request.getName(), request.getStrategy(), request.getPeriod());

        validateBacktestRequest(request);

        // Calcola la durata del backtest
        long months = ChronoUnit.MONTHS.between(request.getStartDate(), request.getEndDate());

        // Genera i  dati storici simulati
        List<BacktestDataPoint> historicalData = generateHistoricalData(request, (int) months);

        // Esegue il  backtest con strategia selezionata
        BacktestResults results = executeBacktest(request, historicalData);

        // Confronta con il benchmark
        BacktestResults benchmarkResults = executeBenchmarkBacktest(request, historicalData);

        Map<String, Object> response = new HashMap<>();
        response.put("backtest_id", generateBacktestId());
        response.put("request", request);
        response.put("results", results);
        response.put("benchmark_results", benchmarkResults);
        response.put("comparison", compareWithBenchmark(results, benchmarkResults));
        response.put("risk_metrics", calculateRiskMetrics(results));
        response.put("performance_attribution", calculatePerformanceAttribution(request, results));
        response.put("historical_data", historicalData);
        response.put("executed_at", new Date());

        log.info("Backtest completato: rendimento {}%, Sharpe {}",
                results.getTotalReturn(), results.getSharpeRatio());

        return response;
    }

    public Map<String, Object> compareStrategies(Map<String, Object> comparisonRequest) {
        log.info("Confronto strategie backtest");

        @SuppressWarnings("unchecked")
        List<String> strategies = (List<String>) comparisonRequest.get("strategies");

        @SuppressWarnings("unchecked")
        Map<String, Object> baseParameters = (Map<String, Object>) comparisonRequest.get("base_parameters");

        Map<String, BacktestResults> strategyResults = new HashMap<>();

        // Esegue il backtest per ogni strategia
        for (String strategy : strategies) {
            BacktestRequest request = createBacktestRequestFromMap(baseParameters);
            request.setStrategy(strategy);
            request.setName("Comparison_" + strategy);

            Map<String, Object> backtestResult = runBacktest(request);
            strategyResults.put(strategy.toLowerCase(), (BacktestResults) backtestResult.get("results"));
        }

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("strategies", strategies);
        comparison.put("results", strategyResults);
        comparison.put("summary", createStrategySummary(strategyResults));
        comparison.put("best_strategy", findBestStrategy(strategyResults));
        comparison.put("risk_return_chart", createRiskReturnChart(strategyResults));
        comparison.put("executed_at", new Date());

        return comparison;
    }

    public Map<String, Object> getBacktestResults(Long backtestId) {
        log.info("Recupero risultati backtest ID: {}", backtestId);

        // Simula il recupero da database/cache
        Map<String, Object> results = new HashMap<>();
        results.put("backtest_id", backtestId);
        results.put("status", "COMPLETED");
        results.put("created_at", new Date());

        results.put("message", "Risultati backtest recuperati con successo");

        return results;
    }

    private void validateBacktestRequest(BacktestRequest request) {
        // Controllo null safety
        if (request == null) {
            throw new IllegalArgumentException("BacktestRequest non può essere null");
        }

        // Imposta date di default se mancanti
        if (request.getStartDate() == null) {
            request.setStartDate(LocalDate.now().minusYears(5));
            log.warn("StartDate mancante, impostata a: {}", request.getStartDate());
        }

        if (request.getEndDate() == null) {
            request.setEndDate(LocalDate.now());
            log.warn("EndDate mancante, impostata a: {}", request.getEndDate());
        }

        // Validazione date
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Data inizio deve essere precedente alla data fine");
        }

        if (request.getStartDate().isAfter(LocalDate.now().minusMonths(1))) {
            throw new IllegalArgumentException("Data inizio deve essere almeno 1 mese fa");
        }

        // Validazione allocazioni
        if (request.getEtfAllocation() == null || request.getEtfAllocation().isEmpty()) {
            // Imposta allocazione di default
            Map<String, Double> defaultAllocation = new HashMap<>();
            defaultAllocation.put("world_equity", 60.0);
            defaultAllocation.put("bonds", 20.0);
            defaultAllocation.put("emerging", 15.0);
            defaultAllocation.put("real_estate", 5.0);
            request.setEtfAllocation(defaultAllocation);
            log.warn("ETF allocation mancante, impostata allocazione di default");
        }

        double totalAllocation = request.getEtfAllocation().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (Math.abs(totalAllocation - 100.0) > 0.01) {
            throw new IllegalArgumentException("La somma delle allocazioni deve essere 100% (attuale: " + totalAllocation + "%)");
        }

        // Validazione ETF esistenti - solo se repository è disponibile
        try {
            for (String etfId : request.getEtfAllocation().keySet()) {
                if (!etfRepository.existsById(etfId)) {
                    log.warn("ETF non trovato nel database: {}, continuo comunque", etfId);
                }
            }
        } catch (Exception e) {
            log.warn("Impossibile validare ETF IDs: {}", e.getMessage());
        }
    }

    private List<BacktestDataPoint> generateHistoricalData(BacktestRequest request, int months) {
        List<BacktestDataPoint> data = new ArrayList<>();

        // Simula dati storici realistici
        Random random = new Random(42); // Seed fisso per risultati riproducibili
        double baseReturn = 0.008; // 0.8% mensile
        double volatility = 0.04; // 4% volatilità mensile

        double portfolioValue = request.getInitialAmount();
        double totalInvested = request.getInitialAmount();

        for (int month = 0; month <= months; month++) {
            // Simula rendimento mensile con volatilità
            double monthlyReturn = baseReturn + (random.nextGaussian() * volatility);

            // Aggiusta per crisi occasionali
            if (random.nextDouble() < 0.05) { // 5% probabilità di shock
                monthlyReturn -= 0.1 + (random.nextDouble() * 0.2); // -10% a -30%
            }

            // Calcola valore portfolio
            if (month > 0) {
                portfolioValue *= (1 + monthlyReturn);
                portfolioValue += request.getMonthlyAmount(); // Investimento mensile
                totalInvested += request.getMonthlyAmount();
            }

            BacktestDataPoint dataPoint = BacktestDataPoint.builder()
                    .month(month)
                    .date(request.getStartDate().plusMonths(month))
                    .portfolioValue(portfolioValue)
                    .totalInvested(totalInvested)
                    .monthlyReturn(monthlyReturn * 100)
                    .cumulativeReturn(((portfolioValue - totalInvested) / totalInvested) * 100)
                    .build();

            data.add(dataPoint);
        }

        return data;
    }

    private BacktestResults executeBacktest(BacktestRequest request, List<BacktestDataPoint> data) {
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Dati storici vuoti");
        }

        BacktestDataPoint finalPoint = data.get(data.size() - 1);

        // Calcola metriche
        List<Double> returns = data.stream()
                .skip(1)
                .map(BacktestDataPoint::getMonthlyReturn)
                .collect(Collectors.toList());

        double avgReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double volatility = calculateVolatility(returns);
        double maxDrawdown = calculateMaxDrawdown(data);
        double sharpeRatio = (avgReturn - 0.2) / volatility; // Risk-free rate 0.2% mensile

        return BacktestResults.builder()
                .totalReturn(finalPoint.getCumulativeReturn())
                .annualizedReturn((Math.pow(1 + avgReturn/100, 12) - 1) * 100)
                .volatility(volatility * Math.sqrt(12)) // Annualizzata
                .sharpeRatio(sharpeRatio * Math.sqrt(12)) // Annualizzato
                .maxDrawdown(maxDrawdown)
                .finalValue(finalPoint.getPortfolioValue())
                .totalInvested(finalPoint.getTotalInvested())
                .winRate(returns.stream().mapToLong(r -> r > 0 ? 1 : 0).sum() / (double) returns.size() * 100)
                .bestMonth(returns.stream().mapToDouble(Double::doubleValue).max().orElse(0.0))
                .worstMonth(returns.stream().mapToDouble(Double::doubleValue).min().orElse(0.0))
                .build();
    }

    private BacktestResults executeBenchmarkBacktest(BacktestRequest request, List<BacktestDataPoint> data) {
        // Simula performance benchmark (es. S&P 500)
        Random random = new Random(123);
        double benchmarkReturn = 0.007; // 0.7% mensile per benchmark
        double benchmarkVolatility = 0.035;

        List<Double> benchmarkReturns = new ArrayList<>();
        for (int i = 1; i < data.size(); i++) {
            double monthlyReturn = benchmarkReturn + (random.nextGaussian() * benchmarkVolatility);
            benchmarkReturns.add(monthlyReturn * 100);
        }

        double avgReturn = benchmarkReturns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double volatility = calculateVolatility(benchmarkReturns);

        return BacktestResults.builder()
                .totalReturn(avgReturn * data.size())
                .annualizedReturn((Math.pow(1 + avgReturn/100, 12) - 1) * 100)
                .volatility(volatility * Math.sqrt(12))
                .sharpeRatio((avgReturn - 0.2) / volatility * Math.sqrt(12))
                .build();
    }

    private double calculateVolatility(List<Double> returns) {
        if (returns.size() < 2) return 0.0;

        double mean = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double variance = returns.stream()
                .mapToDouble(r -> Math.pow(r - mean, 2))
                .average()
                .orElse(0.0);

        return Math.sqrt(variance);
    }

    private double calculateMaxDrawdown(List<BacktestDataPoint> data) {
        double maxDrawdown = 0.0;
        double peak = data.get(0).getPortfolioValue();

        for (BacktestDataPoint point : data) {
            if (point.getPortfolioValue() > peak) {
                peak = point.getPortfolioValue();
            } else {
                double drawdown = (peak - point.getPortfolioValue()) / peak * 100;
                if (drawdown > maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }

        return maxDrawdown;
    }

    private Map<String, Object> compareWithBenchmark(BacktestResults portfolio, BacktestResults benchmark) {
        Map<String, Object> comparison = new HashMap<>();

        comparison.put("excess_return", portfolio.getTotalReturn() - benchmark.getTotalReturn());
        comparison.put("information_ratio",
                (portfolio.getAnnualizedReturn() - benchmark.getAnnualizedReturn()) /
                        Math.abs(portfolio.getVolatility() - benchmark.getVolatility()));
        comparison.put("alpha", portfolio.getAnnualizedReturn() - benchmark.getAnnualizedReturn());
        comparison.put("beta", portfolio.getVolatility() / benchmark.getVolatility());
        comparison.put("outperformance", portfolio.getTotalReturn() > benchmark.getTotalReturn());

        return comparison;
    }

    private Map<String, Object> calculateRiskMetrics(BacktestResults results) {
        Map<String, Object> metrics = new HashMap<>();

        metrics.put("value_at_risk_95", results.getWorstMonth() * 1.645); // VaR 95%
        metrics.put("expected_shortfall", results.getWorstMonth() * 1.28); // ES 90%
        metrics.put("calmar_ratio", results.getAnnualizedReturn() / Math.max(results.getMaxDrawdown(), 0.01));
        metrics.put("sortino_ratio", results.getAnnualizedReturn() / (results.getVolatility() * 0.7)); // Approx
        metrics.put("treynor_ratio", results.getAnnualizedReturn() / Math.max(results.getVolatility() / 15, 0.01));

        return metrics;
    }

    private Map<String, Object> calculatePerformanceAttribution(BacktestRequest request, BacktestResults results) {
        Map<String, Object> attribution = new HashMap<>();

        // Simula il contributo di ogni ETF alla performance
        for (Map.Entry<String, Double> entry : request.getEtfAllocation().entrySet()) {
            String etfId = entry.getKey();
            Double allocation = entry.getValue();

            // Simula il  contributo basato su allocazione e performance ETF
            double contribution = (allocation / 100.0) * results.getTotalReturn() * (0.8 + Math.random() * 0.4);
            attribution.put(etfId, contribution);
        }

        return attribution;
    }

    private BacktestRequest createBacktestRequestFromMap(Map<String, Object> baseRequest) {
        BacktestRequest request = new BacktestRequest();

        request.setInitialAmount(parseDouble(baseRequest.get("initialAmount")));
        request.setMonthlyAmount(parseDouble(baseRequest.get("monthlyAmount")));

        Object startDateObj = baseRequest.get("startDate");
        if (startDateObj != null && startDateObj instanceof String) {
            try {
                request.setStartDate(LocalDate.parse((String) startDateObj));
            } catch (Exception e) {
                log.warn("Errore parsing startDate, uso default: {}", e.getMessage());
                request.setStartDate(LocalDate.now().minusYears(5));
            }
        } else {
            // Default a 5 anni fa
            request.setStartDate(LocalDate.now().minusYears(5));
        }

        Object endDateObj = baseRequest.get("endDate");
        if (endDateObj != null && endDateObj instanceof String) {
            try {
                request.setEndDate(LocalDate.parse((String) endDateObj));
            } catch (Exception e) {
                log.warn("Errore parsing endDate, uso default: {}", e.getMessage());
                request.setEndDate(LocalDate.now());
            }
        } else {
            // default a oggi
            request.setEndDate(LocalDate.now());
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> etfAllocationRaw = (Map<String, Object>) baseRequest.get("etfAllocation");
        Map<String, Double> etfAllocation = new HashMap<>();

        if (etfAllocationRaw != null) {
            for (Map.Entry<String, Object> entry : etfAllocationRaw.entrySet()) {
                etfAllocation.put(entry.getKey(), parseDouble(entry.getValue()));
            }
            request.setEtfAllocation(etfAllocation);
        } else {
            //Allocazione di default
            etfAllocation.put("world_equity", 60.0);
            etfAllocation.put("bonds", 20.0);
            etfAllocation.put("emerging", 15.0);
            etfAllocation.put("real_estate", 5.0);
            request.setEtfAllocation(etfAllocation);
        }


        request.setFrequency(parseString(baseRequest.get("frequency"), "MONTHLY"));
        request.setPeriod(parseString(baseRequest.get("period"), "5Y"));
        request.setRiskTolerance(parseString(baseRequest.get("riskTolerance"), "MODERATE"));
        request.setRebalanceFrequency(parseString(baseRequest.get("rebalanceFrequency"), "QUARTERLY"));
        request.setAutomaticRebalance(parseBoolean(baseRequest.get("automaticRebalance"), true));
        request.setIncludeTransactionCosts(parseBoolean(baseRequest.get("includeTransactionCosts"), false));
        request.setIncludeDividends(parseBoolean(baseRequest.get("includeDividends"), true));
        request.setBenchmarkIndex(parseString(baseRequest.get("benchmarkIndex"), "SP500"));
        request.setUserId(parseLong(baseRequest.get("userId"), 1L));

        // Imposto altri campi richiesti con valori di default
        request.setStopLoss(parseDouble(baseRequest.get("stopLoss")));
        request.setTakeProfitTarget(parseDouble(baseRequest.get("takeProfitTarget")));

        if (baseRequest.containsKey("transactionCostPercentage")) {
            request.setTransactionCostPercentage(parseDouble(baseRequest.get("transactionCostPercentage")));
        }

        log.info("BacktestRequest creato: startDate={}, endDate={}, allocation={}",
                request.getStartDate(), request.getEndDate(), request.getEtfAllocation());

        return request;
    }

    private Map<String, Object> createStrategySummary(Map<String, BacktestResults> strategyResults) {
        Map<String, Object> summary = new HashMap<>();

        // Viene cercata la migliore e peggiore strategia
        String bestStrategy = strategyResults.entrySet().stream()
                .max(Map.Entry.<String, BacktestResults>comparingByValue(
                        Comparator.comparing(BacktestResults::getTotalReturn)))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        String worstStrategy = strategyResults.entrySet().stream()
                .min(Map.Entry.<String, BacktestResults>comparingByValue(
                        Comparator.comparing(BacktestResults::getTotalReturn)))
                .map(Map.Entry::getKey)
                .orElse("N/A");

        summary.put("best_strategy", bestStrategy);
        summary.put("worst_strategy", worstStrategy);
        summary.put("strategy_count", strategyResults.size());

        // Calcolo delle  statistiche aggregate
        DoubleSummaryStatistics returnStats = strategyResults.values().stream()
                .mapToDouble(BacktestResults::getTotalReturn)
                .summaryStatistics();

        summary.put("avg_return", returnStats.getAverage());
        summary.put("max_return", returnStats.getMax());
        summary.put("min_return", returnStats.getMin());

        return summary;
    }

    private String findBestStrategy(Map<String, BacktestResults> strategyResults) {
        return strategyResults.entrySet().stream()
                .max(Map.Entry.<String, BacktestResults>comparingByValue(
                        Comparator.comparing(r -> r.getSharpeRatio()))) // Usa Sharpe per "migliore"
                .map(Map.Entry::getKey)
                .orElse("N/A");
    }

    private Map<String, Object> createRiskReturnChart(Map<String, BacktestResults> strategyResults) {
        Map<String, Object> chart = new HashMap<>();
        List<Map<String, Object>> dataPoints = new ArrayList<>();

        for (Map.Entry<String, BacktestResults> entry : strategyResults.entrySet()) {
            Map<String, Object> point = new HashMap<>();
            point.put("strategy", entry.getKey());
            point.put("return", entry.getValue().getAnnualizedReturn());
            point.put("risk", entry.getValue().getVolatility());
            point.put("sharpe", entry.getValue().getSharpeRatio());
            dataPoints.add(point);
        }

        chart.put("data_points", dataPoints);
        chart.put("type", "risk_return_scatter");

        return chart;
    }

    private Long generateBacktestId() {
        return System.currentTimeMillis();
    }

    // Metodi helper per parsing sicuro
    private Double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }

    private String parseString(Object value, String defaultValue) {
        return value != null ? value.toString() : defaultValue;
    }

    private Boolean parseBoolean(Object value, Boolean defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Boolean) return (Boolean) value;
        if (value instanceof String) return Boolean.parseBoolean((String) value);
        return defaultValue;
    }

    private Long parseLong(Object value, Long defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Number) return ((Number) value).longValue();
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    // Classi helper
    @lombok.Data
    @lombok.Builder
    public static class BacktestDataPoint {
        private Integer month;
        private LocalDate date;
        private Double portfolioValue;
        private Double totalInvested;
        private Double monthlyReturn;
        private Double cumulativeReturn;
    }

    @lombok.Data
    @lombok.Builder
    public static class BacktestResults {
        private Double totalReturn;
        private Double annualizedReturn;
        private Double volatility;
        private Double sharpeRatio;
        private Double maxDrawdown;
        private Double finalValue;
        private Double totalInvested;
        private Double winRate;
        private Double bestMonth;
        private Double worstMonth;
    }
}