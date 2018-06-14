package simonlee.elecli.reporter;

import simonlee.elegant.finder.Issue;
import simonlee.elegant.finder.reflectionfinder.RIssue;
import simonlee.elegant.utils.CallPoint;

public class RIssueHandle extends AbstractIssueHandle {

    public RIssueHandle(Reporter reporter) {
        super(reporter);
    }

    @Override
    protected CallChain issueToCallChain(Issue i) {
        if (!(i instanceof RIssue)) {
            return null;
        }

        CallChain callChain = new CallChain();
        RIssue rIssue = (RIssue) i;

        callChain.add(new CallPoint(rIssue.getSrcFile(), rIssue.getStartLineNumber(), rIssue.getStartColumnNumber(), rIssue.getMethod()));

        return callChain;
    }

}
