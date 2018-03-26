package com.example.ficfinder.core.finder.plainfinder;

import com.example.ficfinder.Container;
import com.example.ficfinder.core.finder.AbstractIssueHandle;
import com.example.ficfinder.core.finder.Issue;
import com.example.ficfinder.utils.CallPoint;

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
