package com.example.ficfinder;

import com.alibaba.fastjson.JSON;
import com.example.ficfinder.models.ApiContext;
import com.sun.istack.internal.NotNull;
import soot.G;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Configs {

    // Configurations

    public static final String MODELS = "models";

    public static final String APK = "apk";

    // Singleton

    private static Configs instance;

    // args

    private Map<String, String> args;

    public static Configs v() {
        if (instance == null) {
            instance = new Configs();
        }

        return instance;
    }

    public Map<String, String> getArgs() {
        return args;
    }

    public String getArg(@NotNull String key) {
        return args.get(key);
    }

    public void parse(@NotNull List<String> configs) {
        for (String config : configs) {
            // we will pass unformatted configs
            if (!config.startsWith("--")) {
                continue;
            }

            // every legal config is formatted as --<key>=<value>
            String[] tokens = config.split("--|=");

            // we will pass unformatted configs
            if (tokens.length != 3) {
                continue ;
            }

            String key = tokens[1];
            String value = tokens[2];

            switch (key) {
                case MODELS: parseModels(value); args.put(key, value); break;
                case APK: parseApk(value); args.put(key, value); break;
                default: /* we will pass unknown configs */ break;
            }
        }
    }

    private void parseModels(@NotNull String filePath) {
        File f = new File(filePath);

        if (f == null || !f.exists()) {
            throw new RuntimeException("File " + filePath + " does not exist");
        }

        try {
            StringBuilder buffer = new StringBuilder(1024);
            BufferedReader br = new BufferedReader(new FileReader(f));
            String temp;

            while ((temp = br.readLine()) != null) {
                buffer.append(temp.trim());
            }

            List<ApiContext> models = JSON.parseArray(buffer.toString(), ApiContext.class);

            for (ApiContext model : models) {
                System.out.println(model.getApi().getSiganiture());
            }

            Env.v().setModels(models);
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error generated while reading " + filePath);
        }
    }

    private void parseApk(@NotNull String apkPath) {
        try {
            if (!apkPath.endsWith(".apk")) {
                throw new RuntimeException("File " + apkPath + " may not be an legal apk file");
            }

            ProcessManifest manifest = new ProcessManifest(apkPath);

            // set environment
            Env.v().setManifest(manifest);

            // setup application
            SetupApplication app = new SetupApplication(Env.ANDROID_PLATFORMS_PATH, apkPath);
            try {
                app.calculateSourcesSinksEntrypoints(Env.SOURCES_AND_SINKS_TEXT_PATH);
            } catch (Exception e) {
                throw new RuntimeException("Sources and sinks file " + Env.SOURCES_AND_SINKS_TEXT_PATH + " is not available");
            }
            G.reset();

            // set environment
            Env.v().setApp(app);
        } catch (Exception e) {
            throw new RuntimeException("File " + apkPath + " does not exist");
        }
    }

    private Configs() {
        args = new HashMap<>();
    }

}
