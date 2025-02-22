package fitnesstracker.services;

import fitnesstracker.models.Application;
import fitnesstracker.repositories.ApplicationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ApplicationService {
    @Autowired
    ApplicationRepository applicationRepository;

    public Application save(Application application) {
        return applicationRepository.save(application);
    }

    public List<Application> findAll() {
        return applicationRepository.findAll();
    }

    public List<Application> findAllByOrderByCreatedAtAsc() {
        return applicationRepository.findAllByOrderByCreatedAtAsc();
    }

    public List<Application> findAllByOrderByCreatedAtDesc() {
        return applicationRepository.findAllByOrderByCreatedAtDesc();
    }

    public Optional<Application> findByName(String name) {
        return applicationRepository.findByName(name);
    }

    public Optional<Application> findByApiKey(String apiKey) {
        return applicationRepository.findByApiKey(apiKey);
    }
}

