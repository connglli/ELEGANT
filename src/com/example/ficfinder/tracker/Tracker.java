package com.example.ficfinder.tracker;

import java.util.LinkedList;
import java.util.List;

public class Tracker implements PubSub {

    // Singleton

    private static Tracker instance;

    // Handles

    private List<Handle> handles;

    public static Tracker v() {
        if (instance == null) {
            instance = new Tracker();
        }

        return instance;
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

    private Tracker() {
        handles = new LinkedList<>();
    }
}
