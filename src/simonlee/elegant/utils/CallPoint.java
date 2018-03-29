package simonlee.elegant.utils;

public class CallPoint {

    private String srcFile;
    private int startLineNumber;
    private int startColumnNumber;
    private String method;

    public CallPoint(String srcFile,
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
