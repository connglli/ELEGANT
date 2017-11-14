package com.example.ficfinder.tracker;

import com.example.ficfinder.finder.Callsite;
import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;

public class Issue implements PubSub.Issue {

    private String srcFile;

    private int startLineNumber;

    private int startColumnNumber;

    private String method;

    private ApiContext issueModel;

    public static Issue create(Callsite callsite, ApiContext issueModel) {
//        return new Issue(callsite.getMethod().getTag("SourceFileTag").getName(),
//                callsite.getUnit().getJavaSourceStartLineNumber(),
//                callsite.getUnit().getJavaSourceStartColumnNumber(),
//                callsite.getMethod().getSignature(),
//                issueModel);
        return new Issue("~",
                callsite.getUnit().getJavaSourceStartLineNumber(),
                callsite.getUnit().getJavaSourceStartColumnNumber(),
                callsite.getMethod().getSignature(),
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

    @Override
    public String toString() {
        Api api = issueModel.getApi();
        Context context = issueModel.getContext();

        String badDevicesInfo = "";

        if (issueModel.hasBadDevices()) {
            badDevicesInfo = "except devices: ";
            String[] badDevices = context.getBadDevices();
            for (int i = 0, l = badDevices.length; i < l - 1; i ++) {
                badDevicesInfo += badDevices[i] + ", ";
            }
            badDevicesInfo += badDevices[badDevices.length - 1];
        }

        return "API " + api.getSiganiture() + " should be used within the context: \n" +
                "\t android API level: [" + context.getMinApiLevel() + ", " + (context.getMaxApiLevel() == Context.DEFAULT_MAX_API_LEVEL ? "~" : context.getMaxApiLevel()) + "]\n" +
                "\t android system version: [" + context.getMaxSystemVersion() + ", " + (context.getMaxSystemVersion() == context.DEFAULT_MAX_SYSTEM_VERSITON ? "~" : context.getMaxSystemVersion()) + "]\n" +
                "\t " + badDevicesInfo + "\n";
    }
}
