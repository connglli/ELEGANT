package simonlee.elegant.finder;


import simonlee.elegant.ELEGANT;
import simonlee.elegant.finder.plainfinder.PFinder;
import simonlee.elegant.finder.reflectionfinder.RFinder;
import simonlee.elegant.models.ApiContext;
import soot.*;
import soot.options.Options;

import java.util.*;

public class Finder {

    // elegant is the container that finder is in
    private ELEGANT elegant;

    public Finder(ELEGANT elegant) {
        this.elegant = elegant;
    }

    public void find() {
        this.init();
        this.go();
    }

    private void init() {
        soot.G.reset();

        // parse options
        Options.v().parse(this.elegant.getOptions());

        // load classes
        Scene.v().loadNecessaryClasses();

        // fake main created by flowdroid
        SootMethod entryPoint = this.elegant.getApp().getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

        // run it
        PackManager.v().runPacks();

        // uncomment to generate a call graph viewer
        // new CallGraphViewer(Scene.v().getCallGraph(), entryPoint).export("cg", "/Users/apple/Desktop");
    }

    private void go() {
        Set<ApiContext> models = this.elegant.getModels();

        // vanilla checking
        AbstractFinder plainFinder = new PFinder(elegant, models);
        plainFinder.analyse();

        // reflection checking
        AbstractFinder reflectionFinder = new RFinder(elegant, models);
        reflectionFinder.analyse();
    }

}
