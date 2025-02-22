class AppProfile {
    private String name;
    private String description;
    private String apikey;
    private String category;

    AppProfile(String name, String description) {
        this.name = name;
        this.description = description;
        this.apikey = null;
        this.category = "premium";
    }

    public String getName() {
        return name;
    }

    public AppProfile setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getApikey() {
        return apikey;
    }

    public void setApikey(String apikey) {
        this.apikey = apikey;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}

class AppProfileMother {
    public static AppProfile demo1() {
        return new AppProfile("Demo App 1", "In sit amet posuere magna. Cras cursus tincidunt.");
    }

    public static AppProfile demo2() {
        return new AppProfile("Demo App 2", "");
    }

    public static AppProfile withBadName(String name) {
        return new AppProfile(name, "In sit amet posuere magna. Cras cursus tincidunt.");
    }

    public static AppProfile withBadDescription(String description) {
        var name = "app-" + System.currentTimeMillis();
        return new AppProfile(name, description);
    }

    public static AppProfile withBadApiKey(String apiKey) {
        var name = "app-" + System.currentTimeMillis();
        var app = new AppProfile(name, "app description");
        app.setApikey(apiKey);
        return app;
    }

    public static AppProfile basicApp() {
        var name = "app-" + System.currentTimeMillis();
        var app = new AppProfile(name, "app description");
        app.setCategory("basic");
        return app;
    }
}
