package it.university.etfpac.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_profile")
    private RiskProfile riskProfile;

    @Enumerated(EnumType.STRING)
    private Experience experience;

    @Column(name = "total_portfolio")
    private Double totalPortfolio;

    @Column(name = "active_simulations")
    private Integer activeSimulations = 0;

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

    public enum RiskProfile {
        CONSERVATIVO, MODERATO, AGGRESSIVO
    }

    public enum Experience {
        PRINCIPIANTE, INTERMEDIO, AVANZATO
    }
}