package com.example.ficfinder.models.api;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class Api implements Serializable {

    public static String TYPE_METHOD = "METHOD";

    public static String TYPE_FIELD = "FIELD";

    @JSONField(name = "type")
    private String type;

    @JSONField(name = "pkg")
    private String pkg;

    @JSONField(name = "interf")
    private String interf;

    public static String getTypeMethod() {
        return TYPE_METHOD;
    }

    public static void setTypeMethod(String typeMethod) {
        TYPE_METHOD = typeMethod;
    }

    public static String getTypeField() {
        return TYPE_FIELD;
    }

    public static void setTypeField(String typeField) {
        TYPE_FIELD = typeField;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPkg() {
        return pkg;
    }

    public void setPkg(String pkg) {
        this.pkg = pkg;
    }

    public String getInterf() {
        return interf;
    }

    public void setInterf(String interf) {
        this.interf = interf;
    }

}