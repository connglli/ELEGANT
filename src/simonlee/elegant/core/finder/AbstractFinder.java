package simonlee.elegant.core.finder;

import simonlee.elegant.Container;
import simonlee.elegant.models.ApiContext;

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
    protected void setUp() { }

    /**
     * detect will detect all potential bugs triggered by model
     *
     * @param model api context model
     * @return      true for continuing to validate if detected, or false
     */
    protected abstract boolean detect(ApiContext model);

    /**
     * validate will validate all potential bugs and remove others if detect returned true.
     *
     * @param model api context model
     * @return      true for continue to generate, or false
     */
    protected abstract boolean validate(ApiContext model);

    /**
     * generate will generate all validated bugs
     *
     * @param model api context model
     */
    protected abstract void generate(ApiContext model);

    /**
     * analyse will find and submit all validated bugs in the routine:
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
            // when this model has important field, then we skip the validate phase, generate them directly
            if (detect(model) &&
                    (model.isImportant()
                            || validate(model))) {
                generate(model);
            }
//            if (detect(model)) {
//                generate(model);
//            }
        });
    }
}
