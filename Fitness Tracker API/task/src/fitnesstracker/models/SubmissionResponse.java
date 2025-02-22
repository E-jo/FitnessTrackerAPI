package fitnesstracker.models;

import com.google.gson.annotations.Expose;

public record SubmissionResponse (@Expose String name, @Expose String apikey){
}