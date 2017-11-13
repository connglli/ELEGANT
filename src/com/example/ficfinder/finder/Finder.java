package com.example.ficfinder.finder;


import com.example.ficfinder.Configs;
import com.example.ficfinder.Env;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.ApiField;
import com.example.ficfinder.models.api.ApiIface;
import com.example.ficfinder.models.api.ApiMethod;
import com.example.ficfinder.tracker.Issue;
import com.example.ficfinder.utils.Strings;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;
import soot.toolkits.graph.pdg.Region;

import java.util.*;

public class Finder {

    // Singleton

    private static Finder instance;

    public static Finder v() {
        if (instance == null) {
            instance = new Finder();
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
        options.set_output_format(Options.output_format_jimple);
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
                String methodSignature = body.getMethod().getSignature();
                try {
                    Env.v().getPdgMapping().put(
                            methodSignature,
                            new HashMutablePDG(new BriefUnitGraph(body)));
                } catch (Exception e) {
                    System.out.println("@WARNING: Error in generating PDG for " + methodSignature);
                }
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

        Set<ApiContext> models = Env.v().getModels();

        for (ApiContext model : models) {
            Set<Callsite> callsites = this.computeCallsites(model);

            // traverse callsites
            for (Callsite callsite : callsites) {
                if (!maybeFICable(model, callsite)) continue;
                Set<Unit> slice = runBackwardSlicingFor(callsite);
                boolean isIssueHandled = false;

                for (Unit stmt : slice) {
                    if (canHandleIssue(model, stmt)) {
                        isIssueHandled = true;
                        break;
                    }
                }

                if (!isIssueHandled) {
                    Env.v().emit(Issue.create(callsite, model));
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
    private Set<Callsite> computeCallsites(ApiContext model) {
        CallGraph callGraph = Scene.v().getCallGraph();
        Scene scene = Scene.v();

        String type = model.getApi().getClass().toString().split(" ")[1];
        Set<Callsite> callsites = new HashSet<>(128);

        try {
            // get callsites of the specific api
            switch (type) {
                case ApiField.TAG: {
                    // TODO I have no idea how to get the callsite of a specific field

                    System.out.println("@WARNING: Model of @field is not supported by now");

                    // ApiField apiField = (ApiField) model.getApi();
                    // SootField sootField = scene.getField(apiField.getSiganiture());

                    break;
                }

                case ApiMethod.TAG: {
                    ApiMethod apiMethod = (ApiMethod) model.getApi();
                    SootMethod sootMethod = scene.getMethod(apiMethod.getSiganiture());

                    Iterator<Edge> edges = callGraph.edgesInto(sootMethod);

                    while (edges.hasNext()) {
                        Edge edge = edges.next();
                        callsites.add(new Callsite(edge.src(), edge.srcUnit()));
                    }

                    break;
                }

                case ApiIface.TAG: {
                    // TODO I have no idea how to get the callsite of a specific interface

                    System.out.println("@WARNING: Model of @iface is not supported by now");

                    // ApiIface apiIface = (ApiIface) model.getApi();
                    // SootClass sootIface = scene.getSootClass(apiIface.getSiganiture());

                    break;
                }

                default: throw new RuntimeException("Invalid api type: " + type);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return callsites;
    }

    /**
     * Check whether the callsite is ficable i.e. may generate FIC issues
     *
     */
    private boolean maybeFICable(ApiContext model, Callsite callsite) {
        ProcessManifest manifest = Env.v().getManifest();

        // compiled sdk version, used to check whether an api
        // is accessible or not
        final int targetSdk = manifest.targetSdkVersion();
        final int minSdk = manifest.getMinSdkVersion();

        return !model.hasBadDevices() && model.matchApiLevel(targetSdk, minSdk);
    }

    /**
     * Check whether the stmt can handle the specific issue
     *
     */
    public boolean canHandleIssue(ApiContext model, Unit unit) {
        // we only parse Jimple
        if (!(unit instanceof Stmt) || !((Stmt) unit).containsFieldRef()) {
            return false;
        }

        //
        // TODO CHECK
        //  I don;t know exactly what FieldRef of a Stmt is!!!
        //  what if a Stmt contains more than one SootField?
        //
        Stmt stmt = (Stmt) unit;
        SootField field = stmt.getFieldRef().getField();
        String siganiture = field.getSignature();

        if (model.needCheckApiLevel() || model.needCheckSystemVersion()) {
            return Strings.contains(siganiture,
                    "android.os.Build.VERSION_CODES",
                    "android.os.Build.VERSION.SDK_INT",
                    "android.os.Build.VERSION.SDK");
        }

        if (model.hasBadDevices()) {
            return Strings.contains(siganiture,
                    "android.os.Build.BOARD",
                    "android.os.Build.BRAND",
                    "android.os.Build.DEVICE",
                    "android.os.Build.PRODUCT");
        }


        return false;
    }

    /**
     * Backward slicing algorithm.
     *
     * we find the backward slicing of a unit by:
     *  1. get the corresponding pdg, which describes the unit's method
     *  2. find the corresponding PDGNode srcNode, which contains the unit
     *  3. find all the dependent PDGNodes of srcNode
     *  4. get all the units of each dependent PDGNode
     *
     */
    private Set<Unit> runBackwardSlicingFor(Callsite callsite) {
        Set<Unit> slice = new HashSet<>(128);
        Map<String, ProgramDependenceGraph> pdgMapping = Env.v().getPdgMapping();

        Unit callsiteUnit = callsite.getUnit();
        PDGNode srcNode = null;
        ProgramDependenceGraph pdg = null;

        // 1. get the corresponding pdg, which describes the unit's method
        pdg = pdgMapping.get(callsite.getMethod().getSignature());

        // 2. find the corresponding PDGNode srcNode, which contains the unit
        for (PDGNode n : pdg) {
            Iterator<Unit> iterator = unitIteratorOfPDGNode(n);

            if (iterator != null) {
                while (iterator.hasNext()) {
                    if (iterator.next().equals(callsiteUnit)) {
                        srcNode = n;
                        break;
                    }
                }
            }

            if (iterator != null && srcNode != null) {
                break;
            }
        }

        // 3. find all the dependent PDGNodes of srcNode
        List<PDGNode> dependents = srcNode.getBackDependets();

        // 4. get all the units of each dependent PDGNode
        for (PDGNode dependent : dependents) {
            Iterator<Unit> iter = unitIteratorOfPDGNode(dependent);
            while (iter.hasNext()) {
                slice.add(iter.next());
            }
        }

        return slice;
    }

    /**
     * In javaDoc of Soot, the following information are mentioned:
     *
     *   This class(PDGNode) defines a Node in the Program Dependence
     *   Graph. There might be a need to store additional information
     *   in the PDG nodes. In essence, the PDG nodes represent (within
     *   them) either CFG nodes or Region nodes.
     *
     * So we simply considered that as only CFGNODE and REGION are allowed
     */
    private Iterator<Unit> unitIteratorOfPDGNode(PDGNode n) {
        Iterator<Unit> iterator = null;
        PDGNode.Type type = n.getType();

        // get iterator
        if (type.equals(PDGNode.Type.CFGNODE)) {
            iterator = ((Block) n.getNode()).iterator();
        } else if (type.equals(PDGNode.Type.REGION)) {
            iterator = ((Region) n.getNode()).getUnitGraph().iterator();
        } else {
            System.out.println("Only REGION and CFGNODE are allowed");
        }

        return iterator;
    }

}
