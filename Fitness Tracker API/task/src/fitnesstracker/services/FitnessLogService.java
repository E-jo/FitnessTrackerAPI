package fitnesstracker.services;

import fitnesstracker.models.FitnessLog;
import fitnesstracker.repositories.FitnessLogRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FitnessLogService {
    @Autowired
    FitnessLogRepository fitnessLogRepository;

    public FitnessLog save(FitnessLog fitnessLog) {
        return fitnessLogRepository.save(fitnessLog);
    }

    public List<FitnessLog> findAll() {
        return fitnessLogRepository.findAll();
    }

    public List<FitnessLog> findAllByOrderByCreatedAtAsc() {
        return fitnessLogRepository.findAllByOrderByCreatedAtAsc();
    }

    public List<FitnessLog> findAllByOrderByCreatedAtDesc() {
        return fitnessLogRepository.findAllByOrderByCreatedAtDesc();
    }
}
