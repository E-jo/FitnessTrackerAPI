package fitnesstracker.models;

import jakarta.persistence.*;
import com.google.gson.annotations.Expose;
import lombok.*;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicBoolean;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenBucket {
    @Id
    private String clientId;
    private AtomicInteger tokens;
    private LocalDateTime lastUpdated;
    private AtomicBoolean validRequest;
}