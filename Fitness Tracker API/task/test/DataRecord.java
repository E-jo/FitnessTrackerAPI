import java.security.SecureRandom;
import java.util.List;

class DataRecord {
    private String username;
    private String activity;
    private int duration;
    private int calories;
    private String application;

    DataRecord(String username, String activity, int duration, int calories) {
        this.username = username;
        this.activity = activity;
        this.duration = duration;
        this.calories = calories;
        this.application = null;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getActivity() {
        return activity;
    }

    public void setActivity(String activity) {
        this.activity = activity;
    }

    public int getDuration() {
        return duration;
    }

    public void setDuration(int duration) {
        this.duration = duration;
    }

    public int getCalories() {
        return calories;
    }

    public void setCalories(int calories) {
        this.calories = calories;
    }

    public String getApplication() {
        return application;
    }

    public void setApplication(String application) {
        this.application = application;
    }
}

class DataRecordMother {
    private static final SecureRandom rnd = new SecureRandom();
    private static final List<String> activities = List.of(
            "running", "cycling", "dancing", "hiking", "swimming", "walking"
    );

    static DataRecord createRecord() {
        var username = "user-" + System.currentTimeMillis();
        var activity = activities.get(rnd.nextInt(activities.size()));
        var duration = rnd.nextInt(30, 3000);
        var calories = rnd.nextInt(100, 700);
        return new DataRecord(username, activity, duration, calories);
    }
}
