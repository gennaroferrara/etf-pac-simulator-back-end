package it.university.etfpac.dto.response;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class SimulationResponse {
    private Long id;
    private String name;
    private Double initialAmount;
    private Double monthlyAmount;
    private Integer investmentPeriod;
    private String frequency;
    private String strategy;
    private Map<String, Double> etfAllocation;
    private String riskTolerance;
    private String rebalanceFrequency;
    private Boolean automaticRebalance;
    private Double stopLoss;
    private Double takeProfitTarget;
    private String status;

    // Risultati
    private Double finalValue;
    private Double totalInvested;
    private Double cumulativeReturn;
    private Double volatility;
    private Double maxDrawdown;
    private Double sharpeRatio;
    private Double winRate;

    // Dati temporali
    private List<SimulationDataPoint> simulationData;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @Builder
    public static class SimulationDataPoint {
        private Integer month;
        private Double totalValue;
        private Double totalInvested;
        private Double monthlyInvestment;
        private Double monthlyReturn;
        private Double cumulativeReturn;
        private Double inflationAdjustedValue;
        private Double sharpeRatio;
    }
}