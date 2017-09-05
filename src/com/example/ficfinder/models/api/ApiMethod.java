package com.example.ficfinder.models.api;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.annotation.JSONType;

import java.util.List;

@JSONType(typeName = "method")
public class ApiMethod extends Api {

    public static final String TAG = "com.example.ficfinder.models.api.ApiMethod";

    @JSONField(name = "iface")
    private String iface;

    @JSONField(name = "method")
    private String method;

    @JSONField(name = "ret")
    private Type ret;

    @JSONField(name = "paramList")
    private List<Type> paramList;

    public String getIface() {
        return iface;
    }

    public void setIface(String iface) {
        this.iface = iface;
    }

    public Type getRet() {
        return ret;
    }

    public void setRet(Type ret) {
        this.ret = ret;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public List<Type> getParamList() {
        return paramList;
    }

    public void setParamList(List<Type> paramList) {
        this.paramList = paramList;
    }

    @Override
    public String getSiganiture() {
        // TODO maynot be compatible with soot
        StringBuilder params = new StringBuilder();

        if (this.paramList != null && !this.paramList.isEmpty()) {
            int l = this.paramList.size();

            for (int i = 0; i < l - 1; i ++) {
                params.append(this.paramList.get(i) + ",");
            }
            params.append(this.paramList.get(l - 1));
        }

        return "<" + this.pkg + "." + this.iface + ": " + this.ret + " " + this.method + "(" + params + ")>";
    }

}
