package it.university.etfpac.service;

import it.university.etfpac.entity.*;
import it.university.etfpac.repository.ETFRepository;
import it.university.etfpac.repository.SimulationAllocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SimulationEngine {

    private final ETFRepository etfRepository;
    private final SimulationAllocationRepository allocationRepository;

    public List<SimulationData> runSimulation(Simulation simulation) {
        log.info("Esecuzione simulazione per ID: {}", simulation.getId());

        List<SimulationData> simulationData = new ArrayList<>();
        List<SimulationAllocation> allocations = allocationRepository.findBySimulation(simulation);

        // Parametri di mercato
        MarketFactors marketFactors = new MarketFactors();

        double totalValue = 0;
        double totalInvested = simulation.getInitialAmount();

        for (int month = 0; month <= simulation.getInvestmentPeriod(); month++) {
            // Calcola shock di mercato casuale
            double marketShock = Math.random() < 0.05 ? (Math.random() - 0.5) * 0.3 : 0;
            double inflationAdjustment = Math.pow(1 + marketFactors.getInflation() / 12, month);

            double monthlyValue = 0;
            double monthlyReturn = 0;

            // Calcola performance per ogni ETF nell'allocazione
            for (SimulationAllocation allocation : allocations) {
                ETF etf = allocation.getEtf();
                double percentage = allocation.getPercentage();

                if (percentage > 0) {
                    double baseReturn = (etf.getFiveYear() / 100) / 12; // Rendimento mensile atteso
                    double volatility = calculateVolatility(etf.getRisk());
                    double randomFactor = (Math.random() - 0.5) * volatility + marketShock;
                    double monthlyPerformance = baseReturn + randomFactor;

                    double etfValue = (totalValue * percentage / 100) * (1 + monthlyPerformance);
                    monthlyValue += etfValue;
                    monthlyReturn += monthlyPerformance * (percentage / 100);
                }
            }

            // Calcola investimento mensile con strategia selezionata
            double monthlyInvestment = month == 0 ? simulation.getInitialAmount() :
                    calculateMonthlyInvestment(simulation, month, monthlyReturn, totalValue, totalInvested);

            if (month > 0) {
                totalInvested += monthlyInvestment;
                totalValue += monthlyInvestment;
            }

            totalValue = monthlyValue > 0 ? monthlyValue : totalValue;

            SimulationData dataPoint = new SimulationData();
            dataPoint.setSimulation(simulation);
            dataPoint.setMonth(month);
            dataPoint.setTotalValue(totalValue);
            dataPoint.setTotalInvested(totalInvested);
            dataPoint.setMonthlyInvestment(monthlyInvestment);
            dataPoint.setMonthlyReturn(monthlyReturn * 100);
            dataPoint.setCumulativeReturn(((totalValue - totalInvested) / totalInvested) * 100);
            dataPoint.setInflationAdjustedValue(totalValue / inflationAdjustment);
            dataPoint.setSharpeRatio(month > 12 ? (monthlyReturn - 0.02) / 0.1 : 0.0);

            simulationData.add(dataPoint);
        }

        log.info("Simulazione completata per ID: {}", simulation.getId());
        return simulationData;
    }

    public SimulationResults calculateResults(List<SimulationData> simulationData) {
        if (simulationData.isEmpty()) {
            throw new IllegalArgumentException("Dati simulazione vuoti");
        }

        SimulationData finalData = simulationData.get(simulationData.size() - 1);

        // Calcola volatilità
        List<Double> returns = simulationData.stream()
                .skip(1)
                .map(SimulationData::getMonthlyReturn)
                .toList();

        double avgReturn = returns.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
        double volatility = Math.sqrt(returns.stream()
                .mapToDouble(r -> Math.pow(r - avgReturn, 2))
                .average()
                .orElse(0.0));

        // Calcola max drawdown
        double maxDrawdown = calculateMaxDrawdown(simulationData);

        // Calcola win rate
        long positiveMonths = returns.stream()
                .mapToLong(r -> r > 0 ? 1 : 0)
                .sum();
        double winRate = returns.isEmpty() ? 0 : (double) positiveMonths / returns.size() * 100;

        return SimulationResults.builder()
                .finalValue(finalData.getTotalValue())
                .totalInvested(finalData.getTotalInvested())
                .cumulativeReturn(finalData.getCumulativeReturn())
                .volatility(volatility)
                .maxDrawdown(maxDrawdown)
                .sharpeRatio((finalData.getCumulativeReturn() - 2) / volatility)
                .winRate(winRate)
                .build();
    }

    private double calculateVolatility(ETF.RiskLevel risk) {
        return switch (risk) {
            case LOW -> 0.02;
            case MEDIUM -> 0.04;
            case HIGH -> 0.06;
            case VERY_HIGH -> 0.08;
        };
    }

    private double calculateMonthlyInvestment(Simulation simulation, int month,
                                              double monthlyReturn, double totalValue, double totalInvested) {
        double baseAmount = simulation.getMonthlyAmount();

        return switch (simulation.getStrategy()) {
            case VALUE_AVERAGING -> {
                double targetValue = totalInvested + (baseAmount * month);
                yield Math.max(0, targetValue - totalValue);
            }
            case MOMENTUM -> {
                double momentum = month > 3 ? monthlyReturn * 5 : 0;
                yield baseAmount * (1 + momentum);
            }
            case CONTRARIAN -> {
                double contrarian = month > 1 ? -monthlyReturn * 3 : 0;
                yield baseAmount * (1 + Math.max(-0.5, contrarian));
            }
            case SMART_BETA -> {
                double smartFactor = (monthlyReturn * 2) + (Math.random() - 0.5) * 0.1;
                yield baseAmount * (1 + smartFactor * 0.5);
            }
            case TACTICAL -> {
                // Allocazione tattica basata su condizioni di mercato
                double tacticalFactor = Math.random() > 0.5 ? 1.2 : 0.8;
                yield baseAmount * tacticalFactor;
            }
            default -> baseAmount; // DCA
        };
    }

    private double calculateMaxDrawdown(List<SimulationData> simulationData) {
        double maxDrawdown = 0;
        double peak = 0;

        for (SimulationData data : simulationData) {
            if (data.getTotalValue() > peak) {
                peak = data.getTotalValue();
            } else {
                double drawdown = ((data.getTotalValue() - peak) / peak) * 100;
                if (drawdown < maxDrawdown) {
                    maxDrawdown = drawdown;
                }
            }
        }

        return maxDrawdown;
    }

    public List<SimulationData> runSimulationWithAllocations(Simulation simulation, List<SimulationAllocation> allocations) {
        log.info("Esecuzione simulazione con allocazioni temporanee");

        List<SimulationData> simulationData = new ArrayList<>();

        // I Parametri di mercato
        MarketFactors marketFactors = new MarketFactors();

        double totalValue = simulation.getInitialAmount();
        double totalInvested = simulation.getInitialAmount();

        for (int month = 0; month <= simulation.getInvestmentPeriod(); month++) {
            // Calcola lo shock di mercato casuale
            double marketShock = Math.random() < 0.05 ? (Math.random() - 0.5) * 0.3 : 0;
            double inflationAdjustment = Math.pow(1 + marketFactors.getInflation() / 12, month);

            double monthlyValue = 0;
            double monthlyReturn = 0;

            // Calcola le  performance per ogni ETF nell'allocazione
            for (SimulationAllocation allocation : allocations) {
                ETF etf = allocation.getEtf();
                double percentage = allocation.getPercentage();

                if (percentage > 0) {
                    double baseReturn = (etf.getFiveYear() / 100) / 12; // Rendimento mensile atteso
                    double volatility = calculateVolatility(etf.getRisk());
                    double randomFactor = (Math.random() - 0.5) * volatility + marketShock;
                    double monthlyPerformance = baseReturn + randomFactor;

                    double etfValue = (totalValue * percentage / 100) * (1 + monthlyPerformance);
                    monthlyValue += etfValue;
                    monthlyReturn += monthlyPerformance * (percentage / 100);
                }
            }

            // Calcola l'investimento mensile con strategia selezionata
            double monthlyInvestment = month == 0 ? simulation.getInitialAmount() :
                    calculateMonthlyInvestment(simulation, month, monthlyReturn, totalValue, totalInvested);

            if (month > 0) {
                totalInvested += monthlyInvestment;
                totalValue += monthlyInvestment;
            }

            totalValue = monthlyValue > 0 ? monthlyValue : totalValue;

            // Crea punto dati
            SimulationData dataPoint = new SimulationData();
            dataPoint.setMonth(month);
            dataPoint.setTotalValue(totalValue);
            dataPoint.setTotalInvested(totalInvested);
            dataPoint.setMonthlyInvestment(monthlyInvestment);
            dataPoint.setMonthlyReturn(monthlyReturn * 100);
            dataPoint.setCumulativeReturn(((totalValue - totalInvested) / totalInvested) * 100);
            dataPoint.setInflationAdjustedValue(totalValue / inflationAdjustment);
            dataPoint.setSharpeRatio(month > 12 ? (monthlyReturn - 0.02) / 0.1 : 0.0);

            simulationData.add(dataPoint);
        }

        log.info("Simulazione temporanea completata");
        return simulationData;
    }

    // ClassE helper
    private static class MarketFactors {
        private final double inflation = 0.02;
        private final double recessionProbability = 0.15;
        private final double interestRates = 0.035;
        private final double marketSentiment = 0.65;

        public double getInflation() { return inflation; }
    }
}