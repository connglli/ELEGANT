package simonlee.elegantcli.reporter;

import simonlee.elegant.finder.Issue;
import simonlee.elegant.finder.plainfinder.PIssue;
import simonlee.elegant.utils.CallPoint;

public class PIssueHandle extends AbstractIssueHandle {

    public PIssueHandle(Reporter reporter) {
        super(reporter);
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
