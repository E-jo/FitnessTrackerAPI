package fitnesstracker.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.gson.annotations.Expose;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FitnessLog {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    Long id;

    @Expose
    String username;

    @Expose
    String activity;

    @Expose
    Integer duration;

    @Expose
    Integer calories;

    @Expose
    String application;

    @Expose(serialize = false)
    String createdAt = LocalDateTime.now().toString();

    public FitnessLog(String username, String activity, Integer duration, Integer calories) {
        this.username = username;
        this.activity = activity;
        this.duration = duration;
        this.calories = calories;
    }

    @Override
    public String toString() {
        return "FitnessLog{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", activity='" + activity + '\'' +
                ", duration=" + duration +
                ", calories=" + calories +
                '}';
    }
}
