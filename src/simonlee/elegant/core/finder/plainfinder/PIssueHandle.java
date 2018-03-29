package simonlee.elegant.core.finder.plainfinder;

import simonlee.elegant.Container;
import simonlee.elegant.core.finder.AbstractIssueHandle;
import simonlee.elegant.core.finder.Issue;
import simonlee.elegant.utils.CallPoint;

public class PIssueHandle extends AbstractIssueHandle {

    public PIssueHandle(Container container) {
        super(container);
    }

    @Override
    protected CallChain issueToCallChain(Issue i) {
        if (!(i instanceof PIssue)) {
            return null;
        }

        CallChain callChain = new CallChain();
        PIssue pIssue = (PIssue) i;

        pIssue.getCallerPoints().forEach(p ->
                callChain.add(new CallPoint(p.getSrcFile(), p.getStartLineNumber(), p.getStartColumnNumber(), p.getMethod()))
        );

        return callChain;
    }

}
