package com.example.ficfinder.models.api;

import com.alibaba.fastjson.annotation.JSONField;

public class ApiField {

    @JSONField(name = "field")
    private String field;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

}
