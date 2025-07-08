package it.university.etfpac.repository;

import it.university.etfpac.entity.Simulation;
import it.university.etfpac.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SimulationRepository extends JpaRepository<Simulation, Long> {

    Page<Simulation> findByUser(User user, Pageable pageable);
}