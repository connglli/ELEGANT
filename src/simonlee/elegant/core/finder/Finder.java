package simonlee.elegant.core.finder;


import simonlee.elegant.Container;
import simonlee.elegant.core.finder.plainfinder.PFinder;
import simonlee.elegant.core.finder.reflectionfinder.RFinder;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.utils.Logger;
import soot.*;
import soot.options.Options;

import java.util.*;

public class Finder {

    private Logger logger = new Logger(Finder.class);

    // container is the container that finder is in
    private Container container;

    public Finder(Container container) {
        this.container = container;
    }

    public void run() {
        this.init();
        this.go();
    }

    private void init() {
        soot.G.reset();

        // parse options
        Options.v().parse(this.container.getOptions());

        // load classes
        Scene.v().loadNecessaryClasses();

        // fake main created by flowdroid
        SootMethod entryPoint = this.container.getApp().getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

        PackManager.v().runPacks();

        // uncomment to generate a call graph viewer
        // new CallGraphViewer(Scene.v().getCallGraph(), entryPoint).export("cg", "/Users/apple/Desktop");
    }

    private void go() {
        Set<ApiContext> models = this.container.getModels();

        // vanilla checking
        AbstractFinder plainFinder = new PFinder(container, models);
        plainFinder.analyse();

        // reflection checking
        AbstractFinder reflectionFinder = new RFinder(container, models);
        reflectionFinder.analyse();
    }

}
