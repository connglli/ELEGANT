package com.example.ficfinder.core.finder.reflectionfinder;

import com.example.ficfinder.Container;
import com.example.ficfinder.core.finder.AbstractIssueHandle;
import com.example.ficfinder.core.finder.Issue;
import com.example.ficfinder.utils.CallPoint;
import com.example.ficfinder.utils.Logger;

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
