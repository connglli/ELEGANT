package com.example.ficfinder.finder;

import com.example.ficfinder.models.ApiContext;

import java.util.Set;

public abstract class AbstractFinder {

    // models are all ApiContext models
    protected Set<ApiContext> models;

    public AbstractFinder(Set<ApiContext> models) {
        this.models = models;
    }

    /**
     * detect will detect all potential bugs triggered by model
     *
     * @param model api context model
     * @return      true for continuing to validate if detected, or false
     */
    public abstract boolean detect(ApiContext model);

    /**
     * validate will validate all potential bugs and remove others if detect returned true.
     *
     * @param model api context model
     * @return      true for continue to generate, or false
     */
    public abstract boolean validate(ApiContext model);

    /**
     * generate will generate all validated bugs
     *
     * @param model api context model
     */
    public abstract void generate(ApiContext model);

    /**
     * find will find and report all validated bugs
     *
     */
    public void report() {
        this.models.forEach(model -> {
            if (detect(model) && validate(model)) {
                generate(model);
            }
        });
    }
}
