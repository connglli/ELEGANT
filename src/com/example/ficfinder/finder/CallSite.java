package com.example.ficfinder.finder;

import soot.SootMethod;
import soot.Unit;

public class CallSite {

    private SootMethod method;

    private Unit unit;

    public CallSite(SootMethod m, Unit u) {
        this.method = m;
        this.unit = u;
    }

    public SootMethod getMethod() {
        return method;
    }

    public void setMethod(SootMethod method) {
        this.method = method;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit unit) {
        this.unit = unit;
    }

}
