package com.example.ficfinder.finder;


import com.example.ficfinder.Container;
import com.example.ficfinder.finder.plainfinder.PFinder;
import com.example.ficfinder.finder.reflectionfinder.RFinder;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.utils.Logger;
import soot.*;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.*;


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
        Options.v().parse(this.container.getEnvironment().getOptions());

        // load classes
        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();

        // fake main created by flowdroid
        SootMethod entryPoint = this.container.getEnvironment().getApp().getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

        // add a transform to generate PDG
        PackManager.v().getPack("jtp").add(new Transform("jtp.pdg_transform", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                String methodSignature = body.getMethod().getSignature();
                try {
                    container.getEnvironment().setPDG(methodSignature,
                            new HashMutablePDG(new BriefUnitGraph(body)));
                } catch (Exception e) {
                    logger.w("Error in generating PDG for " + methodSignature);
                }
            }
        }));

        PackManager.v().runPacks();

        // uncomment to generate a call graph viewer
        // new CallGraphViewer(Scene.v().getCallGraph(), entryPoint).export("cg", "/Users/apple/Desktop");
    }

    private void go() {
        Set<ApiContext> models = this.container.getEnvironment().getModels();

        // vanilla checking
        AbstractFinder plainFinder = new PFinder(container, models);
        plainFinder.report();

        // reflection checking
        AbstractFinder reflectionFinder = new RFinder(container, models);
        reflectionFinder.report();
    }

}
