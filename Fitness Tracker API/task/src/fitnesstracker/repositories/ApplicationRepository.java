package fitnesstracker.repositories;

import fitnesstracker.models.Application;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface ApplicationRepository extends JpaRepository<Application, Long> {
    List<Application> findAllByOrderByCreatedAtAsc();
    List<Application> findAllByOrderByCreatedAtDesc();
    Optional<Application> findByName(String email);
    Optional<Application> findByApiKey(String apiKey);
}

