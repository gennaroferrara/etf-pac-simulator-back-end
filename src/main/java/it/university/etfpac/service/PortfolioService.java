package it.university.etfpac.service;

import it.university.etfpac.dto.request.PortfolioRequest;
import it.university.etfpac.dto.response.PortfolioResponse;
import it.university.etfpac.entity.*;
import it.university.etfpac.exception.BadRequestException;
import it.university.etfpac.exception.ResourceNotFoundException;
import it.university.etfpac.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserRepository userRepository;
    private final ETFRepository etfRepository;
    private final SimulationRepository simulationRepository;

    public PortfolioResponse createPortfolio(PortfolioRequest request) {
        log.info("Creazione nuovo portfolio: {}", request.getName());

        validatePortfolioRequest(request);

        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        Portfolio portfolio = Portfolio.builder()
                .name(request.getName())
                .description(request.getDescription())
                .user(user)
                .initialAmount(request.getInitialAmount())
                .monthlyAmount(request.getMonthlyAmount())
                .investmentPeriodMonths(request.getInvestmentPeriodMonths())
                .frequency(Portfolio.InvestmentFrequency.valueOf(request.getFrequency().toUpperCase()))
                .strategy(Portfolio.InvestmentStrategy.valueOf(request.getStrategy().toUpperCase()))
                .rebalanceFrequency(Portfolio.RebalanceFrequency.valueOf(request.getRebalanceFrequency().toUpperCase()))
                .automaticRebalance(request.getAutomaticRebalance())
                .stopLossPercentage(request.getStopLossPercentage())
                .takeProfitPercentage(request.getTakeProfitPercentage())
                .isTemplate(request.getIsTemplate())
                .status(Portfolio.PortfolioStatus.valueOf(request.getStatus().toUpperCase()))
                .build();

        // Imposta allocazioni ETF
        Map<ETF, BigDecimal> etfAllocations = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : request.getEtfAllocations().entrySet()) {
            ETF etf = etfRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("ETF non trovato: " + entry.getKey()));
            etfAllocations.put(etf, entry.getValue());
        }
        portfolio.setEtfAllocations(etfAllocations);

        Portfolio savedPortfolio = portfolioRepository.save(portfolio);
        return convertToResponse(savedPortfolio);
    }

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolioById(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));
        return convertToResponse(portfolio);
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getUserPortfolios(Long userId, boolean includeTemplates) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        List<Portfolio> portfolios;
        if (includeTemplates) {
            portfolios = portfolioRepository.findByUserOrIsTemplateOrderByCreatedAtDesc(user, true);
        } else {
            portfolios = portfolioRepository.findByUserAndIsTemplateOrderByCreatedAtDesc(user, false);
        }

        return portfolios.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PortfolioResponse> getUserPortfoliosPaged(Long userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        return portfolioRepository.findByUserAndIsTemplate(user, false, pageable)
                .map(this::convertToResponse);
    }

    public PortfolioResponse updatePortfolio(Long id, PortfolioRequest request) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        if (!portfolio.isEditable()) {
            throw new BadRequestException("Portfolio non modificabile");
        }

        portfolio.setName(request.getName());
        portfolio.setDescription(request.getDescription());
        portfolio.setInitialAmount(request.getInitialAmount());
        portfolio.setMonthlyAmount(request.getMonthlyAmount());
        portfolio.setInvestmentPeriodMonths(request.getInvestmentPeriodMonths());

        // Aggiorna allocazioni ETF
        Map<ETF, BigDecimal> newAllocations = new HashMap<>();
        for (Map.Entry<String, BigDecimal> entry : request.getEtfAllocations().entrySet()) {
            ETF etf = etfRepository.findById(entry.getKey())
                    .orElseThrow(() -> new ResourceNotFoundException("ETF non trovato: " + entry.getKey()));
            newAllocations.put(etf, entry.getValue());
        }
        portfolio.setEtfAllocations(newAllocations);

        return convertToResponse(portfolioRepository.save(portfolio));
    }

    public void deletePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        /*
        long activeSimulations = simulationRepository.countByPortfolioAndStatus(
                portfolio, Simulation.SimulationStatus.RUNNING);

        if (activeSimulations > 0) {
            throw new BadRequestException("Portfolio con simulazioni attive non può essere eliminato");
        } */

        portfolioRepository.delete(portfolio);
    }

    public Map<String, Object> validateEtfAllocation(Map<String, Double> etfAllocation) {
        Map<String, Object> validation = new HashMap<>();
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        double totalAllocation = etfAllocation.values().stream().mapToDouble(Double::doubleValue).sum();
        validation.put("total_allocation", totalAllocation);
        validation.put("is_valid", Math.abs(totalAllocation - 100.0) < 0.01);

        if (Math.abs(totalAllocation - 100.0) >= 0.01) {
            errors.add("La somma delle allocazioni deve essere 100%");
        }

        for (String etfId : etfAllocation.keySet()) {
            if (!etfRepository.existsById(etfId)) {
                errors.add("ETF non trovato: " + etfId);
            }
        }

        if (etfAllocation.size() < 3) {
            warnings.add("Considera più ETF per diversificazione");
        }

        validation.put("errors", errors);
        validation.put("warnings", warnings);
        return validation;
    }

    public Map<String, Object> optimizePortfolio(Map<String, Object> request) {
        String riskProfile = (String) request.get("risk_profile");
        Map<String, Double> suggestedAllocation = generateOptimalAllocation(riskProfile);

        Map<String, Object> optimization = new HashMap<>();
        optimization.put("suggested_allocation", suggestedAllocation);
        optimization.put("risk_profile", riskProfile);
        optimization.put("optimization_timestamp", LocalDateTime.now());

        return optimization;
    }

    public Map<String, Object> analyzePortfolioRisk(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        Map<String, Object> riskAnalysis = new HashMap<>();
        double volatility = calculateVolatility(portfolio);
        double beta = calculateBeta(portfolio);

        riskAnalysis.put("portfolio_volatility", volatility);
        riskAnalysis.put("portfolio_beta", beta);
        riskAnalysis.put("risk_level", determineRiskLevel(volatility, beta));
        riskAnalysis.put("diversification_score", calculateDiversificationScore(portfolio));

        return riskAnalysis;
    }

    public Map<String, Object> comparePortfolios(List<Long> portfolioIds) {
        List<Portfolio> portfolios = portfolioRepository.findAllById(portfolioIds);

        Map<String, Object> comparison = new HashMap<>();
        comparison.put("portfolios", portfolios.stream().map(this::convertToResponse).collect(Collectors.toList()));
        comparison.put("risk_comparison", portfolios.stream().collect(Collectors.toMap(
                p -> "portfolio_" + p.getId(),
                p -> Map.of(
                        "volatility", calculateVolatility(p),
                        "beta", calculateBeta(p),
                        "diversification", calculateDiversificationScore(p)
                )
        )));

        return comparison;
    }

    public PortfolioResponse clonePortfolio(Long id, String newName) {
        Portfolio original = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        Portfolio clone = original.cloneAsTemplate(
                newName != null ? newName : original.getName() + " - Copia");
        clone.setIsTemplate(false);

        return convertToResponse(portfolioRepository.save(clone));
    }

    public PortfolioResponse activatePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        if (!portfolio.isAllocationValid()) {
            throw new BadRequestException("Allocazione non valida");
        }

        portfolio.activate();
        return convertToResponse(portfolioRepository.save(portfolio));
    }

    public PortfolioResponse pausePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        portfolio.pause();
        return convertToResponse(portfolioRepository.save(portfolio));
    }

    public PortfolioResponse completePortfolio(Long id) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        portfolio.complete();
        return convertToResponse(portfolioRepository.save(portfolio));
    }

    @Transactional(readOnly = true)
    public List<PortfolioResponse> getPortfolioTemplates(String riskProfile) {
        List<Portfolio> templates;
        if (riskProfile != null) {
            templates = portfolioRepository.findByIsTemplateAndUserRiskProfileOrderByCreatedAtDesc(
                    true, User.RiskProfile.valueOf(riskProfile.toUpperCase()));
        } else {
            templates = portfolioRepository.findByIsTemplateOrderByCreatedAtDesc(true);
        }

        return templates.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public PortfolioResponse saveAsTemplate(Long id, String templateName) {
        Portfolio portfolio = portfolioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        Portfolio template = portfolio.cloneAsTemplate(templateName);
        return convertToResponse(portfolioRepository.save(template));
    }

    public Map<String, Object> simulatePerformance(Long portfolioId, int months) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        double expectedReturn = calculateExpectedReturn(portfolio);
        double volatility = calculateVolatility(portfolio);

        List<Map<String, Object>> monthlyData = new ArrayList<>();
        double portfolioValue = portfolio.getInitialAmount().doubleValue();
        double totalInvested = portfolioValue;

        Random random = new Random(42);

        for (int month = 0; month < months; month++) {
            if (month > 0) {
                totalInvested += portfolio.getMonthlyAmount().doubleValue();
                portfolioValue += portfolio.getMonthlyAmount().doubleValue();

                double monthlyReturn = expectedReturn / 12.0 + (random.nextGaussian() * volatility / Math.sqrt(12));
                portfolioValue *= (1 + monthlyReturn);
            }

            Map<String, Object> monthData = new HashMap<>();
            monthData.put("month", month);
            monthData.put("portfolio_value", portfolioValue);
            monthData.put("total_invested", totalInvested);
            monthData.put("cumulative_return", ((portfolioValue - totalInvested) / totalInvested) * 100);

            monthlyData.add(monthData);
        }

        Map<String, Object> simulation = new HashMap<>();
        simulation.put("monthly_data", monthlyData);
        simulation.put("final_value", portfolioValue);
        simulation.put("total_invested", totalInvested);
        simulation.put("total_return", ((portfolioValue - totalInvested) / totalInvested) * 100);

        return simulation;
    }

    public Map<String, Object> analyzeDiversification(Long portfolioId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        Map<String, Object> diversification = new HashMap<>();
        diversification.put("num_etfs", portfolio.getEtfAllocations().size());
        diversification.put("diversification_score", calculateDiversificationScore(portfolio));
        diversification.put("sector_allocation", calculateSectorAllocation(portfolio));

        return diversification;
    }

    public Map<String, Object> calculateRebalancing(Long portfolioId, Map<String, Double> currentValues) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        double totalValue = currentValues.values().stream().mapToDouble(Double::doubleValue).sum();
        Map<String, Double> trades = new HashMap<>();

        for (Map.Entry<ETF, BigDecimal> entry : portfolio.getEtfAllocations().entrySet()) {
            String etfId = entry.getKey().getId();
            double targetPercentage = entry.getValue().doubleValue();
            double currentValue = currentValues.getOrDefault(etfId, 0.0);
            double targetValue = (targetPercentage / 100.0) * totalValue;
            double tradeAmount = targetValue - currentValue;

            if (Math.abs(tradeAmount) > 100) {
                trades.put(etfId, tradeAmount);
            }
        }

        Map<String, Object> rebalancing = new HashMap<>();
        rebalancing.put("required_trades", trades);
        rebalancing.put("rebalancing_needed", !trades.isEmpty());

        return rebalancing;
    }

    public Map<String, Object> exportPortfolio(Long portfolioId, String format) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        Map<String, Object> exportData = new HashMap<>();
        exportData.put("portfolio", convertToResponse(portfolio));
        exportData.put("export_format", format);
        exportData.put("export_timestamp", LocalDateTime.now());

        return exportData;
    }

    public Map<String, Object> getUserPortfolioStatistics(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        List<Portfolio> portfolios = portfolioRepository.findByUserAndIsTemplate(user, false);

        Map<String, Object> statistics = new HashMap<>();
        statistics.put("total_portfolios", portfolios.size());
        statistics.put("active_portfolios", portfolios.stream().mapToLong(p -> p.getActive() ? 1 : 0).sum());
        statistics.put("total_initial_investment", portfolios.stream()
                .map(Portfolio::getInitialAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add));

        return statistics;
    }

    public List<PortfolioResponse> getPortfoliosByStrategy(String strategy) {
        Portfolio.InvestmentStrategy investmentStrategy = Portfolio.InvestmentStrategy.valueOf(strategy.toUpperCase());
        List<Portfolio> portfolios = portfolioRepository.findByStrategyAndIsTemplate(investmentStrategy, false);

        return portfolios.stream().map(this::convertToResponse).collect(Collectors.toList());
    }

    public Map<String, Object> checkUserCompatibility(Long portfolioId, Long userId) {
        Portfolio portfolio = portfolioRepository.findById(portfolioId)
                .orElseThrow(() -> new ResourceNotFoundException("Portfolio non trovato"));

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utente non trovato"));

        double portfolioRisk = calculateVolatility(portfolio);
        boolean riskCompatible = isRiskCompatible(portfolioRisk, user.getRiskProfile().name());

        Map<String, Object> compatibility = new HashMap<>();
        compatibility.put("risk_compatible", riskCompatible);
        compatibility.put("portfolio_risk_level", portfolioRisk);
        compatibility.put("user_risk_profile", user.getRiskProfile().name());
        compatibility.put("recommendation", riskCompatible ? "RECOMMENDED" : "NOT_RECOMMENDED");

        return compatibility;
    }

    // Metodi di supporto privati semplificati
    private void validatePortfolioRequest(PortfolioRequest request) {
        BigDecimal totalAllocation = request.getEtfAllocations().values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalAllocation.compareTo(new BigDecimal("100.00")) != 0) {
            throw new BadRequestException("Allocazione deve essere 100%");
        }
    }

    private Map<String, Double> generateOptimalAllocation(String riskProfile) {
        Map<String, Double> allocation = new HashMap<>();

        switch (riskProfile.toUpperCase()) {
            case "CONSERVATIVO":
                allocation.put("bonds", 50.0);
                allocation.put("world_equity", 30.0);
                allocation.put("real_estate", 20.0);
                break;
            case "MODERATO":
                allocation.put("world_equity", 60.0);
                allocation.put("bonds", 25.0);
                allocation.put("emerging", 15.0);
                break;
            case "AGGRESSIVO":
                allocation.put("world_equity", 50.0);
                allocation.put("emerging", 30.0);
                allocation.put("sp500", 20.0);
                break;
            default:
                allocation.put("world_equity", 60.0);
                allocation.put("bonds", 40.0);
        }

        return allocation;
    }

    private double calculateVolatility(Portfolio portfolio) {
        return portfolio.getEtfAllocations().entrySet().stream()
                .mapToDouble(entry -> {
                    double weight = entry.getValue().doubleValue() / 100.0;
                    double volatility = getETFVolatility(entry.getKey());
                    return weight * volatility;
                })
                .sum() * 0.85; // Diversification benefit
    }

    private double calculateBeta(Portfolio portfolio) {
        return portfolio.getEtfAllocations().entrySet().stream()
                .mapToDouble(entry -> {
                    double weight = entry.getValue().doubleValue() / 100.0;
                    double beta = entry.getKey().getBeta() != null ? entry.getKey().getBeta() : 1.0;
                    return weight * beta;
                })
                .sum();
    }

    private double calculateExpectedReturn(Portfolio portfolio) {
        return portfolio.getEtfAllocations().entrySet().stream()
                .mapToDouble(entry -> {
                    double weight = entry.getValue().doubleValue() / 100.0;
                    double returnRate = entry.getKey().getFiveYear() != null ? entry.getKey().getFiveYear() / 100.0 : 0.08;
                    return weight * returnRate;
                })
                .sum();
    }

    private double getETFVolatility(ETF etf) {
        return switch (etf.getRisk()) {
            case LOW -> 0.08;
            case MEDIUM -> 0.15;
            case HIGH -> 0.20;
            case VERY_HIGH -> 0.25;
        };
    }

    private String determineRiskLevel(double volatility, double beta) {
        if (volatility < 0.10 && beta < 0.7) return "BASSO";
        if (volatility < 0.18 && beta < 1.2) return "MEDIO";
        return "ALTO";
    }

    private double calculateDiversificationScore(Portfolio portfolio) {
        int numETFs = portfolio.getEtfAllocations().size();
        double maxAllocation = portfolio.getEtfAllocations().values().stream()
                .mapToDouble(BigDecimal::doubleValue)
                .max()
                .orElse(0.0);

        double baseScore = Math.min(numETFs * 20, 80);
        double concentrationPenalty = Math.max(0, (maxAllocation - 50) * 2);

        return Math.max(0, Math.min(100, baseScore - concentrationPenalty));
    }

    private Map<String, Double> calculateSectorAllocation(Portfolio portfolio) {
        Map<String, Double> sectorAllocation = new HashMap<>();

        for (Map.Entry<ETF, BigDecimal> entry : portfolio.getEtfAllocations().entrySet()) {
            String sector = entry.getKey().getSector() != null ? entry.getKey().getSector() : "Other";
            double allocation = entry.getValue().doubleValue();
            sectorAllocation.merge(sector, allocation, Double::sum);
        }

        return sectorAllocation;
    }

    private boolean isRiskCompatible(double portfolioRisk, String userRiskProfile) {
        return switch (userRiskProfile) {
            case "CONSERVATIVO" -> portfolioRisk <= 0.12;
            case "MODERATO" -> portfolioRisk <= 0.20;
            case "AGGRESSIVO" -> true;
            default -> portfolioRisk <= 0.15;
        };
    }

    private PortfolioResponse convertToResponse(Portfolio portfolio) {
        Map<String, BigDecimal> etfAllocations = portfolio.getEtfAllocations().entrySet().stream()
                .collect(Collectors.toMap(
                        entry -> entry.getKey().getId(),
                        Map.Entry::getValue
                ));

        List<PortfolioResponse.ETFAllocationDetail> etfDetails = portfolio.getEtfAllocations().entrySet().stream()
                .map(entry -> {
                    ETF etf = entry.getKey();
                    return PortfolioResponse.ETFAllocationDetail.builder()
                            .etfId(etf.getId())
                            .etfName(etf.getName())
                            .ticker(etf.getTicker())
                            .allocationPercentage(entry.getValue())
                            .sector(etf.getSector())
                            .riskLevel(etf.getRisk().name())
                            .expenseRatio(etf.getExpense())
                            .expectedReturn(etf.getFiveYear())
                            .build();
                })
                .collect(Collectors.toList());

      //  List<Simulation> recentSimulations = simulationRepository.findTop3ByPortfolioOrderByCreatedAtDesc(portfolio);
        /*List<PortfolioResponse.SimulationSummary> simulationSummaries = recentSimulations.stream()
                .map(sim -> PortfolioResponse.SimulationSummary.builder()
                        .simulationId(sim.getId())
                        .simulationName(sim.getName())
                        .status(sim.getStatus().name())
                        .finalReturn(sim.getCumulativeReturn())
                        .createdAt(sim.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
*/
        return PortfolioResponse.builder()
                .id(portfolio.getId())
                .name(portfolio.getName())
                .description(portfolio.getDescription())
                .userId(portfolio.getUser().getId())
                .userName(portfolio.getUser().getName())
                .initialAmount(portfolio.getInitialAmount())
                .monthlyAmount(portfolio.getMonthlyAmount())
                .investmentPeriodMonths(portfolio.getInvestmentPeriodMonths())
                .frequency(portfolio.getFrequency().name().toLowerCase())
                .strategy(portfolio.getStrategy().name().toLowerCase())
                .rebalanceFrequency(portfolio.getRebalanceFrequency().name().toLowerCase())
                .automaticRebalance(portfolio.getAutomaticRebalance())
                .stopLossPercentage(portfolio.getStopLossPercentage())
                .takeProfitPercentage(portfolio.getTakeProfitPercentage())
                .etfAllocations(etfAllocations)
                .etfAllocationDetails(etfDetails)
                .active(portfolio.getActive())
                .isTemplate(portfolio.getIsTemplate())
                .status(portfolio.getStatus().name().toLowerCase())
                .createdAt(portfolio.getCreatedAt())
                .updatedAt(portfolio.getUpdatedAt())
                .lastSimulatedAt(portfolio.getLastSimulatedAt())
                .totalAllocationPercentage(portfolio.getTotalAllocationPercentage())
                .allocationValid(portfolio.isAllocationValid())
                .estimatedTotalInvestment(portfolio.getEstimatedTotalInvestment())
                .editable(portfolio.isEditable())
                .expectedReturn(BigDecimal.valueOf(calculateExpectedReturn(portfolio)))
                .expectedVolatility(BigDecimal.valueOf(calculateVolatility(portfolio)))
                .riskLevel(determineRiskLevel(calculateVolatility(portfolio), calculateBeta(portfolio)))
            //    .simulationCount(recentSimulations.size())
                //.recentSimulations(simulationSummaries)
                .build();
    }
}