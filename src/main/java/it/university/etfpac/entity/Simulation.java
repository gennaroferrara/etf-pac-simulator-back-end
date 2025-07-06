package it.university.etfpac.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "simulations")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Simulation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String name;

    @Column(name = "initial_amount", nullable = false)
    private Double initialAmount;

    @Column(name = "monthly_amount", nullable = false)
    private Double monthlyAmount;

    @Column(name = "investment_period", nullable = false)
    private Integer investmentPeriod;

    @Enumerated(EnumType.STRING)
    private Frequency frequency;

    @Enumerated(EnumType.STRING)
    private Strategy strategy;

    @Column(name = "risk_tolerance")
    @Enumerated(EnumType.STRING)
    private RiskTolerance riskTolerance;

    @Column(name = "rebalance_frequency")
    @Enumerated(EnumType.STRING)
    private RebalanceFrequency rebalanceFrequency;

    @Column(name = "automatic_rebalance")
    private Boolean automaticRebalance;

    @Column(name = "stop_loss")
    private Double stopLoss;

    @Column(name = "take_profit_target")
    private Double takeProfitTarget;

    @Enumerated(EnumType.STRING)
    private SimulationStatus status;

    @Column(name = "final_value")
    private Double finalValue;

    @Column(name = "total_invested")
    private Double totalInvested;

    @Column(name = "cumulative_return")
    private Double cumulativeReturn;

    @Column(name = "volatility")
    private Double volatility;

    @Column(name = "max_drawdown")
    private Double maxDrawdown;

    @Column(name = "sharpe_ratio")
    private Double sharpeRatio;

    @Column(name = "win_rate")
    private Double winRate;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id")
    private Portfolio portfolio;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = SimulationStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum Frequency {
        MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL
    }

    public enum Strategy {
        DCA, VALUE_AVERAGING, MOMENTUM, CONTRARIAN, SMART_BETA, TACTICAL
    }

    public enum RiskTolerance {
        CONSERVATIVE, MODERATE, AGGRESSIVE
    }

    public enum RebalanceFrequency {
        MONTHLY, QUARTERLY, SEMIANNUAL, ANNUAL
    }

    public enum SimulationStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}