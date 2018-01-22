package com.example.ficfinder.utils;

import com.sun.istack.internal.NotNull;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;
import soot.toolkits.graph.Block;
import soot.toolkits.graph.pdg.IRegion;
import soot.toolkits.graph.pdg.PDGNode;
import soot.toolkits.graph.pdg.ProgramDependenceGraph;

import java.util.*;

public class Soots {

    private static Logger logger = new Logger(Soots.class);

    /**
     * findLatestDefinition will find the latest definition unit of value v at unit u in method m
     *
     * @param v
     * @param u
     * @param m
     * @return
     */
    public static Unit findLatestDefinition(@NotNull Value v, @NotNull Unit u, @NotNull SootMethod m) {
        if (!(u instanceof Stmt)) { return null; }

        Unit       def = null;
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
     * findNodeOf finds node of unit u in pdg
     *
     * @param pdg
     * @param u
     * @return
     */
    public static PDGNode findNodeOf(ProgramDependenceGraph pdg, Unit u) {
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
     * findDependentOf finds dependent units of unit unit
     *
     */
    /**
     * findDependentOf finds dependent units of unit u in pdg
     *
     * @param pdg
     * @param u
     * @return
     */
    public static Set<Unit> findDependentOf(ProgramDependenceGraph pdg, Unit u) {
        Set<Unit> deps = new HashSet<>(128);

        // 1. find the corresponding PDGNode srcNode, which contains the unit
        PDGNode srcNode = findNodeOf(pdg, u);
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
            logger.w("Only REGION and CFGNODE are allowed");
        }

        return iterator;
    }

}
