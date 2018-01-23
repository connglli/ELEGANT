package com.example.ficfinder.tracker;

import com.example.ficfinder.models.ApiContext;

public class Issue implements PubSub.Message {

    protected ApiContext model;

    public Issue(ApiContext model) {
        this.model = model;
    }

    public ApiContext getModel() {
        return model;
    }

    public void setModel(ApiContext model) {
        this.model = model;
    }
}
