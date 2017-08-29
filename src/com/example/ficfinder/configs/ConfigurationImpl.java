package com.example.ficfinder.configs;

import com.example.ficfinder.models.ApiContext;

import java.util.List;

public class ConfigurationImpl implements Configuration {

    @Override
    public List<ApiContext> read(String configFilePath) {
        return null;
    }

    @Override
    public void write(List<ApiContext> acPairs, String configFilePath) {

    }

}
