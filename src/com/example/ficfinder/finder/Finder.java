package com.example.ficfinder.finder;


import com.example.ficfinder.Env;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.ApiField;
import com.example.ficfinder.models.api.ApiIface;
import com.example.ficfinder.models.api.ApiMethod;
import com.example.ficfinder.tracker.Issue;
import com.example.ficfinder.utils.Logger;
import com.example.ficfinder.utils.MultiTree;
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

        // parse options
        Options.v().parse(Env.OPTIONS);

        // load classes
        Scene.v().loadNecessaryClasses();
        Scene.v().loadBasicClasses();

        // fake main created by flowdroid
        SootMethod entryPoint = Env.v().getApp().getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

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
     * core is the core algorithm of fic-finder:
     *
     *   for each api-context model ac in model list do
     *     if (model does not satisfy FIC conditions)
     *       continue
     *     fi
     *
     *     callSitesTree = createTree(model)                # creation
     *     callSitesTree = pruneTree(callSitesTree, model)  # pruning
     *     issues = searchTree(callSitesTree, model)        # searching
     *
     *     emit all issues
     *   done
     *
     */
    private void core() {
        logger.i("Core Algorithm goes here");

        Set<ApiContext> models = Env.v().getModels();
        MultiTree<CallSites> callSitesTree;
        List<Issue> issues;

        for (ApiContext model : models) {
            if (!maybeFICable(model)) {
                continue ;
            }

            // compute call site tree using model and global environment
            callSitesTree = this.computeCallSitesTree(model);

            // execute tree pruning(cut all fixed call sites)
            this.pruneCallSitesTree(callSitesTree, model);

            // if all children of root is cut, then we know that, all issues are fixed
            if (0 == callSitesTree.getRoot().getChildren().size()) { continue ; }

            // search issues in call sites tree
            issues = this.searchCallSitesTree(callSitesTree, model);

            // emit the issues found
            issues.forEach(i -> Env.v().emit(i));
        }
    }

    /**
     * computeCallSitesTree computes all call sites of a specific api and its 0~k-indirect-caller,
     * in the call site tree, the child node saves all call sites of its parent node
     *
     */
    private MultiTree<CallSites> computeCallSitesTree(ApiContext model) {
        Scene scene = Scene.v();

        String type = model.getApi().getClass().toString().split(" ")[1];
        MultiTree<CallSites> callSites = null;

        try {
            // get call sites of the specific api
            switch (type) {
                case ApiField.TAG: {
                    // TODO I have no idea how to get the call site of a specific field

                    logger.w("Model of @field is not supported by now");

                    // ApiField apiField = (ApiField) model.getApi();
                    // SootField sootField = scene.getField(apiField.getSiganiture());

                    break;
                }

                case ApiMethod.TAG: {
                    ApiMethod  apiMethod  = (ApiMethod) model.getApi();
                    SootMethod sootMethod = scene.getMethod(apiMethod.getSiganiture());

                    // we create a virtual node as root, meaning that we mark the api as a caller,
                    // then we will use it to compute its children, thus its call sites
                    CallSites root = new CallSites(null, sootMethod);
                    // compute its children, thus its call sites
                    callSites = new MultiTree<>(computeCallSites(new MultiTree.Node<>(root), 0));

                    break;
                }

                case ApiIface.TAG: {
                    // TODO I have no idea how to get the call site of a specific interface

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

        return callSites;
    }

    /**
     * computeCallSites computes all call sites of calleeNode, thus find all its children,
     * this is not a pure function, because calleeNode will be added its children
     *
     */
    private MultiTree.Node<CallSites> computeCallSites(MultiTree.Node<CallSites> calleeNode, int level) {
        if (null == calleeNode || null == calleeNode.getData()) { return null; }

        CallGraph callGraph = Scene.v().getCallGraph();
        SootMethod callee = calleeNode.getData().getCaller();

        // we firstly clarify all its call sites by the caller method
        Iterator<Edge> edgeIterator = callGraph.edgesInto(callee);
        Map<SootMethod, CallSites> callers = new HashMap<>(1);
        while (edgeIterator.hasNext()) {
            Edge edge = edgeIterator.next();
            SootMethod caller = edge.src();
            Unit callSite = edge.srcUnit();

            if (callers.containsKey(caller)) {
                callers.get(caller).addCallSite(callSite);
            } else {
                CallSites callSites = new CallSites(callee, caller, callSite);
                callers.put(caller, callSites);
            }
        }

        // then we create a node for each caller method, and compute its call sites if necessary
        for (Map.Entry<SootMethod, CallSites> entry : callers.entrySet()) {
            MultiTree.Node<CallSites> callSitesNode = new MultiTree.Node<>(entry.getValue());

            // computing done until the K-INDIRECT-CALLER
            if (level < Env.ENV_K_INDIRECT_CALLER) {
                computeCallSites(callSitesNode, level + 1);
            }

            calleeNode.addChild(callSitesNode);
        }

        return calleeNode;
    }

    /**
     * pruneCallSitesTree cuts all fixed call site as well as the node
     *
     */
    private void pruneCallSitesTree(MultiTree<CallSites> callSitesTree, ApiContext model) {
        List<MultiTree.Node<CallSites>> nodesToBeCut = new ArrayList<>();
        List<Unit> callSitesToBeCut = new ArrayList<>();
        SootMethod caller;
        Set<Unit>  callSites;

        // 1. first cut all fixed call sites
        MultiTree.Node<CallSites> root = callSitesTree.getRoot();
        Queue<MultiTree.Node<CallSites>> queue = new LinkedList<>();

        queue.offer(root);
        while (!queue.isEmpty()) {
            MultiTree.Node<CallSites> currentNode = queue.poll();
            currentNode.getChildren().forEach(c -> queue.offer(c));

            // skip the virtual node root, root is the model itself
            if (currentNode.equals(root)) {
                continue;
            }

            caller = currentNode.getData().getCaller();
            callSites = currentNode.getData().getCallSites();
            callSitesToBeCut.clear();

            for (Unit callSite : callSites) {
                Set<Unit> slicing = runBackwardSlicingFor(caller, callSite);
                for (Unit slice : slicing) {
                    if (canHandleIssue(model, slice)) {
                        callSitesToBeCut.add(callSite);
                        break;
                    }
                }
            }

            if (callSitesToBeCut.size() == callSites.size()) {
                callSites.clear();
                nodesToBeCut.add(currentNode);
            } else {
                for (Unit u : callSitesToBeCut) { callSites.remove(u); }
            }
        }

        // 2. cut all fixed node
        for (MultiTree.Node<CallSites> n : nodesToBeCut) { callSitesTree.remove(n.getData()); }
    }

    /**
     * searchCallSitesTree searches all issues(the fic path)
     *
     */
    private List<Issue> searchCallSitesTree(MultiTree<CallSites> callSitesTree, ApiContext model) {
        if (callSitesTree.isEmpty()) { return new ArrayList<>(); }
        return searchIssuesInCallSitesNode(callSitesTree.getRoot(), callSitesTree.getRoot(), model);
    }

    /**
     * searchIssuesInCallSitesNode recursively searches issues of a call sites node
     *
     */
    private List<Issue> searchIssuesInCallSitesNode(MultiTree.Node<CallSites> root,
                                                    MultiTree.Node<CallSites> n,
                                                    ApiContext model) {
        final SootMethod caller = n.getData().getCaller();
        final Set<Unit> callSites = n.getData().getCallSites();
        final List<Issue.CallerPoint> callerPoints = new ArrayList<>();
        final List<Issue> issues = new ArrayList<>();

        callSites.forEach(u -> {
            callerPoints.add(new Issue.CallerPoint(
                    "~",
                    u.getJavaSourceStartLineNumber(),
                    u.getJavaSourceStartColumnNumber(),
                    caller.getSignature()));
        });


        if (0 == n.getChildren().size()) {
            callerPoints.forEach(si -> {
                Issue issue = new Issue(model);
                issue.addCallPoint(si);
                issues.add(issue);
            });
        } else {
            for (MultiTree.Node<CallSites> c : n.getChildren()) {
                List<Issue> issuesOfC = searchIssuesInCallSitesNode(root, c, model);
                if (n.equals(root)) {
                    // root is a virtual node, just add all issues of its children
                    issues.addAll(issuesOfC);
                    issues.forEach(i -> i.setCalleePoint(new Issue.CalleePoint(caller.getSignature())));
                } else {
                    // new issues are issuesOfC X subIssues (Cartesian Product)
                    callerPoints.forEach(si -> {
                        List<Issue> cloneIssues = new ArrayList<> (issuesOfC.size());
                        issuesOfC.forEach(i -> {
                            try {
                                cloneIssues.add((Issue) i.clone());
                            } catch (CloneNotSupportedException e) {

                            }
                        });
                        cloneIssues.forEach(ci -> ci.addCallPoint(si));
                        issues.addAll(cloneIssues);
                    });
                }
            }
        }

        return issues;
    }

    /**
     * maybeFICable checks whether the call site is ficable i.e. may generate FIC issues
     *
     */
    private boolean maybeFICable(ApiContext model) {
        // compiled sdk version, used to check whether an api
        // is accessible or not
        final int targetSdk = Env.v().getManifest().targetSdkVersion();
        final int minSdk = Env.v().getManifest().getMinSdkVersion();

        return model.hasBadDevices() || !model.matchApiLevel(targetSdk, minSdk);
    }

    /**
     * canHandleIssue checks whether the stmt can handle the specific issue
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

            if ((model.needCheckApiLevel() || model.needCheckSystemVersion()) &&
                    (Strings.containsIgnoreCase(siganiture,
                            "android.os.Build$VERSION: int SDK_INT",
                            "android.os.Build$VERSION: java.lang.String SDK"))) {
                return true;
            }

            if (model.hasBadDevices() &&
                    Strings.containsIgnoreCase(siganiture,
                            "android.os.Build: java.lang.String BOARD",
                            "android.os.Build: java.lang.String BRAND",
                            "android.os.Build: java.lang.String DEVICE",
                            "android.os.Build: java.lang.String PRODUCT")) {
                return true;
            }
        }


        return false;
    }

    /**
     * runBackwardSlicingFor runs backward slicing algorithm.
     *
     * we find the backward slicing of a unit by:
     *  1. get the corresponding pdg, which describes the unit's method
     *  2. find the dependents of callerUnit
     *  3. find the nearest IfStmt of the caller unit
     *  4. find the dependents of IfStmt(which defines the variable used in IfStmt)
     *
     */
    private Set<Unit> runBackwardSlicingFor(SootMethod caller, Unit callerUnit) {
        Set<Unit> slice = new HashSet<>(128);

        // 1. get the corresponding pdg, which describes the unit's method
        ProgramDependenceGraph pdg = Env.v().getPDG(caller.getSignature());

        // 2. find the dependents of callerUnit
        if (pdg != null) {
            slice.addAll(findDependentOf(pdg, callerUnit));
        }

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
     * findKNeighbors finds K IfStmt neighbors
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
     * findNodeOf finds node of unit in pdg
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
     * findDependentOf finds dependent units of unit unit
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
