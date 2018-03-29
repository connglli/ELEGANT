package simonlee.elegant.core.finder.reflectionfinder;

import simonlee.elegant.models.ApiContext;
import simonlee.elegant.core.finder.Issue;


public class RIssue extends Issue {

    private String srcFile;
    private int startLineNumber;
    private int startColumnNumber;
    private String method;

    public RIssue(ApiContext model,
                  String srcFile,
                  int startLineNumber,
                  int startColumnNumber,
                  String method) {
        super(model);
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
