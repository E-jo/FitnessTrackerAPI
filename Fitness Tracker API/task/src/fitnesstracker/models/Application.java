package fitnesstracker.models;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.gson.annotations.SerializedName;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import com.google.gson.annotations.Expose;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "application_table")
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Expose
    Long id;

    @NotNull
    @NotBlank
    @Expose
    String name;

    @NotNull
    @Expose
    String description;

    @Expose
    @SerializedName("apikey")
    String apiKey;

    @Expose(serialize = false)
    String createdAt;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public Application(String name, String description, String apiKey, User user) {
        this.name = name;
        this.description = description;
        this.apiKey = apiKey;
        this.createdAt = LocalDateTime.now().toString();
    }

    @Override
    public String toString() {
        return "Application{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", apiKey='" + apiKey + '\'' +
                ", createdAt=" + createdAt +
                ", user=" + (user != null ? "User[id=" + user.getId() + "]" : "null") +  // Avoid full user details
                '}';
    }


}

/*
{
  "name": <string, not null, not blank, unique>,
  "description": <string, not null>
}
 */
