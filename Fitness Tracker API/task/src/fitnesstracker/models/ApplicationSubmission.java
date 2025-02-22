package fitnesstracker.models;

import com.google.gson.annotations.Expose;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ApplicationSubmission (@Expose @NotNull @NotBlank String name,
                                     @Expose @NotNull String description){
}
