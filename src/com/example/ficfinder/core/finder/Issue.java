package com.example.ficfinder.core.finder;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.utils.PubSub;

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
