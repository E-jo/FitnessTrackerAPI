package fitnesstracker.models;

import com.google.gson.annotations.Expose;

import java.util.List;

public record DeveloperProfile (@Expose Long id, @Expose String email, @Expose List<Application> applications){
}
