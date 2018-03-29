package simonlee.elegant.core.tracker;

import simonlee.elegant.core.finder.Issue;
import simonlee.elegant.utils.PubSub;

import java.util.LinkedList;
import java.util.List;

public class Tracker implements PubSub {

    // Handles

    private List<Handle> handles;

    public Tracker() {
        handles = new LinkedList<>();
    }

    @Override
    public int subscribe(Handle handle) {
        if (handles.add(handle)) {
            return handles.size() - 1;
        }

        return -1;
    }

    @Override
    public void unsubscribe(int handler) {
        handles.remove(handler);
    }

    @Override
    public void publish(Message message) {
        if (!(message instanceof Issue)) {
            return ;
        }

        Issue issue = (Issue) message;
        for (Handle handle : handles) {
            handle.handle(issue);
        }
    }

}
