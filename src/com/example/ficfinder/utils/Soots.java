package com.example.ficfinder.utils;

import com.sun.istack.internal.NotNull;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.AssignStmt;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;

import java.util.ArrayList;
import java.util.List;

public class Soots {

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

}
