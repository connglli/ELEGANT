package simonlee.elegant.finder;


import simonlee.elegant.ELEGANT;
import simonlee.elegant.finder.plainfinder.PFinder;
import simonlee.elegant.finder.reflectionfinder.RFinder;
import simonlee.elegant.models.ApiContext;
import soot.PackManager;

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
        try {
            // run info flow analysis
            this.elegant.getApp().runInfoflow();
        } catch (Exception e) {
            e.printStackTrace();
        }

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
