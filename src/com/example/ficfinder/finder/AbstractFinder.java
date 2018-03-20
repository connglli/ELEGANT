package com.example.ficfinder.finder;

import com.example.ficfinder.Container;
import com.example.ficfinder.models.ApiContext;

import java.util.Set;

public abstract class AbstractFinder {

    protected Container container;

    // models are all ApiContext models
    protected Set<ApiContext> models;

    public AbstractFinder(Container container, Set<ApiContext> models) {
        this.container = container;
        this.models = models;
        setUp();
    }

    /**
     * setUp allows you to do some setup works, such as initializations
     */
    public void setUp() { }

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
     * analyse will find and report all validated bugs in the routine:
     *
     * algorithm:
     *   for each model m in model list do
     *     detection:  detect all potential bugs
     *     validation: validate all detected potential bugs
     *     generation: generate issues for all validated bugs
     *   done
     *
     */
    public void analyse() {
        this.models.forEach(model -> {
            if (detect(model) && validate(model)) {
                generate(model);
            }
        });
    }
}
