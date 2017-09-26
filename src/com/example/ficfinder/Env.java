package com.example.ficfinder;


import com.example.ficfinder.models.ApiContext;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class Env {

    // Singleton

    private static Env instance;

    // Environments

    public static final String ANDROID_PLATFORMS_PATH = "assets/android-platforms";

    public static final String SOURCES_AND_SINKS_TEXT_PATH = "assets/SourcesAndSinks.txt";

    private SetupApplication app;

    private ProcessManifest manifest;

    private Set<ApiContext> models;

    private Map<String, ProgramDependenceGraph> pdgMapping;

    public static Env v() {
        if (instance == null) {
            instance = new Env();
        }

        return instance;
    }

    public SetupApplication getApp() {
        return app;
    }

    public ProcessManifest getManifest() {
        return manifest;
    }

    public Set<ApiContext> getModels() {
        return models;
    }

    public Map<String, ProgramDependenceGraph> getPdgMapping() {
        return pdgMapping;
    }

    public void setApp(SetupApplication app) {
        this.app = app;
    }

    public void setManifest(ProcessManifest manifest) {
        this.manifest = manifest;
    }

    public void setModels(Set<ApiContext> models) {
        this.models = models;
    }

    public void setPdgMapping(Map<String, ProgramDependenceGraph> pdgMapping) {
        this.pdgMapping = pdgMapping;
    }

    private Env() {
        this.pdgMapping = new HashMap<>();
    }

}
