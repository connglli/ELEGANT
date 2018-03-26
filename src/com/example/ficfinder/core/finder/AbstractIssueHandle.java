package com.example.ficfinder.core.finder;

import com.example.ficfinder.Container;
import com.example.ficfinder.core.reporter.Reporter;
import com.example.ficfinder.utils.PubSub;

public abstract class AbstractIssueHandle implements PubSub.Handle {

    // CallChain is just a intermediate representation of Information
    public static class CallChain extends Reporter.Information { }

    private Container container;

    public AbstractIssueHandle(Container container) {
        this.container = container;
    }

    @Override
    public void handle(PubSub.Message message) {
        if (!(message instanceof Issue)) {
            return ;
        }

        CallChain callChain = issueToCallChain((Issue) message);
        if (null != callChain) {
            container.submit(((Issue) message).getModel(), callChain);
        }
    }

    /**
     * issueToCallChain converts an issue to a CallChain
     *
     * @param i issue to be converted
     * @return  call chain after converting
     */
    protected abstract CallChain issueToCallChain(Issue i);

}
