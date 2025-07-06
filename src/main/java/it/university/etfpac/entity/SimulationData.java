package it.university.etfpac.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "simulation_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SimulationData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "simulation_id", nullable = false)
    private Simulation simulation;

    @Column(nullable = false)
    private Integer month;

    @Column(name = "total_value", nullable = false)
    private Double totalValue;

    @Column(name = "total_invested", nullable = false)
    private Double totalInvested;

    @Column(name = "monthly_investment")
    private Double monthlyInvestment;

    @Column(name = "monthly_return")
    private Double monthlyReturn;

    @Column(name = "cumulative_return")
    private Double cumulativeReturn;

    @Column(name = "inflation_adjusted_value")
    private Double inflationAdjustedValue;

    @Column(name = "sharpe_ratio")
    private Double sharpeRatio;
}