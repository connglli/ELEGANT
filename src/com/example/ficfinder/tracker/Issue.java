package com.example.ficfinder.tracker;

import com.example.ficfinder.finder.CallSite;
import com.example.ficfinder.models.ApiContext;

public class Issue implements PubSub.Issue {

    private String srcFile;

    private int startLineNumber;

    private int startColumnNumber;

    private String method;

    private ApiContext issueModel;

    public static Issue create(CallSite callSite, ApiContext issueModel) {
//        return new Issue(callsite.getMethod().getTag("SourceFileTag").getName(),
//                callsite.getUnit().getJavaSourceStartLineNumber(),
//                callsite.getUnit().getJavaSourceStartColumnNumber(),
//                callsite.getMethod().getSignature(),
//                issueModel);
        return new Issue("~",
                callSite.getUnit().getJavaSourceStartLineNumber(),
                callSite.getUnit().getJavaSourceStartColumnNumber(),
                callSite.getMethod().getSignature(),
                issueModel);
    }

    public Issue(String srcFile,
                 int startLineNumber,
                 int startColumnNumber,
                 String method,
                 ApiContext issueModel) {
        this.srcFile = srcFile;
        this.startLineNumber = startLineNumber;
        this.startColumnNumber = startColumnNumber;
        this.method = method;
        this.issueModel = issueModel;
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

    public ApiContext getIssueModel() {
        return issueModel;
    }

    public void setIssueModel(ApiContext issueModel) {
        this.issueModel = issueModel;
    }

}
