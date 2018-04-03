package simonlee.elegant.core.finder.plainfinder;

import simonlee.elegant.Container;
import simonlee.elegant.core.environment.Environment;
import simonlee.elegant.core.finder.AbstractFinder;
import simonlee.elegant.core.finder.CallSites;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.models.api.ApiMethod;
import simonlee.elegant.utils.Logger;
import simonlee.elegant.utils.MultiTree;
import simonlee.elegant.utils.Soots;
import simonlee.elegant.utils.Strings;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;

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
 *     emitIssue all issues
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

    // issue types definitions
    private static final int NO_FIC_ISSUES                 = 0x0;
    private static final int DEVICE_SPECIFIC_FIC_ISSUE     = 0x1;
    private static final int NON_DEVICE_SPECIFIC_FIC_ISSUE = 0x2;
    private static final int BOTH_FIC_ISSUE                = DEVICE_SPECIFIC_FIC_ISSUE | NON_DEVICE_SPECIFIC_FIC_ISSUE;

    // callSitesTree is a call site tree for the detected model
    private MultiTree<CallSites> callSitesTree;
    // issueType is the fic issue type of the detected model
    private int issueType = NO_FIC_ISSUES;

    public PFinder(Container container, Set<ApiContext> models) {
        super(container, models);
    }

    @Override
    protected void setUp() {
        this.container.watchIssues(new PIssueHandle(this.container));
    }

    // We will use create_Tree in detection phase
    @Override
    protected boolean detect(ApiContext model) {
        // TODO eliminate this condition to support iface and field
        if (!(model.getApi() instanceof ApiMethod)) {
            return false;
        }

        issueType = ficIssueGetType(model);

        if (NO_FIC_ISSUES == issueType) {
            return false;
        }

        try {
            ApiMethod  apiMethod  = (ApiMethod) model.getApi();
            SootMethod sootMethod = Scene.v().getMethod(apiMethod.getSignature());

            // we create a virtual node as root, meaning that we mark the api as a caller,
            // then we will use it to compute its children, thus its call sites
            CallSites root = new CallSites(null, sootMethod);
            // compute its children, thus its call sites
            callSitesTree = new MultiTree<>(computeCallSitesRoot(new MultiTree.Node<>(root), 0));
        } catch (Exception e) {
            callSitesTree = null;
        }

        return null != callSitesTree;
    }

    // We will use prune_Tree in validation phase
    @Override
    protected boolean validate(ApiContext model) {
        // TODO eliminate this condition to support iface and field
        if (!(model.getApi() instanceof ApiMethod)) {
            return false;
        }

        CallGraph    cg   = this.container.getCallGraph();
        IInfoflowCFG icfg = this.container.getInterproceduralCFG();

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
                Set<Unit> slicing = Soots.findBackwardSlcing(callSite, cg, icfg);
                for (Unit aSlicing : slicing) {
                    if (canHandleIssue(model, issueType, aSlicing)) {
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
        return 0 != callSitesTree.getRoot().getChildren().size();
    }

    // We will use genPathes_Tree in generation phase
    @Override
    protected void generate(ApiContext model) {
        // TODO eliminate this condition to support iface and field
        if (!(model.getApi() instanceof ApiMethod)) {
            return;
        }

        // search issues in call sites tree
        List<PIssue> pIssues = this.searchIssuesInCallSitesNode(
                callSitesTree.getRoot(), callSitesTree.getRoot(), model);

        // emitIssue the issues found
        pIssues.forEach(i -> this.container.emitIssue(i));
    }

    // ficIssueGetType checks whether the call site is ficable i.e. may generate FIC issues
    private int ficIssueGetType(ApiContext model) {
        // compiled sdk version, used to check whether an api
        // is accessible or not
        final int targetSdk = this.container.getManifest().targetSdkVersion();
        final int minSdk    = this.container.getManifest().getMinSdkVersion();

        int result = NO_FIC_ISSUES;

        result |= model.hasBadDevices() ? DEVICE_SPECIFIC_FIC_ISSUE : NO_FIC_ISSUES;
        result |= model.matchApiLevel(targetSdk, minSdk) ? NO_FIC_ISSUES : NON_DEVICE_SPECIFIC_FIC_ISSUE;

        return result;
    }

    // computeCallSitesRoot computes all call sites of calleeNode, thus find all its children,
    // this is not a pure function, because calleeNode will be added its children
    private MultiTree.Node<CallSites> computeCallSitesRoot(MultiTree.Node<CallSites> calleeNode, int level) {
        if (null == calleeNode || null == calleeNode.getData()) { return null; }

        SootMethod callee = calleeNode.getData().getCaller();

        // we firstly clarify all its call sites by the caller method
        Map<SootMethod, CallSites> callers = Soots.findCallSites(
                callee,
                Scene.v().getCallGraph(),
                Scene.v().getClasses(),
                Arrays.asList(this.container.getAppPackage()));

        // then we create a node for each caller method, and compute its call sites if necessary
        for (Map.Entry<SootMethod, CallSites> entry : callers.entrySet()) {
            MultiTree.Node<CallSites> callSitesNode = new MultiTree.Node<>(entry.getValue());

            // computing done until the K-INDIRECT-CALLER
            if (level < Environment.ENV_K_INDIRECT_CALLER) {
                computeCallSitesRoot(callSitesNode, level + 1);
            }

            calleeNode.addChild(callSitesNode);
        }

        return calleeNode;
    }

    // canHandleIssue checks whether the stmt can handle the specific issue
    private boolean canHandleIssue(ApiContext model, int issueType, Unit aSlicing) {
        switch (issueType) {
            case NO_FIC_ISSUES:
                return true;
            case NON_DEVICE_SPECIFIC_FIC_ISSUE:
                return canHandleNonDeviceSpecificIssue(model, aSlicing);
            case DEVICE_SPECIFIC_FIC_ISSUE:
                return canHandleDeviceSpecificIssue(model, aSlicing);
            case BOTH_FIC_ISSUE:
                return canHandleDeviceSpecificIssue(model, aSlicing) &&
                        canHandleNonDeviceSpecificIssue(model, aSlicing);
            default:
                logger.w("Illegal issue type " + issueType);
                return false;
        }
    }

    // canHandleDeviceSpecificIssue checks whether the stmt can handle the device specific issue
    private boolean canHandleDeviceSpecificIssue(ApiContext model, Unit aSlicing) {
        String[]  devices = model.getContext().getBadDevices();

        String s = aSlicing.toString();
        if (Strings.contains(s,
                "android.os.Build: java.lang.String BOARD",
                "android.os.Build: java.lang.String BRAND",
                "android.os.Build: java.lang.String DEVICE",
                "android.os.Build: java.lang.String PRODUCT",
                "android.os.Build: java.lang.String MANUFACTURER",
                "android.os.Build: java.lang.String MODEL",
                "BOARD",
                "BRAND",
                "DEVICE",
                "PRODUCT",
                "MANUFACTURER",
                "MODEL")) {
            return true;
        } else {
            for (String device : devices) {
                if (s.toLowerCase().contains(device.toLowerCase())) {
                    return true;
                }
            }
        }

        return false;
    }

    // canHandleNonDeviceSpecificIssue checks whether the stmt can handle the non device specific issue
    private boolean canHandleNonDeviceSpecificIssue(ApiContext model, Unit aSlicing) {
        if (!(aSlicing instanceof Stmt)) {
            return false;
        }

        // developers may use a function to check api, in this case, we only consider 1-depth invoking
        if (((Stmt) aSlicing).containsInvokeExpr()) {
            try {
                List<Unit> units = new ArrayList<>(((Stmt) aSlicing).getInvokeExpr().getMethod().getActiveBody().getUnits());
                for (Unit u : units) {
                    if (!(u instanceof Stmt)) { continue; }
                    if (((Stmt) u).containsInvokeExpr()) { continue; }
                    if (canHandleNonDeviceSpecificIssue(model, u)) { return true; }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        } else {
            return (aSlicing instanceof Stmt) &&
                    (model.needCheckApiLevel() || model.needCheckSystemVersion()) &&
                    Strings.containsIgnoreCase(aSlicing.toString(),
                            "android.os.Build$VERSION: int SDK_INT",
                            "android.os.Build$VERSION: java.lang.String SDK",
                            "android.os.Build: long TIME",
                            "android.os.Build$VERSION_CODES",
                            "VERSION_CODES",
                            "VERSION",
                            "SDK_INT",
                            "SDK",
                            "TIME");
        }

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
                    caller.getDeclaringClass().getName(),
                    u.getJavaSourceStartLineNumber(),
                    u.getJavaSourceStartColumnNumber(),
                    caller.getName()));
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
