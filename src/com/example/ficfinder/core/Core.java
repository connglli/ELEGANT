package com.example.ficfinder.core;


import com.example.ficfinder.Configs;
import com.example.ficfinder.Env;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.ApiField;
import com.example.ficfinder.models.api.ApiIface;
import com.example.ficfinder.models.api.ApiMethod;
import soot.*;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.EnhancedUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

import java.util.*;

public class Core {

    // Singleton

    private static Core instance;
    
    public static Core v() {
        if (instance == null) {
            instance = new Core();
        }

        return instance;
    }

    public void run() {
        this.setUp();
        this.core();
        this.tearDown();
    }

    private void setUp() {
        // for performance
        Options options = Options.v();
        PackManager packManager = PackManager.v();

        // gc
        System.gc();

        // options pass to soot/flowdroid
        options.set_src_prec(Options.src_prec_apk);
        options.set_process_dir(Collections.singletonList(Configs.v().getArg(Configs.APK)));
        options.set_android_jars(Env.ANDROID_PLATFORMS_PATH);
        options.set_whole_program(true);
        options.set_allow_phantom_refs(true);
        options.set_output_format(Options.output_format_none);
        options.setPhaseOption("cg.spark", "on");

        Scene.v().loadNecessaryClasses();

        // fake main created by flowdroid
        SootMethod entryPoint = Env.v().getApp().getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

        // add a transform to generate PDG
        packManager.getPack("jtp").add(new Transform("jtp.pdg_transform", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                Env.v().setPdg(new HashMutablePDG(new EnhancedUnitGraph(body)));
            }
        }));

        // run it
        packManager.runPacks();
    }

    /**
     * Core Algorithm of fic-finder
     *
     */
    private void core() {
        System.out.println("Core Algorithm goes here");

        List<ApiContext> models = Env.v().getModels();

        for (ApiContext model : models) {
            List<Unit> callsites = this.computeCallsites(model);

            // traverse callsites
            for (Unit callsite : callsites) {
                if (!maybeFicable(model, callsite)) continue;
                List<Unit> slice = runBackwardSlicingFor(callsite);
                boolean isIssueHandled = false;

                for (Unit stmt : slice) {
                    if (false/* TODO do checking here */) {
                        isIssueHandled = true;
                        break;
                    }
                }

                if (!isIssueHandled) {
                    System.out.println("EMIT A ISSUE HERE");
                }
            }
        }
    }

    private void tearDown() {

    }

    /**
     * Compute all callsites of a specific api
     *
     */
    private List<Unit> computeCallsites(ApiContext model) {
        CallGraph callGraph = Scene.v().getCallGraph();
        Scene scene = Scene.v();

        String type = model.getApi().getClass().toString().split(" ")[1];
        List<Unit> callsites = new ArrayList<>(128);

        // get callsites of the specific api
        switch (type) {
            case ApiField.TAG: {
                ApiField apiField = (ApiField) model.getApi();
                SootField sootField = scene.getField(apiField.getSiganiture());

                // TODO I have no idea how to get the callsite of a specific field

                break;
            }

            case ApiMethod.TAG: {
                ApiMethod apiMethod = (ApiMethod) model.getApi();
                SootMethod sootMethod = scene.getMethod(apiMethod.getSiganiture());

                // callsites = callGraph.edgesInto(sootMethod);
                Iterator<Edge> edges = callGraph.edgesInto(sootMethod);

                while (edges.hasNext()) {
                    Edge edge = edges.next();
                    callsites.add(edge.srcUnit());
                }

                break;
            }

            case ApiIface.TAG: {
                ApiIface apiIface = (ApiIface) model.getApi();
                SootClass sootIface = scene.getSootClass(apiIface.getSiganiture());

                // TODO I have no idea how to get the callsite of a specific interface

                break;
            }

            default: throw new RuntimeException("Invalid api type: " + type);
        }

        return callsites;
    }

    /**
     * Check whether the callsite is ficable i.e. maybe generate FIC issues
     *
     */
    private boolean maybeFicable(ApiContext model, Unit callsite) {
        ProcessManifest manifest = Env.v().getManifest();

        // compiled sdk version, used to check whether an api
        // is accessible or not
        final int targetSdk = manifest.targetSdkVersion();
        final int minSdk = manifest.getMinSdkVersion();

        return !model.hasBadDevices() && model.matchApiLevel(targetSdk, minSdk);
    }

    /**
     * Backward slicing algorithm
     *
     */
    private List<Unit> runBackwardSlicingFor(Unit callsite) {

        // TODO run backward slicing here

        List<Unit> slice = new ArrayList<>(128);
        ProgramDependenceGraph pdg = Env.v().getPdg();

        Iterator<PDGNode> iterator = pdg.iterator();

        while (iterator.hasNext()) {
            PDGNode node = iterator.next();
            System.out.println(node.getNode().getClass());
        }

        return slice;
    }

}
