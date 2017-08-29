package com.example.ficfinder.models.api;

import com.alibaba.fastjson.annotation.JSONField;

import java.util.List;

public class ApiMethod extends Api {

    public static class Param {

        @JSONField(name = "pkg")
        public String pkg;

        @JSONField(name = "interf")
        public String interf;

    }

    @JSONField(name = "method")
    private String method;

    @JSONField(name = "paramList")
    private List<Param> paramList;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Param> getParamList() {
        return paramList;
    }

    public void setParamList(List<Param> paramList) {
        this.paramList = paramList;
    }

}
