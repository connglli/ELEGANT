package simonlee.elegant.core.finder.reflectionfinder;

import simonlee.elegant.Container;
import simonlee.elegant.core.finder.AbstractFinder;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.models.api.ApiMethod;
import simonlee.elegant.utils.Logger;
import simonlee.elegant.utils.Soots;
import simonlee.elegant.utils.Strings;
import soot.*;
import soot.jimple.Stmt;
import soot.jimple.toolkits.annotation.nullcheck.NullnessAnalysis;
import soot.jimple.toolkits.callgraph.Edge;
import soot.toolkits.graph.BriefUnitGraph;

import java.util.*;

public class RFinder extends AbstractFinder {

    private Logger logger = new Logger(RFinder.class);

    // REFLECTION_GET_METHOD_SIGNATURE is the soot signature of class.getMethod
    private static String REFLECTION_GET_METHOD_SIGNATURE =
            "<java.lang.Class: java.lang.reflect.Method getMethod(java.lang.String,java.lang.Class[])>";
    // edges stores all edges calling into REFLECTION_GET_METHOD_SIGNATURE
    private Set<Edge> edges;
    // detectedEdges stores detected edges in the detection phase, which will be processed in
    private Set<Edge> detectedEdges;
    // validatedEdges stores all validated edges in validation phase, that will be emitted in generation phase
    private Set<Edge> validatedEdges;

    public RFinder(Container container, Set<ApiContext> models) {
        super(container, models);

        edges          = new HashSet<>();
        detectedEdges  = new HashSet<>();
        validatedEdges = new HashSet<>();

        // find all edges
        Iterator<Edge> it = Scene.v().getCallGraph().edgesInto(Scene.v().getMethod(REFLECTION_GET_METHOD_SIGNATURE));
        while (it.hasNext()) { edges.add(it.next()); }
    }

    @Override
    protected void setUp() {
        this.container.watchIssues(new RIssueHandle(this.container));
    }

    @Override
    protected boolean detect(ApiContext model) {
        // TODO eliminate this condition to support iface and field
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
    protected boolean validate(ApiContext model) {
        // TODO eliminate this condition to support iface and field
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
                // no variable is defined when using this reflection, we simply treat this as a potential bug
                validatedEdges.add(edge);
            } else {
                // non-nullness can be ensured by: 1. if-else checking 2. try-catch block

                Body body = caller.getActiveBody();

                // here we will run a nullness analysis, to guarantee that the r9 is checked non-nullness via if-else
                NullnessAnalysis nullnessAnalysis = new NullnessAnalysis(new BriefUnitGraph(body));

                // we must guarantee that the handler got by reflection is not null
                boolean    fixed = true;
                List<Unit> units = new ArrayList<>(body.getUnits());
                int        index = units.indexOf(callSiteUnit);

                // get all try-catch blocks to get ready for try-catch checking
                List<Trap> traps = new ArrayList<>(body.getTraps());

                // traverse units after call site, guarantee that invoking of r9 is checked non-nullness
                for (int i = index + 1; i < units.size(); i++) {
                    Unit u = units.get(i);

                    // u does not invoke r9, skip it
                    if (!Strings.contains(u.toString(), definedVar.toString() + ".")) { continue; }

                    // ensure that r9 is non-null via if-else
                    if (!nullnessAnalysis.isAlwaysNonNullBefore(u, (Immediate) definedVar)) {
                        // non-nullness can not be guaranteed by if-else,
                        // then, we must do a try-catch checking
                        boolean caught = false;
                        for (Trap trap : traps) {
                            SootClass exceptionClass = trap.getException();
                            String exceptionJavaStyleName = exceptionClass.getJavaStyleName();
                            int bidx = units.indexOf(trap.getBeginUnit());
                            int eidx = units.indexOf(trap.getEndUnit());

                            // this trap can catch u, and the exception to be caught
                            // is a NullPointerException or a NoSuchMethodException
                            // or a its super classes' instance
                            if (bidx <= i && i <= eidx &&
                                    ("NullPointerException".equals(exceptionJavaStyleName) ||
                                     "NoSuchMethodException".equals(exceptionJavaStyleName) ||
                                     "ReflectiveOperationException".equals(exceptionJavaStyleName) ||
                                     "Exception".equals(exceptionJavaStyleName))
                                    ) {
                                caught = true;
                                break;
                            }
                        }

                        if (!caught) {
                            fixed = false;
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
    protected void generate(ApiContext model) {
        // TODO eliminate this condition to support iface and field
        if (!(model.getApi() instanceof ApiMethod)) {
            return ;
        }

        for (Edge edge : validatedEdges) {
            Unit       callSiteUnit = edge.srcUnit();
            SootMethod caller       = edge.src();

            RIssue rIssue = new RIssue(model,
                caller.getDeclaringClass().getName(),
                callSiteUnit.getJavaSourceStartLineNumber(),
                callSiteUnit.getJavaSourceStartColumnNumber(),
                caller.getName());

            this.container.emitIssue(rIssue);
        }
    }

}
