package com.example.ficfinder.finder.plainfinder;

import com.example.ficfinder.Env;
import com.example.ficfinder.finder.AbstractFinder;
import com.example.ficfinder.finder.CallSites;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.ApiMethod;
import com.example.ficfinder.tracker.Tracker;
import com.example.ficfinder.utils.Logger;
import com.example.ficfinder.utils.MultiTree;
import com.example.ficfinder.utils.Soots;
import com.example.ficfinder.utils.Strings;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

import java.util.*;

/**
 * the core algorithm of PlainFinder:
 *
 *   for each api-context model ac in model list do
 *     if (model does not satisfy FIC conditions)
 *       continue
 *     fi
 *
 *     callSitesTree = create_Tree(model)                # creation
 *     callSitesTree = prune_Tree(callSitesTree, model)  # pruning
 *     issues = genPathes_Tree(callSitesTree, model)     # generating
 *
 *     emit all issues
 *   done
 *
 *   function create_Tree(callgraph, api) {
 *     root = new Root({ caller: api })
 *
 *     clarify all call sites of api by every call site's method
 *     add all methods to root as root.method
 *
 *     foreach method m in root.methods do
 *       childnode = create_Tree(callgraph, m)
 *       append childnode to root as a child node
 *     done
 *   done
 *
 *   function prune_Tree(t)
 *     queue = Queue(t.root)
 *
 *     # we use BFS to traverse the tree
 *     while queue is not empty do
 *       node = queue.deque()
 *       foreach child node c in node.getChildren() do
 *         queue.enque(c)
 *       done
 *
 *       foreach call site cs in node.getCallSites() do
 *         slicing = runBackgroundSlicing(cs)
 *         foreach slice s in slicing do
 *           if (s can fix issue) then
 *             delete cs in node
 *             break
 *           fi
 *         done
 *       done
 *
 *       if node.getCallSites() == empty then
 *         while 1 == node.getParent().getChildren().size() and node.getParent() != t.root do
 *           node = node.getParent()
 *         done
 *         delete node in t
 *       fi
 *     done
 *   done
 *
 *   function genPathes_Tree(t)
 *     pathes = []
 *
 *     foreach child node n in t.getChlidren do
 *       genPathes_Tree(n)
 *         .map(p, pathes.add(p.clone().append(t.caller)))
 *     done
 *
 *     return pathes
 *   done
 *
 */
public class PFinder extends AbstractFinder {

    private Logger logger = new Logger(PFinder.class);

    // callSitesTree is a call site tree for the detected model
    private MultiTree<CallSites> callSitesTree;

    public PFinder(Set<ApiContext> models) {
        super(models);
    }

    @Override
    public void setUp() {
        Tracker.v().subscribe(new PIssueHandle());
    }

    // We will use create_Tree in detection phase
    @Override
    public boolean detect(ApiContext model) {
        if (!(model.getApi() instanceof ApiMethod)) {
            return false;
        }

        if (!maybeFICable(model)) {
            return false;
        }

        try {
            ApiMethod  apiMethod  = (ApiMethod) model.getApi();
            SootMethod sootMethod = Scene.v().getMethod(apiMethod.getSignature());

            // we create a virtual node as root, meaning that we mark the api as a caller,
            // then we will use it to compute its children, thus its call sites
            CallSites root = new CallSites(null, sootMethod);
            // compute its children, thus its call sites
            callSitesTree = new MultiTree<>(computeCallSites(new MultiTree.Node<>(root), 0));
        } catch (Exception e) {
            logger.w("Cannot find `" + model.getApi().getSignature() + "`");
            callSitesTree = null;
        }

        return null != callSitesTree;
    }

