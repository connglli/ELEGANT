package simonlee.elegant.reporter;

import simonlee.elegant.finder.Issue;
import simonlee.elegant.utils.PubSub;

public abstract class AbstractIssueHandle implements PubSub.Handle {

    // CallChain is just a intermediate representation of Information
    public static class CallChain extends Reporter.Information { }

    private Reporter reporter;

    public AbstractIssueHandle(Reporter reporter) {
        this.reporter = reporter;
    }

    @Override
    public void handle(PubSub.Message message) {
        if (!(message instanceof Issue)) {
            return ;
        }

        CallChain callChain = issueToCallChain((Issue) message);
        if (null != callChain) {
            reporter.submit(((Issue) message).getModel(), callChain);
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
