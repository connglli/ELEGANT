package com.example.ficfinder.models.context;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class Context implements Serializable {

    @JSONField(name = "min_api_level")
    private int minApiLevel = 1;

    @JSONField(name = "max_api_level")
    private int maxApiLevel = Integer.MAX_VALUE;

    @JSONField(name = "min_system_version")
    private double minSystemVersion = 1;

    @JSONField(name = "max_system_version")
    private double maxSystemVersion = Double.MAX_VALUE;

    @JSONField(name = "bad_devices")
    private String[] badDevices = {};

    public int getMinApiLevel() {
        return minApiLevel;
    }

    public void setMinApiLevel(int minSdkLvel) {
        this.minApiLevel = minSdkLvel;
    }

    public int getMaxApiLevel() {
        return maxApiLevel;
    }

    public void setMaxApiLevel(int maxApiLevel) {
        this.maxApiLevel = maxApiLevel;
    }

    public double getMinSystemVersion() {
        return minSystemVersion;
    }

    public void setMinSystemVersion(double minSystemVersion) {
        this.minSystemVersion = minSystemVersion;
    }

    public double getMaxSystemVersion() {
        return maxSystemVersion;
    }

    public void setMaxSystemVersion(double maxSystemVersion) {
        this.maxSystemVersion = maxSystemVersion;
    }

    public String[] getBadDevices() {
        return badDevices;
    }

    public void setBadDevices(String[] badDevices) {
        this.badDevices = badDevices;
    }

}
