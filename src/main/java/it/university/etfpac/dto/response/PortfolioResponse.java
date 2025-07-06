package it.university.etfpac.dto.response;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class PortfolioResponse {
    private Long id;
    private String name;
    private String description;
    private Long userId;
    private String userName;

    // Investment Parameters
    private BigDecimal initialAmount;
    private BigDecimal monthlyAmount;
    private Integer investmentPeriodMonths;
    private String frequency;
    private String strategy;
    private String rebalanceFrequency;
    private Boolean automaticRebalance;
    private BigDecimal stopLossPercentage;
    private BigDecimal takeProfitPercentage;

    // ETF Allocations
    private Map<String, BigDecimal> etfAllocations;
    private List<ETFAllocationDetail> etfAllocationDetails;

    // Status and Metadata
    private Boolean active;
    private Boolean isTemplate;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastSimulatedAt;

    // Calculated Fields
    private BigDecimal totalAllocationPercentage;
    private Boolean allocationValid;
    private BigDecimal estimatedTotalInvestment;
    private Boolean editable;

    // Performance Metrics (if simulated)
    private BigDecimal expectedReturn;
    private BigDecimal expectedVolatility;
    private BigDecimal expectedSharpeRatio;
    private String riskLevel;

    // Related Data
    private Integer simulationCount;
    private List<SimulationSummary> recentSimulations;

    @Data
    @Builder
    public static class ETFAllocationDetail {
        private String etfId;
        private String etfName;
        private String ticker;
        private BigDecimal allocationPercentage;
        private String sector;
        private String riskLevel;
        private Double expenseRatio;
        private Double expectedReturn;
    }

    @Data
    @Builder
    public static class SimulationSummary {
        private Long simulationId;
        private String simulationName;
        private String status;
        private Double finalReturn;
        private LocalDateTime createdAt;
    }
}