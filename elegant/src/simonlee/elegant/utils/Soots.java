package simonlee.elegant.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simonlee.elegant.d3algo.AbstractD3Algo;
import simonlee.elegant.finder.CallSites;
import soot.*;
import soot.jimple.*;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.*;
import soot.toolkits.graph.pdg.HashMutablePDG;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;
import soot.util.Chain;

import java.util.*;

public class Soots {

    private static Logger logger = LoggerFactory.getLogger(Soots.class);

    private static final String CLASS_STATIC_CODE_BLOCK_METHOD_NAME = "<clinit>";
    private static final String CLASS_CODE_BLOCK_METHOD_NAME        = "<init>";

    // invokingStmtsCache, as a cache, stores all statements that will invoke a method
    // the key is the method where the statement is
    private static Map<SootMethod, Set<Unit>> invokingStmtsCache = new HashMap<>(256);

    /**
     * findLatestDefinition will find the latest definition unit of value v at unit u in method m
     *
     * @param v the variable who wants to find its latest definition
     * @param u the unit where the variable lives at
     * @param m the method where the variable lives at
     * @return  the latest definition unit of v
     */
    public static Unit findLatestDefinition(Value v, Unit u, SootMethod m) {
        if (null == v || null == u || null == m) { return null; }
        if (!(u instanceof Stmt)) { return null; }

        Unit       def   = null;
        List<Unit> units = new ArrayList<>(m.getActiveBody().getUnits());
        int        index = units.indexOf(u);

        if (index != -1) {
            for (int i = index - 1; i >= 0; i --) {
                Stmt s = (Stmt) units.get(i);
                if (s instanceof IdentityStmt || s instanceof AssignStmt) {
                    try {
                        if (v.equals(s.getDefBoxes().get(0).getValue())) {
                            def = s;
                            break;
                        }
                    } catch (NullPointerException e) {
                        // do nothing
                    }
                }
            }
        }

        return def;
    }

