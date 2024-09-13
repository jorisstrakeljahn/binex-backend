package de.hsbi.binex.binex_backend.repository;


import de.hsbi.binex.binex_backend.entity.Participation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ParticipationRepository extends JpaRepository<Participation, Long> {
    boolean existsByHashValue(String hashValue);
}

