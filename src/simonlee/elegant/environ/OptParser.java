package simonlee.elegant.environ;

import com.alibaba.fastjson.JSON;
import simonlee.elegant.Dbs;
import simonlee.elegant.d3algo.D3AlgoFactory;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.utils.Bundle;
import simonlee.elegant.utils.PubSub;
import soot.jimple.infoflow.InfoflowConfiguration;
import soot.jimple.infoflow.android.InfoflowAndroidConfiguration;
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

    // option OPT_D3_ALGO and its bundles
    public static final String OPT_D3_ALGO = "d3-algo";
    public static final String OPT_BDL_D3_ALGO_ALGO = "d3-algo.algo";

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

        String d3Algo = (String) this.getOpt(OPT_D3_ALGO);
        bundle = parseD3Algo(d3Algo);
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
        // TODO - Yes, hard code here, don't touch it, should be a parameter passed to ELEGANT
        String sourcesAndSinksFilePath = Dbs.SOURCES_AND_SINKS_FILE;
        String androidCallBacksFilePath = Dbs.ANDROID_CALLBACKS_FILE;

        try {
            if (!apkPath.endsWith(".apk")) {
                throw new RuntimeException("File " + apkPath + " may not be a legal apk file");
            }

            // manifest
            ProcessManifest manifest = new ProcessManifest(apkPath);
            // setup application
            SetupApplication app = new SetupApplication(androidPlatformsPath, apkPath);

            // see more configurations in ``SetupApplication.initializeSoot()''
            // set andoid callbacks, and use
            app.setCallbackFile(androidCallBacksFilePath);
            // set new instance mode
            app.getConfig().setSootIntegrationMode(InfoflowAndroidConfiguration.SootIntegrationMode.CreateNewInstace);
            // set sources and sinks files, which is used to calculate the call graph
            app.getConfig().getAnalysisFileConfig().setSourceSinkFile(sourcesAndSinksFilePath);
            // set call graph construction algorithms
            app.getConfig().setCallgraphAlgorithm(InfoflowConfiguration.CallgraphAlgorithm.SPARK);

            bundle.putExtra(OPT_BDL_APK_PATH_APP, app);
            bundle.putExtra(OPT_BDL_APK_PATH_MANIFEST, manifest);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }

        return bundle;
    }

    // parseModels will parse the option OPT_APK_PATH
    private OptBundle<String> parseD3Algo(String d3Algo) {
        OptBundle<String> bundle = new OptBundle<>(OPT_D3_ALGO, d3Algo);
        String platformsDirPath = (String) getOpt(OPT_PLATFORMS_PATH);
        List<String> args = new ArrayList<>();

        // add apk path
        args.add((String) getOpt(OPT_APK_PATH));

        // add other args
        File platformsDir = new File(platformsDirPath);
        if (!platformsDir.isDirectory()) {
            throw new RuntimeException("Platforms `" + platformsDirPath + "' is not a directory");
        }

        File[] platforms = platformsDir.listFiles();
        if (null == platforms || 0 == platforms.length) {
            throw new RuntimeException("Platforms `" + platformsDirPath + "' is not a valid platforms directory");
        }

        for (File platform : platforms) {
            if (!platformsDir.isDirectory()) {
                throw new RuntimeException("Platforms' subdirectory`" + platform.getParent() + "' is not a directory");
            } else {
                File[] fs = platform.listFiles((dir, name) -> "android.jar".equals(name));
                if (null != fs && fs.length > 0) {
                    args.add(fs[0].getAbsolutePath());
                }
            }
        }

        bundle.putExtra(OPT_BDL_D3_ALGO_ALGO, D3AlgoFactory.getD3Algo(d3Algo, args));

        return bundle;
    }

}
