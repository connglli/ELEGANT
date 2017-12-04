package com.example.ficfinder.finder;


import com.example.ficfinder.Env;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.ApiField;
import com.example.ficfinder.models.api.ApiIface;
import com.example.ficfinder.models.api.ApiMethod;
import com.example.ficfinder.tracker.Issue;
import com.example.ficfinder.utils.Logger;
import com.example.ficfinder.utils.Strings;
import soot.*;
import soot.jimple.AssignStmt;
import soot.jimple.IfStmt;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.options.Options;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.pdg.*;


import java.util.*;

public class Finder {

    // Singleton

    private static Finder instance;

    private Logger logger = new Logger(Finder.class);

    public static Finder v() {
        if (instance == null) {
            instance = new Finder();
        }

        return instance;
    }

    public void run() {
        this.setUp();
        this.core();
    }

    private void setUp() {
        soot.G.reset();

        Options.v().parse(Env.OPTIONS);

        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();

        // TODO: main class: this is hard coding
        SootClass c = Scene.v().loadClassAndSupport("com.example.Example");
        c.setApplicationClass();
        Scene.v().setMainClass(c);

        // add a transform to generate PDG
        PackManager.v().getPack("jtp").add(new Transform("jtp.pdg_transform", new BodyTransformer() {
            @Override
            protected void internalTransform(Body body, String s, Map<String, String> map) {
                String methodSignature = body.getMethod().getSignature();
                try {
                    Env.v().setPDG(methodSignature,
                            new HashMutablePDG(new BriefUnitGraph(body)));
                } catch (Exception e) {
                    logger.w("Error in generating PDG for " + methodSignature);
                }
            }
        }));

        PackManager.v().runPacks();
    }

    /**
     * Core Algorithm of fic-finder
     *
     */
    private void core() {
        logger.i("Core Algorithm goes here");

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

                    logger.w("Model of @field is not supported by now");

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

                    logger.w("Model of @iface is not supported by now");

                    // ApiIface apiIface = (ApiIface) model.getApi();
                    // SootClass sootIface = scene.getSootClass(apiIface.getSiganiture());

                    break;
                }

