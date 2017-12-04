package com.example.ficfinder;


import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.tracker.PubSub;
import com.example.ficfinder.tracker.PubSub.Issue;
import com.example.ficfinder.tracker.Tracker;
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
            "-process-dir", "test/example/out", // TODO it should be a parameter passed to fic-finder
            "-src-prec", "class",
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

    // Singleton

    private static Env instance;

    // Environments

    private PubSub tracker = Tracker.v();

    private Set<ApiContext> models;

    private Map<String, ProgramDependenceGraph> pdgMapping;

    public static Env v() {
        if (instance == null) {
            instance = new Env();
        }

        return instance;
    }

    public Set<ApiContext> getModels() {
        return models;
    }

    public ProgramDependenceGraph getPDG(String method) {
        return pdgMapping.get(method);
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
