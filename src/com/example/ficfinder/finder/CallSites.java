package com.example.ficfinder.finder;

import soot.SootMethod;
import soot.Unit;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class CallSite {

    private SootMethod caller;

    private SootMethod callee;

    private Set<Unit> callSites = new HashSet<>();

    public CallSite() { }

    public CallSite(SootMethod callee) {
        this();
        this.callee = callee;
    }

    public CallSite(SootMethod callee, SootMethod caller) {
        this(callee);
        this.caller = caller;
    }

    public CallSite(SootMethod callee, SootMethod caller, Unit ...callSites) {
        this(callee, caller);
        this.callSites.addAll(Arrays.asList(callSites));
    }

    public SootMethod getCaller() {
        return caller;
    }

    public void setCaller(SootMethod caller) {
        this.caller = caller;
    }

    public SootMethod getCallee() {
        return callee;
    }

    public void setCallee(SootMethod callee) {
        this.callee = callee;
    }

    public Set<Unit> getCallSites() {
        return callSites;
    }

    public void setCallSites(Set<Unit> callSites) {
        this.callSites = callSites;
    }

    public void addCallSite(Unit u) {
        this.callSites.add(u);
    }

}
