package com.example.ficfinder.tracker;

public interface PubSub {

    interface Issue {}

    interface Handle {
        void handle(Issue issue);
    }

    /**
     * Subscribe to watch issues
     *
     * @param handle
     * @return
     */
    int subscribe(Handle handle);

    /**
     * Unsubscribe to watch issues
     *
     * @param handler
     */
    void unsubscribe(int handler);

    /**
     * Publish an Issue
     *
     * @param issue
     */
    void publish(Issue issue);

}
