package com.example.ficfinder.models;

import com.alibaba.fastjson.annotation.JSONField;
import com.example.ficfinder.models.api.Api;
import com.example.ficfinder.models.context.Context;

import java.util.Arrays;

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

    public boolean matchApiLevel(int targetApiLevel, int minApiLevel) {
        return minApiLevel >= context.getMinApiLevel() && targetApiLevel <= context.getMaxApiLevel();
    }

    public boolean matchSystemVersion(double targetVersion) {
        return targetVersion >= context.getMinSystemVersion() && targetVersion <= context.getMaxSystemVersion();
    }

    public boolean hasBadDevices() {
        return !Arrays.<String>asList(this.context.getBadDevices()).isEmpty();
    }

    public boolean needCheckApiLevel() {
        return context.getMaxApiLevel() != Context.DEFAULT_MAX_API_LEVEL
                || context.getMinApiLevel() != Context.DEFAULT_MIN_API_LEVEL;
    }

    public boolean needCheckSystemVersion() {
        return context.getMaxSystemVersion() != Context.DEFAULT_MAX_SYSTEM_VERSITON
                || context.getMinSystemVersion() != Context.DEFAULT_MIN_SYSTEM_VERSION;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ApiContext)) {
            return false;
        }

        ApiContext apiContext = (ApiContext) obj;

        return api.equals(apiContext.api) && context.equals(apiContext.context);
    }

    @Override
    public int hashCode() {
        // we don't have to override hashCode because our equals method uses every field of a context obj
        return super.hashCode();
    }

}
