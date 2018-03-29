package simonlee.elegant.core.finder.plainfinder;

import simonlee.elegant.models.ApiContext;
import simonlee.elegant.core.finder.Issue;
import simonlee.elegant.utils.CallPoint;

import java.util.LinkedList;
import java.util.List;

public class PIssue extends Issue implements Cloneable {

    public static class CalleePoint {

        private String method;

        public CalleePoint(String method) {
            this.method = method;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

    }

    // a delegate of CallPoint
    public static class CallerPoint extends CallPoint {

        public CallerPoint(String srcFile,
                           int startLineNumber,
                           int startColumnNumber,
                           String method) {
            super(srcFile, startLineNumber, startColumnNumber, method);
        }

    }

    private CalleePoint calleePoint;
    private List<CallerPoint> callerPoints = new LinkedList<>();

    public PIssue(ApiContext model) {
        super(model);
    }

    public List<CallerPoint> getCallerPoints() {
        return callerPoints;
    }

    public void setCallerPoints(List<CallerPoint> callerPoints) {
        this.callerPoints = callerPoints;
    }

    public void addCallPoint(CallerPoint s) {
        this.callerPoints.add(s);
    }

    public CalleePoint getCalleePoint() {
        return calleePoint;
    }

    public void setCalleePoint(CalleePoint calleePoint) {
        this.calleePoint = calleePoint;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        PIssue newPIssue = (PIssue) super.clone();
        newPIssue.callerPoints = (List<CallerPoint>) ((LinkedList)this.callerPoints).clone();
        return newPIssue;
    }

}
