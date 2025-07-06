package it.university.etfpac.repository;

import it.university.etfpac.entity.Portfolio;
import it.university.etfpac.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, Long> {

    // Query per utente
    List<Portfolio> findByUserAndIsTemplateOrderByCreatedAtDesc(User user, Boolean isTemplate);

    List<Portfolio> findByUserOrIsTemplateOrderByCreatedAtDesc(User user, Boolean isTemplate);

    List<Portfolio> findByUserAndIsTemplate(User user, Boolean isTemplate);

    Page<Portfolio> findByUserAndIsTemplate(User user, Boolean isTemplate, Pageable pageable);

    // Query per template
    List<Portfolio> findByIsTemplateOrderByCreatedAtDesc(Boolean isTemplate);

    @Query("""
      SELECT p
        FROM Portfolio p
       WHERE p.isTemplate      = :isTemplate
         AND p.user.riskProfile = :riskProfile
     ORDER BY p.createdAt     DESC
    """)
    List<Portfolio> findByIsTemplateAndUserRiskProfileOrderByCreatedAtDesc(
            @Param("isTemplate") Boolean isTemplate,
            @Param("riskProfile") User.RiskProfile riskProfile
    );


    // Query per strategia
    List<Portfolio> findByStrategyAndIsTemplate(Portfolio.InvestmentStrategy strategy, Boolean isTemplate);
}