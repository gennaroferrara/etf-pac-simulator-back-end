package it.university.etfpac.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Portfolio Entity
 *
 * Represents a user's investment portfolio configuration
 */
@Entity
@Table(name = "portfolios", indexes = {
        @Index(name = "idx_portfolios_user_id", columnList = "user_id"),
        @Index(name = "idx_portfolios_name", columnList = "name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false)
public class Portfolio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    @ToString.Exclude
    private User user;

    @NotNull
    @DecimalMin(value = "0.00", message = "Initial amount must be non-negative")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal initialAmount;

    @NotNull
    @DecimalMin(value = "0.00", message = "Monthly amount must be non-negative")
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal monthlyAmount;

    @NotNull
    @DecimalMin(value = "1", message = "Investment period must be at least 1 month")
    @DecimalMax(value = "600", message = "Investment period cannot exceed 600 months")
    @Column(nullable = false)
    private Integer investmentPeriodMonths;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvestmentFrequency frequency = InvestmentFrequency.MONTHLY;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private InvestmentStrategy strategy = InvestmentStrategy.DCA;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private RebalanceFrequency rebalanceFrequency = RebalanceFrequency.QUARTERLY;

    @Column(nullable = false)
    @Builder.Default
    private Boolean automaticRebalance = true;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "50.00")
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal stopLossPercentage = BigDecimal.ZERO;

    @DecimalMin(value = "0.00")
    @DecimalMax(value = "200.00")
    @Column(precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal takeProfitPercentage = BigDecimal.ZERO;

    @ElementCollection
    @CollectionTable(
            name = "portfolio_etf_allocations",
            joinColumns = @JoinColumn(name = "portfolio_id")
    )
    @MapKeyJoinColumn(name = "etf_id")
    @Column(name = "allocation_percentage", precision = 5, scale = 2)
    private Map<ETF, BigDecimal> etfAllocations;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isTemplate = false;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PortfolioStatus status = PortfolioStatus.DRAFT;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private List<Simulation> simulations;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastSimulatedAt;

    /**
     * Investment Frequency Enum
     */
    public enum InvestmentFrequency {
        WEEKLY("Settimanale"),
        MONTHLY("Mensile"),
        QUARTERLY("Trimestrale");

        private final String displayName;

        InvestmentFrequency(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Investment Strategy Enum
     */
    public enum InvestmentStrategy {
        DCA("Dollar Cost Averaging"),
        VALUE_AVERAGING("Value Averaging"),
        MOMENTUM("Momentum Strategy"),
        CONTRARIAN("Contrarian Strategy"),
        SMART_BETA("Smart Beta Strategy"),
        TACTICAL("Tactical Asset Allocation");

        private final String displayName;

        InvestmentStrategy(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Rebalance Frequency Enum
     */
    public enum RebalanceFrequency {
        NEVER("Mai"),
        MONTHLY("Mensile"),
        QUARTERLY("Trimestrale"),
        SEMIANNUAL("Semestrale"),
        ANNUAL("Annuale");

        private final String displayName;

        RebalanceFrequency(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Portfolio Status Enum
     */
    public enum PortfolioStatus {
        DRAFT("Bozza"),
        ACTIVE("Attivo"),
        PAUSED("In Pausa"),
        COMPLETED("Completato"),
        ARCHIVED("Archiviato");

        private final String displayName;

        PortfolioStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Get total allocation percentage
     */
    public BigDecimal getTotalAllocationPercentage() {
        if (etfAllocations == null || etfAllocations.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return etfAllocations.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Check if allocation is valid (totals 100%)
     */
    public boolean isAllocationValid() {
        return getTotalAllocationPercentage().compareTo(new BigDecimal("100.00")) == 0;
    }

    /**
     * Get estimated total investment
     */
    public BigDecimal getEstimatedTotalInvestment() {
        return initialAmount.add(
                monthlyAmount.multiply(new BigDecimal(investmentPeriodMonths))
        );
    }

    /**
     * Check if portfolio is editable
     */
    public boolean isEditable() {
        return status == PortfolioStatus.DRAFT || status == PortfolioStatus.PAUSED;
    }

    /**
     * Update last simulation timestamp
     */
    public void updateLastSimulation() {
        this.lastSimulatedAt = LocalDateTime.now();
    }

    /**
     * Activate portfolio
     */
    public void activate() {
        this.status = PortfolioStatus.ACTIVE;
        this.active = true;
    }

    /**
     * Pause portfolio
     */
    public void pause() {
        this.status = PortfolioStatus.PAUSED;
    }

    /**
     * Complete portfolio
     */
    public void complete() {
        this.status = PortfolioStatus.COMPLETED;
        this.active = false;
    }

    /**
     * Archive portfolio
     */
    public void archive() {
        this.status = PortfolioStatus.ARCHIVED;
        this.active = false;
    }

    /**
     * Clone portfolio as template
     */
    public Portfolio cloneAsTemplate(String templateName) {
        return Portfolio.builder()
                .name(templateName)
                .description("Template based on: " + this.name)
                .initialAmount(this.initialAmount)
                .monthlyAmount(this.monthlyAmount)
                .investmentPeriodMonths(this.investmentPeriodMonths)
                .frequency(this.frequency)
                .strategy(this.strategy)
                .rebalanceFrequency(this.rebalanceFrequency)
                .automaticRebalance(this.automaticRebalance)
                .stopLossPercentage(this.stopLossPercentage)
                .takeProfitPercentage(this.takeProfitPercentage)
                .etfAllocations(this.etfAllocations)
                .isTemplate(true)
                .status(PortfolioStatus.DRAFT)
                .build();
    }
}