    // We will use prune_Tree in validation phase
    @Override
    public boolean validate(ApiContext model) {
        if (!(model.getApi() instanceof ApiMethod)) {
            return false;
        }

        List<MultiTree.Node<CallSites>> nodesToBeCut     = new ArrayList<>();
        List<Unit>                      callSitesToBeCut = new ArrayList<>();
        SootMethod                      caller           = null;
        Set<Unit>                       callSites        = null;

        // 1. first cut all fixed call sites
        MultiTree.Node<CallSites>        root  = callSitesTree.getRoot();
        Queue<MultiTree.Node<CallSites>> queue = new LinkedList<>();

        queue.offer(root);
        while (!queue.isEmpty()) {
            // we will firstly do pruning, and then add its children to queue accordingly
            MultiTree.Node<CallSites> currentNode = queue.poll();

            // skip the virtual node root, root is the model itself
            if (currentNode.equals(root)) {
                currentNode.getChildren().forEach(c -> queue.offer(c));
                continue;
            }

            caller    = currentNode.getData().getCaller();
            callSites = currentNode.getData().getCallSites();
            callSitesToBeCut.clear();

            for (Unit callSite : callSites) {
                Map<Object, Set<Unit>>            slicing         = runBackwardSlicingFor(caller, callSite);
                Set<Map.Entry<Object, Set<Unit>>> partialSlicings = slicing.entrySet();
                for (Map.Entry<Object, Set<Unit>> paritialSlicing : partialSlicings) {
                    if (canHandleIssue(model, paritialSlicing)) {
                        callSitesToBeCut.add(callSite);
                        break;
                    }
                }
            }

            if (callSitesToBeCut.size() == callSites.size()) {
                // current node will be cut, we don't need to traverse its children
                callSites.clear();
                nodesToBeCut.add(currentNode);
            } else {
                for (Unit u : callSitesToBeCut) { callSites.remove(u); }
                currentNode.getChildren().forEach(c -> queue.offer(c));
            }
        }

        // 2. cut all fixed node
        for (MultiTree.Node<CallSites> n : nodesToBeCut) {
            // parent has no other children
            while (1 == n.getParent().getChildren().size() && !n.getParent().equals(callSitesTree.getRoot())) {
                n = n.getParent();
            }
            callSitesTree.remove(n.getData());
        }

        // if all children of root is cut, then we know that, all issues are fixed
        if (0 == callSitesTree.getRoot().getChildren().size()) { return false; }

        return true;
    }

    // We will use genPathes_Tree in generation phase
    @Override
    public void generate(ApiContext model) {
        if (!(model.getApi() instanceof ApiMethod)) {
            return;
        }

        // search issues in call sites tree
        List<PIssue> pIssues = this.searchIssuesInCallSitesNode(
                callSitesTree.getRoot(), callSitesTree.getRoot(), model);

        // emit the issues found
        pIssues.forEach(i -> Env.v().emit(i));
    }

    // maybeFICable checks whether the call site is ficable i.e. may generate FIC issues
    private boolean maybeFICable(ApiContext model) {
        // compiled sdk version, used to check whether an api
        // is accessible or not
        final int targetSdk = Env.v().getManifest().targetSdkVersion();
        final int minSdk    = Env.v().getManifest().getMinSdkVersion();

        return model.hasBadDevices() || !model.matchApiLevel(targetSdk, minSdk);
    }

