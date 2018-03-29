package simonlee.elegant.core.finder.reflectionfinder;

import simonlee.elegant.Container;
import simonlee.elegant.core.finder.AbstractIssueHandle;
import simonlee.elegant.core.finder.Issue;
import simonlee.elegant.utils.CallPoint;
import simonlee.elegant.utils.Logger;

public class RIssueHandle extends AbstractIssueHandle {

    private static final Logger logger = new Logger(RIssueHandle.class);

    public RIssueHandle(Container container) {
        super(container);
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