    /**
     * findPreviousDefinitions will find all previous definitions unit of value v at unit u in method m
     *
     * @param v the variable who wants to find its previous definitions
     * @param u the unit where the variable lives at
     * @param m the method where the variable lives at
     * @return  the previous definitions unit of v
     */
    public static Set<Unit> findPreviousDefinitions(Value v, Unit u, SootMethod m) {
        if (null == v || null == u || null == m) { return null; }
        if (!(u instanceof Stmt)) { return new HashSet<>(); }

        try {
            Set<Unit>  defs  = new HashSet<>();
            List<Unit> units = new ArrayList<>(m.getActiveBody().getUnits());
            int        index = units.indexOf(u);

            if (index != -1) {
                for (int i = index - 1; i >= 0; i --) {
                    Stmt s = (Stmt) units.get(i);
                    if (s instanceof IdentityStmt || s instanceof AssignStmt) {
                        try {
                            if (v.equals(s.getDefBoxes().get(0).getValue())) {
                                defs.add(s);
                            }
                        } catch (NullPointerException e) {
                            // do nothing
                        }
                    }
                }
            }

            return defs;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    /**
     * findNodeOf finds node of unit u in pdg
     *
     * @param u   the unit who wants to find its PDG node
     * @param pdg the pdg where the unit lives
     * @return    the PDG node where the unit lives at
     */
    public static PDGNode findNodeOf(Unit u, ProgramDependenceGraph pdg) {
        PDGNode node = null;

        for (PDGNode n : pdg) {
            Iterator<Unit> iterator = unitIteratorOfPDGNode(n);
            if (iterator == null) continue;

            while (iterator.hasNext()) {
                if (iterator.next().equals(u)) { node = n; break; }
            }

            if (node != null) { break; }
        }

        return node;
    }

    /**
     * findInternalForwardSlicing finds internal forward slicing units of unit u in pdg
     *
     * @param u   the unit who wants to find its internal forward slicing
     * @param pdg the pdg where the unit lives at
     * @return    the set of forward slicing in the pdg of the unit
     */
    public static Set<Unit> findInternalForwardSlicing(Unit u, ProgramDependenceGraph pdg) {
        Set<Unit> deps = new HashSet<>(128);

        // 1. find the corresponding PDGNode srcNode, which contains the unit
        PDGNode srcNode = findNodeOf(u, pdg);
        if (srcNode == null) { return deps; }

        // 2. find all the dependent PDGNodes of srcNode
        List<PDGNode> depOfCallerUnit = pdg.getDependents(srcNode);

        // 3. get all the units of each dependent PDGNode
        for (PDGNode dependent : depOfCallerUnit) {
            Iterator<Unit> iter = unitIteratorOfPDGNode(dependent);
            while (iter.hasNext()) {
                deps.add(iter.next());
            }
        }

        return deps;
    }

    /**
     * findInternalBackwardSlicing finds internal backward slicing units of unit u in pdg
     *
     * @param u   the unit who wants to find its internal backward slicing
     * @param pdg the pdg where the unit lives at
     * @return    the set of backward slicing in the pdg of the unit
     */
    public static Set<Unit> findInternalBackwardSlicing(Unit u, ProgramDependenceGraph pdg) {
        Set<Unit> internalBackwardSlicing = new HashSet<>(128);

        PDGNode srcNode = findNodeOf(u, pdg);
        List<PDGNode> nodes = srcNode.getBackDependets();

        for (PDGNode n : nodes) {
            Iterator<Unit> iter = unitIteratorOfPDGNode(n);
            while (iter.hasNext()) {
                internalBackwardSlicing.add(iter.next());
            }
        }

        return internalBackwardSlicing;
    }

    /**
     * findDominators find all dominators of u in icfg
     *
     * @param u    the unit who wants to find its dominators
     * @param m    the method where the unit lives at
     * @param icfg the icfg where the unit lives at
     * @return     the set of domonators in the icfg of the unit
     */
    public static Set<Unit> findDominators(Unit u, SootMethod m, IInfoflowCFG icfg) {
        try {
            // TODO - extends to inter-procedural, now only finds the intra- ones using the unit graph
            DirectedGraph<Unit> graph = icfg.getOrCreateUnitGraph(m);
            MHGDominatorsFinder<Unit> mhgDominatorsFinder = new MHGDominatorsFinder<>(graph);
            return new HashSet<>(mhgDominatorsFinder.getDominators(u));
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    /**
     * findImmediateDominator finds the immediate dominator of u in icfg
     *
     * @param u    the unit who wants to find its immediate dominator
     * @param m    the method where the unit lives at
     * @param icfg the icfg where the unit lives at
     * @return     the immediate domonator in the icfg of the unit
     */
    public static Unit findImmediateDominator(Unit u, SootMethod m, IInfoflowCFG icfg) {
        // TODO - extends to inter-procedural, now only finds the intra- ones using the unit graph
        return new MHGDominatorsFinder<>(icfg.getOrCreateUnitGraph(m))
                .getImmediateDominator(u);
    }

    /**
     * findPostDominators find all post dominators of u in icfg
     *
     * @param u    the unit who wants to find its post dominators
     * @param m    the method where the unit lives at
     * @param icfg the icfg where the unit lives at
     * @return     the set of post domonators in the icfg of the unit
     */
    public static Set<Unit> findPostDominators(Unit u, SootMethod m, IInfoflowCFG icfg) {
        // TODO - extends to inter-procedural, now only finds the intra- ones using the unit graph
        DirectedGraph<Unit> graph = icfg.getOrCreateUnitGraph(m);
        MHGPostDominatorsFinder<Unit> mhgPostDominatorsFinder = new MHGPostDominatorsFinder<>(graph);
        return new HashSet<>(mhgPostDominatorsFinder.getDominators(u));
    }

    /**
     * findImmediatePostDominator finds the immediate post dominator of u in icfg
     *
     * @param u    the unit who wants to find its immediate post dominator
     * @param m    the method where the unit lives at
     * @param icfg the icfg where the unit lives at
     * @return     the immediate post domonator in the icfg of the unit
     */
    public static Unit findImmediatePostDominator(Unit u, SootMethod m, IInfoflowCFG icfg) {
        // TODO - extends to inter-procedural, now only finds the intra- ones using the unit graph
        return new MHGPostDominatorsFinder<>(icfg.getOrCreateUnitGraph(m))
                .getImmediateDominator(u);
    }

    /**
     * findBackwardSlicing finds all the backward slicing of a unit
     *
     * @param u      the unit who wants to find its backward slicing
     * @param m      the method where the unit lives at
     * @param cg     the call graph where the unit lives at
     * @param icfg   the icfg where the unit lives at
     * @param d3Algo the d3 algo
     * @return       the backward slicing of the unit in cg and icfg
     */
    public static Set<Unit> findBackwardSlicing(Unit u, SootMethod m, CallGraph cg, IInfoflowCFG icfg, AbstractD3Algo d3Algo) {
        Set<Unit> backwardSlicing = new HashSet<>();

        // a slicing includes the data-flow dependencies and control-flow dependencies
        // firstly we compute the data-flow dependencies using the call graph
        // secondly we compute the control-flow dependencies using the inter-procedural control flow graph

        // 1. data-flow dependencies
        Set<Unit> backwardDataBackwardDependencies = findBackwardDataDependencies(u, m, cg, d3Algo);
        backwardSlicing.addAll(backwardDataBackwardDependencies);

        // 2. control-flow dependencies
        Set<Unit> dominators = findDominators(u, m, icfg);
        for (Unit d : dominators) {
            if (!backwardSlicing.contains(d)) {
                // find data-flow dependencies of this dominator
                backwardSlicing.addAll(findBackwardDataDependencies(d, icfg.getMethodOf(d), cg, d3Algo));
            }
        }
        backwardSlicing.addAll(dominators);

        // 3. we use the built-in backward slicing to get the intra-procedural backward slicing
        try {
            Set<Unit> builtInBackwardSlicing = findInternalBackwardSlicing(u,
                    new HashMutablePDG(new BriefUnitGraph(m.getActiveBody())));
            backwardSlicing.addAll(builtInBackwardSlicing);
        } catch (Exception e) {
            // do nothing here
        }

        // 4. TRICK here: we add all IfStmt before u and its corresponding definitions into slicing,
        //    because most developers will use this method
        try {
            Set<Unit> trickySlicing = findTrickySlicing(u, m);
            backwardSlicing.addAll(trickySlicing);
        } catch (Exception e) {
            // do nothing
        }

        return backwardSlicing;
    }

    /**
     * findCallSites gets the relatively complete set of call sites of a callee, given that the call graph
     * built in soot is incomplete, but firstly, we caches the invoking statements, and then we invoke
     * doFindCallSites to do actual finding.
     *
     * @param callee  the callee who wants to find its call sites
     * @param cg      the call graph needed traversing
     * @param classes the classes needed traversing
     * @param d3Algo  the d3 algo
     * @return        the call sites of callee
     */
    public static Map<SootMethod, CallSites> findCallSites(
            SootMethod callee,
            CallGraph cg,
            Chain<SootClass> classes,
            AbstractD3Algo d3Algo) {
        // firstly, we traverse each soot method's body, caches the invoking statements
        if (invokingStmtsCache.isEmpty()) {
            for (SootClass c : classes) {
                if (d3Algo.is3rdPartyLibClass(c)) { continue; }

                for (SootMethod m : c.getMethods()) {
                    try {
                        Body body = m.getActiveBody();
                        Chain<Unit> units = body.getUnits();
                        for (Unit u : units) {
                            if (!(u instanceof Stmt) || !((Stmt) u).containsInvokeExpr()) {
                                continue;
                            } else if (invokingStmtsCache.containsKey(m)) {
                                invokingStmtsCache.get(m).add(u);
                            } else {
                                invokingStmtsCache.put(m, new HashSet<>());
                            }
                        }
                    } catch (Exception e) {
                        // do nothing, some method may have no body, and a RuntimeException will be thrown
                    }
                }
            }
        }

        // then we get real
        return doFindCallSites(callee, cg, d3Algo);
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
     *
     * @param n the PDGNode to find
     * @return  iterator of n's units
     */
    public static Iterator<Unit> unitIteratorOfPDGNode(PDGNode n) {
        Iterator<Unit> iterator = null;
        PDGNode.Type type = n.getType();

        // get iterator
        if (type.equals(PDGNode.Type.CFGNODE)) {
            iterator = ((Block) n.getNode()).iterator();
        } else if (type.equals(PDGNode.Type.REGION)) {
            iterator = ((IRegion) n.getNode()).getUnitGraph().iterator();
        } else {
            logger.warn("Only REGION and CFGNODE are allowed");
        }

        return iterator;
    }

    // doFindCallSites finds the relatively complete set of call sites of a callee, by:
    // 1. the built-in call graph
    // 2. the classes cached in invokingStmtsCache
    private static Map<SootMethod, CallSites> doFindCallSites(SootMethod callee, CallGraph cg, AbstractD3Algo d3Algo) {
        Map<SootMethod, CallSites> callers = new HashMap<>(1);

        // firstly we get all callers from the incomplete call graph built in soot
        Iterator<Edge> edgeIterator = cg.edgesInto(callee);
        while (edgeIterator.hasNext()) {
            Edge       edge     = edgeIterator.next();
            SootMethod caller   = edge.src();
            Unit       callSite = edge.srcUnit();

            if (null == caller || null == caller.getDeclaringClass() ||
                    d3Algo.is3rdPartyLibMethod(caller)) {
                continue ;
            } else if (callers.containsKey(caller)) {
                callers.get(caller).addCallSite(callSite);
            } else {
                callers.put(caller, new CallSites(callee, caller, callSite));
            }
        }

        // then we traverse all invoking statements, and check
        for (Map.Entry<SootMethod, Set<Unit>> entry : invokingStmtsCache.entrySet()) {
            for (Unit u : entry.getValue()) {
                InvokeExpr invokeExpr = ((Stmt) u).getInvokeExpr();
                if (callee.equals(invokeExpr.getMethod())) {
                    if (callers.containsKey(u)) {
                        callers.get(entry.getKey()).addCallSite(u);
                    } else {
                        callers.put(entry.getKey(), new CallSites(callee, entry.getKey(), u));
                    }
                }
            }
        }

        return callers;
    }

    // findBackwardDataDependencies finds the data-flow dependencies of u located at m in the call graph
    private static Set<Unit> findBackwardDataDependencies(Unit u, SootMethod m, CallGraph cg, AbstractD3Algo d3Algo) {
        Set<Value>      exValues        = new HashSet<>();
        return findBackwardDataDependenciesExcept(null, u, m, cg, exValues, d3Algo);
    }

    // findBackwardDataDependenciesExcept finds the data-flow dependencies of u located at m in the call graph,
    // but exclude the exValues, i.e. do not trace them recursively
    private static Set<Unit>  findBackwardDataDependenciesExcept(
            Set<Unit> prevRet, // prevRet saves the ret value previously, to fix the bug of cross-level-recursion
            Unit u,
            SootMethod m,
            CallGraph cg,
            Set<Value> exValues,
            AbstractD3Algo d3Algo) {
        Set<Unit> ret = null == prevRet ? new HashSet<>() : prevRet;
        SootClass c = null == m ? null : m.getDeclaringClass();

        // skip 3rd-party
        if (null == m || null == c || d3Algo.is3rdPartyLibClass(c)) {
            return new HashSet<>();
        }

        // trick, we add the unit itself and all units in <clinit>(the static code block) and <init> into slicing
        // because these two blocks are sometimes the dependencies of other units
        try {
            ret.add(u);
            for (SootMethod mm : c.getMethods()) {
                if (CLASS_STATIC_CODE_BLOCK_METHOD_NAME.equals(mm.getName()) ||
                        CLASS_CODE_BLOCK_METHOD_NAME.equals(mm.getName())) {
                    ret.addAll(mm.getActiveBody().getUnits());
                }
            }
        } catch (Exception e) { }

        for (ValueBox vb : u.getUseBoxes()) {
            Value v = vb.getValue();

            // ignore non-local values and excepted values
            if (!(v instanceof Local) || exValues.contains(v)) { continue; }

            // find those redefined statements
            Set<Unit> redefinedStmts = findPreviousDefinitions(v, u, m);
            Set<Unit> alreadyInRet   = new HashSet<>();
            // remove those already in ret
            for (Unit uu : redefinedStmts) {
                if (ret.contains(uu)) {
                    alreadyInRet.add(uu);
                }
            }
            redefinedStmts.removeAll(alreadyInRet);
            // add the rest redefined statements
            ret.addAll(redefinedStmts);
            // track these redefined statements
            for (Unit uu : redefinedStmts) {
                // when the redefined statement is redefined by a method arg, we continually track the caller
                if (uu instanceof IdentityStmt && ((IdentityStmt) uu).getRightOp() instanceof ParameterRef) {
                    int argIdx = ((ParameterRef) ((IdentityStmt) uu).getRightOp()).getIndex();
                    Map<SootMethod, CallSites> callSites = doFindCallSites(m, cg, d3Algo);

                    for (Map.Entry<SootMethod, CallSites> entry : callSites.entrySet()) {
                        // skip recursions
                        SootMethod caller = entry.getKey();
                        if (null != caller && caller.equals(m)) { continue; }
                        // not recursions
                        for (Unit callSite : entry.getValue().getCallSites()) {
                            assert callSite instanceof Stmt && ((Stmt) callSite).containsInvokeExpr();
                            // skip already sliced
                            if (ret.contains(callSite)) { continue; }
                            // u is control-depend-on callSite
                            else { ret.add(callSite); }
                            Value      trackedArg    = ((Stmt) callSite).getInvokeExpr().getArg(argIdx);
                            Set<Value> untrackedArgs = new HashSet<>(((Stmt) callSite).getInvokeExpr().getArgs());
                            untrackedArgs.remove(trackedArg);
                            ret.addAll(findBackwardDataDependenciesExcept(ret, callSite, entry.getKey(), cg, untrackedArgs, d3Algo));
                        }
                    }
                } else {
                    ret.addAll(findBackwardDataDependenciesExcept(ret, uu, m, cg, new HashSet<>(), d3Algo));
                }
            }
        }

        return ret;
    }

    // findTrickySlicing finds the slicing that is not easy by a comman way, i.e. by a tricky way
    private static Set<Unit> findTrickySlicing(Unit u, SootMethod m) {
        Set<Unit> ret  = new HashSet<>();
        Body      body = null;

        try { body = m.getActiveBody(); } catch (Exception e) { return ret; }

        List<Unit> units = new ArrayList<>(body.getUnits());
        int        uIdx  = units.indexOf(u);
        for (int i = 0; i < uIdx; i ++) {
            Unit uu = units.get(i);
            if (null == uu || !(uu instanceof IfStmt)) { continue; }
            else { ret.add(uu); }

            IfStmt ifStmt = (IfStmt) uu;

            try {
                Value leftV = ifStmt.getCondition().getUseBoxes().get(0).getValue();
                if (leftV instanceof Local) {
                    ret.add(findLatestDefinition(leftV, uu, m));
                }
            } catch (Exception e) {
                // do nothing
            }

            try {
                Value rightV = ifStmt.getCondition().getUseBoxes().get(1).getValue();
                if (rightV instanceof Local) {
                    ret.add(findLatestDefinition(rightV, uu, m));
                }
            } catch (Exception e) {
                // do nothing
            }
        }

        return ret;
    }

}