    // computeCallSites computes all call sites of calleeNode, thus find all its children,
    // this is not a pure function, because calleeNode will be added its children
    private MultiTree.Node<CallSites> computeCallSites(MultiTree.Node<CallSites> calleeNode, int level) {
        if (null == calleeNode || null == calleeNode.getData()) { return null; }

        CallGraph  callGraph = Scene.v().getCallGraph();
        SootMethod callee    = calleeNode.getData().getCaller();

        // we firstly clarify all its call sites by the caller method
        Iterator<Edge>             edgeIterator = callGraph.edgesInto(callee);
        Map<SootMethod, CallSites> callers      = new HashMap<>(1);
        while (edgeIterator.hasNext()) {
            Edge       edge     = edgeIterator.next();
            SootMethod caller   = edge.src();
            Unit       callSite = edge.srcUnit();

            if (callers.containsKey(caller)) {
                callers.get(caller).addCallSite(callSite);
            } else {
                callers.put(caller, new CallSites(callee, caller, callSite));
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
     * runBackwardSlicingFor runs backward slicing algorithm.
     *
     * we find the backward slicing of a unit by:
     *  1. get the corresponding pdg, which describes the unit's method
     *  2. find the dependents of callerUnit
     *  3. find the nearest IfStmt of the caller unit
     *  4. find the dependents of IfStmt(which defines the variable used in IfStmt)
     *
     * @param caller
     * @param callerUnit
     */
    private Map<Object, Set<Unit>> runBackwardSlicingFor(SootMethod caller, Unit callerUnit) {
        Map<Object, Set<Unit>> slice = new HashMap<>();

        // 1. get the corresponding pdg, which describes the unit's method
        ProgramDependenceGraph pdg = Env.v().getPDG(caller.getSignature());

        // 2. find the dependents of callerUnit
        if (pdg != null) {
            slice.put(0, Soots.findDependentOf(pdg, callerUnit));
        } else {
            slice.put(0, new HashSet<>());
        }

        // 3. find the nearest K IfStmt of the caller unit
        List<Unit>                         unitsOfCaller     = new ArrayList<>(caller.getActiveBody().getUnits());
        List<Map.Entry<Integer, Unit>>     kIfStmtNeightbors = findKNeighbors(unitsOfCaller, callerUnit);
        Iterator<Map.Entry<Integer, Unit>> iterator          = kIfStmtNeightbors.iterator();

        // 4. find the dependents of each IfStmt(which defines the variable used in IfStmt)
        while (iterator.hasNext()) {
            Map.Entry<Integer, Unit> entry  = iterator.next();
            int                      ifIdx  = entry.getKey();
            Unit                     ifUnit = entry.getValue();

            if (ifUnit != null) {
                IfStmt ifStmt = (IfStmt) ifUnit;
                slice.put(ifStmt, new HashSet<>());
                // get use boxes, e.g. [$v1, $v2] of `if $v1 > $v2 goto label 0`
                // we assume here that, an IfStmt will use only two values
                Value leftV  = null;
                Value rightV = null;

                try {
                    leftV = ifStmt.getCondition().getUseBoxes().get(0).getValue();
                } catch (Exception e) {
                    leftV = null;
                }

                try {
                    rightV = ifStmt.getCondition().getUseBoxes().get(1).getValue();
                } catch (Exception e) {
                    rightV = null;
                }

                // traverse to find the definition of them
                for (int i = ifIdx - 1; i >= 0; i --) {
                    Unit u = unitsOfCaller.get(i);
                    // $vx = ...
                    if (u instanceof AssignStmt) {
                        AssignStmt assignStmt = (AssignStmt) u;
                        List<ValueBox> defBoxesOfAssignUnit = assignStmt.getDefBoxes();
                        // check whether $vx in useBoxesOfIfUnit, if yes, then add it to slice
                        for (ValueBox vb : defBoxesOfAssignUnit) {
                            if (vb.getValue() instanceof Local &&
                                    (vb.getValue().equals(leftV) || vb.getValue().equals(rightV))) {
                                slice.get(ifStmt).add(assignStmt);
                                if (((AssignStmt) u).containsInvokeExpr()) {
                                    try {
                                        slice.get(0).addAll(((AssignStmt) u).getInvokeExpr().getMethod().getActiveBody().getUnits());
                                    } catch (Exception e) {
                                        // do nothing, for those without an active body
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        return slice;
    }

    // findKNeighbors finds K IfStmt neighbors
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

    // canHandleIssue checks whether the stmt can handle the specific issue
    public boolean canHandleIssue(ApiContext model, Map.Entry<Object, Set<Unit>> paritialSlicing) {
        Object    key     = paritialSlicing.getKey();
        Set<Unit> slicing = paritialSlicing.getValue();

        if (key instanceof IfStmt) {
            // definitions of values used in if stmt
            return canHandleIssue_IfStmt(model, (IfStmt) key, slicing);
        }

        // slicing of common callsites
        return canHandleIssue_Common(model, slicing);
    }

    // canHandleIssue_IfStmt checks whether the IfStmt s will fix the issue or not
    private boolean canHandleIssue_IfStmt(ApiContext model, IfStmt s, Set<Unit> slicing) {
        List<ValueBox> useBoxes = s.getCondition().getUseBoxes();
        Value          leftV    = useBoxes.get(0).getValue();
        Value          rightV   = useBoxes.get(1).getValue();

        if (leftV instanceof Constant && rightV instanceof Local) {
            Constant   c  = (Constant) leftV;
            Local      v  = (Local) rightV;
            AssignStmt as = slicing.iterator().hasNext() ? (AssignStmt) slicing.iterator().next() : null;

            if ((c instanceof IntConstant) &&
                    (((IntConstant)c).value == 0 || ((IntConstant)c).value == 1) &&
                    canHandleIssue_IfStmt_VariableConstant01(model, as)) {
                return true;
            } else if ((c instanceof IntConstant) &&
                    canHandleIssue_IfStmt_VariableConstantNot01(model, c, as)) {
                return true;
            }
        } else if (rightV instanceof Constant && leftV instanceof Local) {
            Local      v  = (Local) leftV;
            Constant   c  = (Constant) rightV;
            AssignStmt as = slicing.iterator().hasNext() ? (AssignStmt) slicing.iterator().next() : null;

            if ((c instanceof IntConstant) &&
                    (((IntConstant)c).value == 0 || ((IntConstant)c).value == 1) &&
                    canHandleIssue_IfStmt_VariableConstant01(model, as)) {
                return true;
            } else if ((c instanceof IntConstant) &&
                    canHandleIssue_IfStmt_VariableConstantNot01(model, c, as)) {
                return true;
            }
        }

        return false;
    }

    // canHandleIssue_IfStmt checks whether the other Stmt s contained in the slicing will fix the issue or not
    private boolean canHandleIssue_Common(ApiContext model, Set<Unit> slicing) {
        for (Unit unit : slicing) {
            // we only parse Jimple
            if (!(unit instanceof AssignStmt)) {
                return false;
            }

            if (canHandleIssue_Common_UnitContainsSpecStrings(model, (AssignStmt) unit)) {
                return true;
            }
        }

        return false;
    }

    private boolean canHandleIssue_IfStmt_VariableConstant01(ApiContext model, AssignStmt defStmt) {
        if (null != defStmt && defStmt.containsInvokeExpr()) {
            InvokeExpr invokeExpr = defStmt.getInvokeExpr();
            List<Value> values = invokeExpr.getArgs();
            for (Value value : values) {
                if (value instanceof Constant && canHandleIssue_IfStmt_ArgMatchApiContext(model, (Constant) value)) {
                    return true;
                }
            }
        }

        return false;
    }

    private boolean canHandleIssue_IfStmt_VariableConstantNot01(ApiContext model, Constant constant, AssignStmt defStmt) {
        if (null != defStmt && canHandleIssue_IfStmt_ArgMatchApiContext(model, constant) &&
                canHandleIssue_Common_UnitContainsSpecStrings(model, defStmt)) {
            return true;
        }

        return false;
    }

    private boolean canHandleIssue_IfStmt_ArgMatchApiContext(ApiContext model, Constant constant) {
        if (constant instanceof IntConstant) {
            int sdkInt = ((IntConstant) constant).value;
            return model.getContext().getMinApiLevel() <= sdkInt && sdkInt <= model.getContext().getMaxApiLevel();
        } else if (constant instanceof FloatConstant) {
            float sysVer = ((FloatConstant) constant).value;
            return model.getContext().getMinSystemVersion() <= sysVer && sysVer <= model.getContext().getMaxSystemVersion();
        }

        return false;
    }

    private boolean canHandleIssue_Common_UnitContainsSpecStrings(ApiContext model, AssignStmt stmt) {
        try {
            // if the state contains a method, we assume that most develops won't check api just using 1+ method invoking.
            if (stmt.containsInvokeExpr()) {
                if (canHandleIssue_Common_UnitContainsSpecStrings_Checking(
                        model, stmt.getInvokeExpr().getMethod().getActiveBody().toString())) {
                    return true;
                }
            }

            // check use boxes
            List<ValueBox> useValueBoxes = stmt.getUseBoxes();

            for (ValueBox vb : useValueBoxes) {
                if (canHandleIssue_Common_UnitContainsSpecStrings_Checking(
                        model, vb.getValue().toString())) {
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean canHandleIssue_Common_UnitContainsSpecStrings_Checking(ApiContext model, String s) {
        if ((model.needCheckApiLevel() || model.needCheckSystemVersion()) &&
                (Strings.containsIgnoreCase(s,
                        "android.os.Build$VERSION: int SDK_INT",
                        "android.os.Build$VERSION: java.lang.String SDK"))) {
            return true;
        }

        if (model.hasBadDevices() &&
                Strings.containsIgnoreCase(s,
                        "android.os.Build: java.lang.String BOARD",
                        "android.os.Build: java.lang.String BRAND",
                        "android.os.Build: java.lang.String DEVICE",
                        "android.os.Build: java.lang.String PRODUCT")) {
            return true;
        }

        return false;
    }

    // searchIssuesInCallSitesNode recursively searches issues of a call sites node
    private List<PIssue> searchIssuesInCallSitesNode(MultiTree.Node<CallSites> root,
                                                     MultiTree.Node<CallSites> n,
                                                     ApiContext model) {
        final SootMethod              caller       = n.getData().getCaller();
        final Set<Unit>               callSites    = n.getData().getCallSites();
        final List<PIssue.CallerPoint> callerPoints = new ArrayList<>();
        final List<PIssue> pIssues = new ArrayList<>();

        callSites.forEach(u -> {
            callerPoints.add(new PIssue.CallerPoint(
                    "~",
                    u.getJavaSourceStartLineNumber(),
                    u.getJavaSourceStartColumnNumber(),
                    caller.getSignature()));
        });


        if (0 == n.getChildren().size()) {
            callerPoints.forEach(si -> {
                PIssue pIssue = new PIssue(model);
                pIssue.addCallPoint(si);
                pIssues.add(pIssue);
            });
        } else {
            for (MultiTree.Node<CallSites> c : n.getChildren()) {
                List<PIssue> issuesOfC = searchIssuesInCallSitesNode(root, c, model);
                if (n.equals(root)) {
                    // root is a virtual node, just add all issues of its children
                    pIssues.addAll(issuesOfC);
                    pIssues.forEach(i -> i.setCalleePoint(new PIssue.CalleePoint(caller.getSignature())));
                } else {
                    // new issues are issuesOfC X subIssues (Cartesian Product)
                    callerPoints.forEach(si -> {
                        List<PIssue> clonePIssues = new ArrayList<> (issuesOfC.size());
                        issuesOfC.forEach(i -> {
                            try {
                                clonePIssues.add((PIssue) i.clone());
                            } catch (CloneNotSupportedException e) {
                                // do nothing here
                            }
                        });
                        clonePIssues.forEach(ci -> ci.addCallPoint(si));
                        pIssues.addAll(clonePIssues);
                    });
                }
            }
        }

        return pIssues;
    }
}
