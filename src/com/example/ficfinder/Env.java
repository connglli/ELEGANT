package com.example.ficfinder;


import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.tracker.Issue;
import com.example.ficfinder.tracker.PubSub;
import com.example.ficfinder.tracker.PubSub.Message;
import com.example.ficfinder.tracker.Tracker;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class Env {

    // Environment variable of FicFinder

    public static final String[] OPTIONS = {
            // general options
            "-whole-program",

            // input options
            "-process-dir", "test/example.apk", // TODO it should be a parameter passed to fic-finder
            "-src-prec", "apk",
            "-android-jars", "assets/android-platforms",
            "-prepend-classpath",
            "-allow-phantom-refs",
            "-no-bodies-for-excluded",
            "-keep-line-number",

            // output options
            "-output-format", "none",

            // process options
            "-phase-option", "cg.spark", "enabled:true"
    };

    // k neighbors, used in backward slicing
    public static final int ENV_K_NEIGHBORS = 5;

    // k-indirect-caller, used in call site computing
    // 0-indirect-caller is its direct caller
    public static final int ENV_K_INDIRECT_CALLER = 1;

    // Singleton

    private static Env instance;

    // Environments

    public static final String ANDROID_PLATFORMS_PATH = "assets/android-platforms";

    public static final String SOURCES_AND_SINKS_TEXT_PATH = "assets/SourcesAndSinks.txt";

    private PubSub tracker = Tracker.v();

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

    public ProgramDependenceGraph getPDG(String method) {
        return pdgMapping.get(method);
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

    public void setPDG(String method, ProgramDependenceGraph pdg) {
        this.pdgMapping.put(method, pdg);
    }

    /**
     * Emit an issue to Tracker
     *
     * @param issue
     */
    public void emit(Issue issue) {
        tracker.publish(issue);
    }

    private Env() {
        this.pdgMapping = new HashMap<>();
    }

}
