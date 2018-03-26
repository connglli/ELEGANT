package com.example.ficfinder.core.configurations;

import com.example.ficfinder.utils.PubSub;
import com.sun.istack.internal.NotNull;

import java.util.*;

public class Parser implements PubSub {

    public static class Argument
            extends HashMap.SimpleEntry<String, String>
            implements PubSub.Message {

        public Argument(String key, String value) { super(key, value); }

    }

    public static final String INSTR_DONE = "instruction_done"; // INSTR_DONE means configurations are all parsed

    private Map<String, String> args;
    private List<Handle> handles;

    public Parser() {
        args = new HashMap<>();
        handles = new ArrayList<>();
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public String getArg(@NotNull String key) {
        return args.get(key);
    }

    public void parse(@NotNull List<String> configs) {
        for (String config : configs) {
            // we will omit unformatted configs
            if (!config.startsWith("--")) {
                continue;
            }

            // every legal config is formatted as --<key>=<value>
            String[] tokens = config.split("--|=");

            // we will omit unformatted configs
            if (tokens.length != 3) {
                continue ;
            }

            String key = tokens[1];
            String value = tokens[2];

            // publish it
            publish(new Argument(key, value));
            // save it for future use
            args.put(key, value);
        }

        publish(new Argument(INSTR_DONE, ""));
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
        if (!(message instanceof Argument)) {
            return ;
        }

        Argument argument = (Argument) message;
        for (Handle handle : handles) {
            handle.handle(argument);
        }
    }

}
