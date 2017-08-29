package com.example.ficfinder.configs;

import com.example.ficfinder.models.ApiContext;

import java.util.List;

public interface Configuration {

    List<ApiContext> read(String configFilePath);

    void write(List<ApiContext> acPairs, String configFilePath);

}
