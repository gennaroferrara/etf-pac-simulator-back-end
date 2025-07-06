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

        // Calcola durata del backtest
        long months = ChronoUnit.MONTHS.between(request.getStartDate(), request.getEndDate());

        // Genera dati storici simulati
        List<BacktestDataPoint> historicalData = generateHistoricalData(request, (int) months);

        // Esegui backtest con strategia selezionata
        BacktestResults results = executeBacktest(request, historicalData);

        // Confronta con benchmark
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
        Map<String, Object> baseRequest = (Map<String, Object>) comparisonRequest.get("base_parameters");

        Map<String, BacktestResults> strategyResults = new HashMap<>();

        // Esegui backtest per ogni strategia
        for (String strategy : strategies) {
            BacktestRequest request = createBacktestRequestFromMap(baseRequest);
            request.setStrategy(strategy);
            request.setName("Comparison_" + strategy);

            Map<String, Object> backtestResult = runBacktest(request);
            strategyResults.put(strategy, (BacktestResults) backtestResult.get("results"));
        }

        // Crea confronto
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

        // Simula recupero da database/cache
        Map<String, Object> results = new HashMap<>();
        results.put("backtest_id", backtestId);
        results.put("status", "COMPLETED");
        results.put("created_at", new Date());

        // In una implementazione reale, recupereresti i dati dal database
        // Per ora restituiamo dati simulati
        results.put("message", "Risultati backtest recuperati con successo");

        return results;
    }

    private void validateBacktestRequest(BacktestRequest request) {
        // Validazione date
        if (request.getStartDate().isAfter(request.getEndDate())) {
            throw new IllegalArgumentException("Data inizio deve essere precedente alla data fine");
        }

        if (request.getStartDate().isAfter(LocalDate.now().minusMonths(1))) {
            throw new IllegalArgumentException("Data inizio deve essere almeno 1 mese fa");
        }

        // Validazione allocazioni
        double totalAllocation = request.getEtfAllocation().values().stream()
                .mapToDouble(Double::doubleValue)
                .sum();

        if (Math.abs(totalAllocation - 100.0) > 0.01) {
            throw new IllegalArgumentException("La somma delle allocazioni deve essere 100%");
        }

        // Validazione ETF esistenti
        for (String etfId : request.getEtfAllocation().keySet()) {
            if (!etfRepository.existsById(etfId)) {
                throw new IllegalArgumentException("ETF non trovato: " + etfId);
            }
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

        // Simula contributo di ogni ETF alla performance
        for (Map.Entry<String, Double> entry : request.getEtfAllocation().entrySet()) {
            String etfId = entry.getKey();
            Double allocation = entry.getValue();

            // Simula contributo basato su allocazione e performance ETF
            double contribution = (allocation / 100.0) * results.getTotalReturn() * (0.8 + Math.random() * 0.4);
            attribution.put(etfId, contribution);
        }

        return attribution;
    }

    private BacktestRequest createBacktestRequestFromMap(Map<String, Object> baseRequest) {
        BacktestRequest request = new BacktestRequest();
        // Mappa i parametri dalla mappa alla request
        // Implementazione semplificata
        request.setInitialAmount(((Number) baseRequest.get("initialAmount")).doubleValue());
        request.setMonthlyAmount(((Number) baseRequest.get("monthlyAmount")).doubleValue());
        // ... altri parametri
        return request;
    }

    private Map<String, Object> createStrategySummary(Map<String, BacktestResults> strategyResults) {
        Map<String, Object> summary = new HashMap<>();

        // Trova migliore e peggiore strategia
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

        // Calcola statistiche aggregate
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