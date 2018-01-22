package com.example.ficfinder.finder;

import com.example.ficfinder.Env;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.ApiMethod;
import com.example.ficfinder.tracker.Issue;
import com.example.ficfinder.utils.Logger;
import com.example.ficfinder.utils.Soots;
import com.example.ficfinder.utils.Strings;
import soot.*;
import soot.jimple.IfStmt;
import soot.jimple.NullConstant;
import soot.jimple.Stmt;
import soot.jimple.toolkits.callgraph.Edge;

import java.util.*;

public class ReflectionFinder extends AbstractFinder {

    private Logger logger = new Logger(ReflectionFinder.class);

    // REFLECTION_GET_METHOD_SIGNATURE is the soot signature of class.getMethod
    private static String REFLECTION_GET_METHOD_SIGNATURE =
            "<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>";

    // edges stores all edges calling into REFLECTION_GET_METHOD_SIGNATURE
    private Set<Edge> edges;

    // detectedEdges stores detected edges in the detection phase, which will be processed in
    private Set<Edge> detectedEdges;

    // validatedEdges stores all validated edges in validation phase, that will be emitted in generation phase
    private Set<Edge> validatedEdges;

    public ReflectionFinder(Set<ApiContext> models) {
        super(models);

        edges          = new HashSet<>();
        detectedEdges  = new HashSet<>();
        validatedEdges = new HashSet<>();

        // find all edges
        Iterator<Edge> it = Scene.v().getCallGraph().edgesInto(Scene.v().getMethod(REFLECTION_GET_METHOD_SIGNATURE));
        while (it.hasNext()) { edges.add(it.next()); }
    }

    @Override
    public boolean detect(ApiContext model) {
        if (!(model.getApi() instanceof ApiMethod)) {
            return false;
        }

        ApiMethod callee = (ApiMethod) model.getApi();

        // clear all pre-detected, and to detect new edges
        detectedEdges.clear();
        for (Edge edge : edges) {
            Unit       callSiteUnit = edge.srcUnit();
            SootMethod caller       = edge.src();

            if (Strings.contains(callSiteUnit.toString(), "\"" + callee.getMethod() + "\"")) {
                // this reflection gets this method directly, e.g.
                // r9 = virtualinvoke $r2.<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>("getActionBar", $r1);
                detectedEdges.add(edge);
            } else {
                // this reflection gets this method directly, e.g.
                // $r1 = "getActionBar"
                // ...
                // r9 = virtualinvoke $r2.<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>($r1, $r2);
                if (callSiteUnit instanceof Stmt && ((Stmt) callSiteUnit).containsInvokeExpr()) {
                    List<Value> args  = ((Stmt) callSiteUnit).getInvokeExpr().getArgs();

                    for (Value arg : args) {
                        if (!(arg instanceof Local)) { continue; }

                        // find the definition of this arg
                        Unit u = Soots.findLatestDefinition(arg, callSiteUnit, caller);
                        if (null != u && Strings.contains(u.toString(), "\"" + callee.getMethod() + "\"")) {
                            detectedEdges.add(edge);
                            break;
                        }
                    }
                }
            }
        }

        // remove all edges that is detected
        this.edges.removeAll(detectedEdges);

        return 0 != detectedEdges.size();
    }

    @Override
    public boolean validate(ApiContext model) {
        if (!(model.getApi() instanceof ApiMethod)) {
            return false;
        }

        validatedEdges.clear();
        for (Edge edge : detectedEdges) {
            Value      definedVar   = null;
            Unit       callSiteUnit = edge.srcUnit();
            SootMethod caller       = edge.src();

            List<ValueBox> defBoxes = callSiteUnit.getDefBoxes();
            if (null != defBoxes && null != defBoxes.get(0)) {
                definedVar = defBoxes.get(0).getValue();
            }

            if (null == definedVar) {
                validatedEdges.add(edge);
            } else {
                // we must guarantee that the handler got by reflection is not null
                boolean    fixed = false;
                List<Unit> units = new ArrayList<>(caller.getActiveBody().getUnits());
                int        index = units.indexOf(callSiteUnit);

                // traverse units after call site, guarantee that r9 is checked unnullness
                for (int i = index + 1; i < units.size(); i++) {
                    Unit u = units.get(i);
                    if (u instanceof IfStmt) {
                        List<ValueBox> useBoxes = ((IfStmt) u).getCondition().getUseBoxes();
                        Value leftV = useBoxes.get(0).getValue();
                        Value rightV = useBoxes.get(1).getValue();

                        if ((leftV instanceof NullConstant && rightV.equals(definedVar)) ||
                                (rightV instanceof NullConstant && leftV.equals(definedVar))) {
                            fixed = true;
                            break;
                        }
                    }
                }

                if (!fixed) {
                    validatedEdges.add(edge);
                }
            }
        }

        return 0 != validatedEdges.size();
    }

    @Override
    public void generate(ApiContext model) {
        if (!(model.getApi() instanceof ApiMethod)) {
            return ;
        }

        for (Edge edge : validatedEdges) {
            Unit       callSiteUnit = edge.srcUnit();
            SootMethod caller       = edge.src();
            ApiMethod  callee       = (ApiMethod) model.getApi();

            Issue issue = new Issue(model);
            issue.setCalleePoint(new Issue.CalleePoint(callee.getSignature()));
            issue.setCallerPoints(Arrays.asList(new Issue.CallerPoint(
                    "~",
                    callSiteUnit.getJavaSourceStartLineNumber(),
                    callSiteUnit.getJavaSourceStartColumnNumber(),
                    caller.getSignature())));

            Env.v().emit(issue);
        }
    }

}
