package com.example.ficfinder;

import com.example.ficfinder.finder.Env;
import com.example.ficfinder.finder.Finder;
import com.example.ficfinder.tracker.Tracker;
import com.sun.istack.internal.NotNull;

import java.util.List;

public class Container {

    private ConfigParser configurations;
    private Tracker tracker;
    private Finder finder;

    private Env environment;

    {
        configurations = new ConfigParser();
        tracker        = new Tracker();

        environment    = new Env(this);
        finder         = new Finder(this);
    }

    public void run(@NotNull List<String> configs) {
        configurations.parse(configs);
        finder.run();
    }

    public ConfigParser getConfigurations() {
        return configurations;
    }

    public Tracker getTracker() {
        return tracker;
    }

    public Finder getFinder() {
        return finder;
    }

    public Env getEnvironment() {
        return environment;
    }
}
