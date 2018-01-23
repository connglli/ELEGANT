package com.example.ficfinder.finder.plainfinder;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.tracker.Issue;

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

    public static class CallerPoint {
        private String srcFile;

        private int startLineNumber;

        private int startColumnNumber;

        private String method;

        public CallerPoint(String srcFile,
                           int startLineNumber,
                           int startColumnNumber,
                           String method) {
            this.srcFile = srcFile;
            this.startLineNumber = startLineNumber;
            this.startColumnNumber = startColumnNumber;
            this.method = method;
        }

        public String getSrcFile() {
            return srcFile;
        }

        public void setSrcFile(String srcFile) {
            this.srcFile = srcFile;
        }

        public int getStartLineNumber() {
            return startLineNumber;
        }

        public void setStartLineNumber(int startLineNumber) {
            this.startLineNumber = startLineNumber;
        }

        public int getStartColumnNumber() {
            return startColumnNumber;
        }

        public void setStartColumnNumber(int startColumnNumber) {
            this.startColumnNumber = startColumnNumber;
        }

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
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
