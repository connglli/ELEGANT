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

    public Map<String, ProgramDependenceGraph> getPdgMapping() {
        return pdgMapping;
    }

    public void setModels(Set<ApiContext> models) {
        this.models = models;
    }

    public void setPdgMapping(Map<String, ProgramDependenceGraph> pdgMapping) {
        this.pdgMapping = pdgMapping;
    }

    /**
     * Emit an issue to IFCTracker
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
