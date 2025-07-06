package it.university.etfpac.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "etfs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ETF {

    @Id
    private String id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String ticker;

    @Column(nullable = false)
    private Double expense;

    @Enumerated(EnumType.STRING)
    private RiskLevel risk;

    private String sector;
    private String aum;
    private String dividend;
    private Double beta;
    private Double sharpe;

    @Column(name = "max_drawdown")
    private Double maxDrawdown;

    private Double ytd;

    @Column(name = "one_year")
    private Double oneYear;

    @Column(name = "three_year")
    private Double threeYear;

    @Column(name = "five_year")
    private Double fiveYear;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum RiskLevel {
        LOW, MEDIUM, HIGH, VERY_HIGH
    }
}