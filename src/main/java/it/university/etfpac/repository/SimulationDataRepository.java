package it.university.etfpac.repository;

import it.university.etfpac.entity.Simulation;
import it.university.etfpac.entity.SimulationData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationDataRepository extends JpaRepository<SimulationData, Long> {
    List<SimulationData> findBySimulationOrderByMonth(Simulation simulation);
    void deleteBySimulation(Simulation simulation);
}