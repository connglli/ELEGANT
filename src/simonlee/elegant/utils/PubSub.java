package simonlee.elegant.utils;

public interface PubSub {

    interface Message {}

    interface Handle {
        void handle(Message message);
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
     * @param message
     */
    void publish(Message message);

}
