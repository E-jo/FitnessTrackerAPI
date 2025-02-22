package fitnesstracker.repositories;

import fitnesstracker.models.FitnessLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FitnessLogRepository extends JpaRepository<FitnessLog, Long> {
    List<FitnessLog> findAllByOrderByCreatedAtAsc();
    List<FitnessLog> findAllByOrderByCreatedAtDesc();
}