                default: throw new RuntimeException("Invalid api type: " + type);
            }
        } catch (Exception e) {
            logger.w("Cannot find `" + model.getApi().getSiganiture() + "`");
        }

        return callsites;
    }

    /**
     * Check whether the callsite is ficable i.e. may generate FIC issues
     *
     */
    private boolean maybeFICable(ApiContext model, Callsite callsite) {
        // TODO This is hard coding

        // compiled sdk version, used to check whether an api
        // is accessible or not
        final int targetSdk = 23;
        final int minSdk = 2;

        return model.hasBadDevices() || !model.matchApiLevel(targetSdk, minSdk);
    }

    /**
     * Check whether the stmt can handle the specific issue
     *
     */
    public boolean canHandleIssue(ApiContext model, Unit unit) {
        // we only parse Jimple
        if (!(unit instanceof AssignStmt)) {
            return false;
        }

        //
        // TODO
        //
        AssignStmt stmt = (AssignStmt) unit;

        // check use boxes
        List<ValueBox> useValueBoxes = stmt.getUseBoxes();

        for (ValueBox vb : useValueBoxes) {
            String siganiture = vb.getValue().toString();

            if (model.needCheckApiLevel() || model.needCheckSystemVersion()) {
                return Strings.containsIgnoreCase(siganiture,
                        "android.os.Build.VERSION_CODES",
                        "android.os.Build.VERSION.SDK_INT",
                        "android.os.Build.VERSION.SDK",
                        "os.Build.VERSION_CODES",
                        "os.Build.VERSION.SDK_INT",
                        "os.Build.VERSION.SDK",
                        "Build.VERSION_CODES",
                        "Build.VERSION.SDK_INT",
                        "Build.VERSION.SDK",
                        "VERSION_CODES",
                        "VERSION.SDK_INT",
                        "VERSION.SDK");
            }

            if (model.hasBadDevices()) {
                return Strings.containsIgnoreCase(siganiture,
                        "android.os.Build.BOARD",
                        "android.os.Build.BRAND",
                        "android.os.Build.DEVICE",
                        "android.os.Build.PRODUCT",
                        "os.Build.BOARD",
                        "os.Build.BRAND",
                        "os.Build.DEVICE",
                        "os.Build.PRODUCT",
                        "Build.BOARD",
                        "Build.BRAND",
                        "Build.DEVICE",
                        "Build.PRODUCT",
                        "BOARD",
                        "BRAND",
                        "DEVICE",
                        "PRODUCT");
            }
        }


        return false;
    }

    /**
     * Backward slicing algorithm.
     *
     * we find the backward slicing of a unit by:
     *  1. get the corresponding pdg, which describes the unit's method
     *  2. find the dependents of callerUnit
     *  3. find the nearest IfStmt of the caller unit
     *  4. find the dependents of IfStmt(which defines the variable used in IfStmt)
     *
     */
    private Set<Unit> runBackwardSlicingFor(Callsite callsite) {
        Set<Unit> slice = new HashSet<>(128);

        // 1. get the corresponding pdg, which describes the unit's method
        SootMethod caller = callsite.getMethod();
        ProgramDependenceGraph pdg = Env.v().getPDG(caller.getSignature());
        Unit callerUnit = callsite.getUnit();

        // 2. find the dependents of callerUnit
        slice.addAll(findDependentOf(pdg, callerUnit));

        // 3. find the nearest K IfStmt of the caller unit
        List<Unit> unitsOfCaller = new ArrayList<>(caller.getActiveBody().getUnits());
        List<Map.Entry<Integer, Unit>> kIfStmtNeightbors = findKNeighbors(unitsOfCaller, callerUnit);
        Iterator<Map.Entry<Integer, Unit>> iterator = kIfStmtNeightbors.iterator();

        // 4. find the dependents of each IfStmt(which defines the variable used in IfStmt)
        while (iterator.hasNext()) {
            Map.Entry<Integer, Unit> entry = iterator.next();
            int ifIdx   = entry.getKey();
            Unit ifUnit = entry.getValue();

            if (ifUnit != null) {
                IfStmt ifStmt = (IfStmt) ifUnit;
                // get use boxed, e.g. [$v1, $v2] of `if $v1 > $v2 goto label 0`
                Set<Value> useValuesOfIfUnit = new HashSet<>(2);
                for (ValueBox vb : ifStmt.getCondition().getUseBoxes()) {
                    Value v = vb.getValue();
                    // we only save Locals
                    if (v instanceof Local) {
                        useValuesOfIfUnit.add(vb.getValue());
                    }
                }
                for (int i = ifIdx - 1; i >= 0; i --) {
                    Unit u = unitsOfCaller.get(i);
                    // $vx = ...
                    if (u instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) u;
                        List<ValueBox> defBoxesOfAssignUnit = assignStmt.getDefBoxes();
                        // check whether $vx in useBoxesOfIfUnit, if yes, then add it to slice
                        for (ValueBox vb : defBoxesOfAssignUnit) {
                            if (useValuesOfIfUnit.contains(vb.getValue())) {
                                slice.add(assignStmt);
                                // $vx = virtualinvoke method, then we need to add all stmt in method
                                if (assignStmt.containsInvokeExpr()) {
                                    slice.addAll(assignStmt.getInvokeExpr().getMethod().getActiveBody().getUnits());
                                }
                            }
                        }
                    }
                }
            }
        }

        return slice;
    }

    /**
     * Find K IfStmt neighbors
     *
     */
    private List<Map.Entry<Integer, Unit>> findKNeighbors(List<Unit> unitsOfCaller, Unit callerUnit) {
        LinkedList<Map.Entry<Integer, Unit>> queue = new LinkedList<>();

        for (int i = 0; i < unitsOfCaller.size(); i ++) {
            Unit u = unitsOfCaller.get(i);

            // stop here
            if (u.equals(callerUnit)) { break; }

            // add to queue
            if (u instanceof IfStmt) {
                if (queue.size() == Env.ENV_K_NEIGHBORS) {
                    queue.poll();
                }
                queue.offer(new HashMap.SimpleEntry<>(i, u));
            }
        }

        return queue;
    }

    /**
     * Find node of unit in pdg
     *
     */
    private PDGNode findNodeOf(ProgramDependenceGraph pdg, Unit unit) {
        PDGNode node = null;

        for (PDGNode n : pdg) {
            Iterator<Unit> iterator = unitIteratorOfPDGNode(n);
            if (iterator == null) continue;

            while (iterator.hasNext()) {
                if (iterator.next().equals(unit)) { node = n; break; }
            }

            if (node != null) { break; }
        }

        return node;
    }

    /**
     * Find dependent units of unit unit
     *
     */
    private Set<Unit> findDependentOf(ProgramDependenceGraph pdg, Unit unit) {
        Set<Unit> deps = new HashSet<>(128);

        // 1. find the corresponding PDGNode srcNode, which contains the unit
        PDGNode srcNode = findNodeOf(pdg, unit);
        if (srcNode == null) { return deps; }

        // 2. find all the dependent PDGNodes of srcNode
        List<PDGNode> depOfCallerUnit = pdg.getDependents(srcNode);

        // 3. get all the units of each dependent PDGNode
        for (PDGNode dependent : depOfCallerUnit) {
            Iterator<Unit> iter = unitIteratorOfPDGNode(dependent);
            while (iter.hasNext()) {
                Unit u = iter.next();
                deps.add(u);
            }
        }

        return deps;
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
            iterator = ((IRegion) n.getNode()).getUnitGraph().iterator();
        } else {
            logger.w("Only REGION and CFGNODE are allowed");
        }

        return iterator;
    }

}
