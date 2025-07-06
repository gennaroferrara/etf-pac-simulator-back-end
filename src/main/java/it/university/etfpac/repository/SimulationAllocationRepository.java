package it.university.etfpac.repository;

import it.university.etfpac.entity.Simulation;
import it.university.etfpac.entity.SimulationAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface SimulationAllocationRepository extends JpaRepository<SimulationAllocation, Long> {
    List<SimulationAllocation> findBySimulation(Simulation simulation);
    void deleteBySimulation(Simulation simulation);
}