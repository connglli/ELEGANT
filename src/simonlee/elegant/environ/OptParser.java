package simonlee.elegant.environ;

import com.alibaba.fastjson.JSON;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.utils.Bundle;
import simonlee.elegant.utils.PubSub;
import soot.G;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.*;
import java.util.*;

public class OptParser implements PubSub {

    // OptBundle is a Bundle keyed String
    public static class OptBundle<V>
            extends Bundle<String, V>
            implements PubSub.Message {
        public OptBundle(String k, V v) { super(k, v); }
    }

    // option OPT_APK_PATH and its bundles
    public static final String OPT_APK_PATH = "apk-path";
    public static final String OPT_BDL_APK_PATH_APP = "apk-path.app";
    public static final String OPT_BDL_APK_PATH_MANIFEST = "apk-path.manifest";

    // option OPT_MODELS_PATH and its bundles
    public static final String OPT_MODELS_PATH = "models-path";
    public static final String OPT_BDL_MODELS_MODEL = "models-path.model";

    // option OPT_PLATFORMS_PATH and its bundles
    public static final String OPT_PLATFORMS_PATH = "platforms";

    private Map<String, Object> opts;
    private List<Handle> handles;

    public OptParser() {
        opts = new HashMap<>();
        handles = new ArrayList<>();
    }

    public Map<String, Object> getOpts() {
        return opts;
    }

    public Object getOpt(String key) {
        return opts.get(key);
    }

    public void putOpt(String key, Object value) { opts.put(key, value); }

    public void parse() {
        OptBundle bundle;

        // some opts need parsing
        String modelsPath = (String) this.getOpt(OPT_MODELS_PATH);
        bundle = parseModels(modelsPath);
        publish(bundle);

        String apkPath = (String) this.getOpt(OPT_APK_PATH);
        bundle = parseApk(apkPath);
        publish(bundle);

        // some opts don't need parsing, publish them directly
        publish(new OptBundle<>(OPT_PLATFORMS_PATH, (String) getOpt(OPT_PLATFORMS_PATH)));
    }

    @Override
    public int subscribe(Handle handle) {
        if (handles.add(handle)) {
            return handles.size() - 1;
        }

        return -1;
    }

    @Override
    public void unsubscribe(int handler) {
        handles.remove(handler);
    }

    @Override
    public void publish(Message message) {
        if (!(message instanceof OptBundle)) {
            return ;
        }

        OptBundle bundle = (OptBundle) message;
        for (Handle handle : handles) {
            handle.handle(bundle);
        }
    }

    // parseModels will parse the argument OPT_MODELS_PATH
    private OptBundle<String> parseModels(String modelsPath) {
        OptBundle<String> bundle = new OptBundle<>(OPT_MODELS_PATH, modelsPath);
        File f = new File(modelsPath);

        if (!f.exists()) {
            throw new RuntimeException("File " + modelsPath + " does not exist");
        }

        try {
            StringBuilder buffer = new StringBuilder(1024);
            BufferedReader br = new BufferedReader(new FileReader(f));
            String temp;

            while ((temp = br.readLine()) != null) {
                buffer.append(temp.trim());
            }

            // set models
            bundle.putExtra(OPT_BDL_MODELS_MODEL, new HashSet<>(JSON.parseArray(buffer.toString(), ApiContext.class)));
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error generated while reading " + modelsPath);
        }

        return bundle;
    }

    // parseModels will parse the option OPT_APK_PATH
    private OptBundle<String> parseApk(String apkPath) {
        OptBundle<String> bundle = new OptBundle<>(OPT_APK_PATH, apkPath);
        String androidPlatformsPath = (String) getOpt(OPT_PLATFORMS_PATH);
        // TODO - Yes, hard code here, don't touch it
        String sourcesAndSinksFilePath = "SourcesAndSinks.txt";
        String androidCallBacksFilePath = "AndroidCallbacks.txt";

        try {
            if (!apkPath.endsWith(".apk")) {
                throw new RuntimeException("File " + apkPath + " may not be a legal apk file");
            }

            // manifest
            ProcessManifest manifest = new ProcessManifest(apkPath);
            // setup application
            SetupApplication app = new SetupApplication(androidPlatformsPath, apkPath);

            // set andoid callbacks, and use sources and sinks to calculate entry point
            app.setCallbackFile(androidCallBacksFilePath);
            try {
                app.calculateSourcesSinksEntrypoints(sourcesAndSinksFilePath);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }

            G.reset();

            bundle.putExtra(OPT_BDL_APK_PATH_APP, app);
            bundle.putExtra(OPT_BDL_APK_PATH_MANIFEST, manifest);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return bundle;
    }

}
