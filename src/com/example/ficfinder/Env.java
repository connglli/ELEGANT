package com.example.ficfinder;


import com.example.ficfinder.models.ApiContext;
import soot.jimple.infoflow.android.SetupApplication;

import java.util.List;

public class Env {

    // Singleton

    private static Env instance;

    // Environments

    public static final String ANDROID_PLATFORMS_PATH = "assets/android-platforms";

    public static final String SOURCES_AND_SINKS_TEXT_PATH = "assets/SourcesAndSinks.txt";

    private SetupApplication app;

    private List<ApiContext> models;

    public static Env v() {
        if (instance == null) {
            instance = new Env();
        }

        return instance;
    }

    public SetupApplication getApp() {
        return app;
    }

    public List<ApiContext> getModels() {
        return models;
    }

    void setApp(SetupApplication app) {
        this.app = app;
    }

    void setModels(List<ApiContext> models) {
        this.models = models;
    }

    private Env() {}

}
