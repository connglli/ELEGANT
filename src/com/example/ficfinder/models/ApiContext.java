package com.example.ficfinder.models;

import com.alibaba.fastjson.annotation.JSONField;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;

public class ApiContext {

    @JSONField(name = "api")
    Api api;

    @JSONField(name = "context")
    Context context;

    public Api getApi() {
        return api;
    }

    public void setApi(Api api) {
        this.api = api;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }

}
