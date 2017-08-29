package com.example.ficfinder.models.context;

import com.alibaba.fastjson.annotation.JSONField;

import java.io.Serializable;

public class Context implements Serializable {

    @JSONField(name = "min_sdk_level")
    private int minSdkLevel;

    @JSONField(name = "max_sdk_level")
    private int maxSdkLevel;

    @JSONField(name = "min_android_level")
    private int minAndroidVersion;

    @JSONField(name = "max_android_level")
    private int maxAndroidVersion;

    @JSONField(name = "devices")
    private String[] devices;

    public int getMinSdkLevel() {
        return minSdkLevel;
    }

    public void setMinSdkLevel(int minSdkLvel) {
        this.minSdkLevel = minSdkLvel;
    }

    public int getMaxSdkLevel() {
        return maxSdkLevel;
    }

    public void setMaxSdkLevel(int maxSdkLevel) {
        this.maxSdkLevel = maxSdkLevel;
    }

    public int getMinAndroidVersion() {
        return minAndroidVersion;
    }

    public void setMinAndroidVersion(int minAndroidVersion) {
        this.minAndroidVersion = minAndroidVersion;
    }

    public int getMaxAndroidVersion() {
        return maxAndroidVersion;
    }

    public void setMaxAndroidVersion(int maxAndroidVersion) {
        this.maxAndroidVersion = maxAndroidVersion;
    }

    public String[] getDevices() {
        return devices;
    }

    public void setDevices(String[] devices) {
        this.devices = devices;
    }

}
