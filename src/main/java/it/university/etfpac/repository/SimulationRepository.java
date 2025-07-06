package it.university.etfpac.repository;

import it.university.etfpac.entity.Simulation;
import it.university.etfpac.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    List<Simulation> findByUserOrderByCreatedAtDesc(User user);

    Page<Simulation> findByUser(User user, Pageable pageable);

    List<Simulation> findByStatus(Simulation.SimulationStatus status);

    @Query("SELECT s FROM Simulation s WHERE s.user = :user AND s.status = 'COMPLETED' ORDER BY s.cumulativeReturn DESC")
    List<Simulation> findBestPerformingSimulations(User user);

    @Query("SELECT COUNT(s) FROM Simulation s WHERE s.user = :user AND s.status = 'COMPLETED'")
    Long countCompletedSimulationsByUser(User user);
}