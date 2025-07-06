package it.university.etfpac.repository;

import it.university.etfpac.entity.ETF;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface ETFRepository extends JpaRepository<ETF, String> {

    List<ETF> findByRisk(ETF.RiskLevel risk);

    List<ETF> findBySectorContaining(String sector);

    @Query("SELECT e FROM ETF e ORDER BY e.oneYear DESC")
    List<ETF> findTopPerformingETFs();

    @Query("SELECT e FROM ETF e WHERE e.expense <= :maxExpense ORDER BY e.expense ASC")
    List<ETF> findLowCostETFs(Double maxExpense);
}