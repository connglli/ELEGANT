package simonlee.elegant.core.environment;


import com.alibaba.fastjson.JSON;
import simonlee.elegant.core.configurations.Parser;
import simonlee.elegant.Container;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.utils.Logger;
import simonlee.elegant.utils.PubSub;
import com.sun.istack.internal.NotNull;
import soot.G;
import soot.Scene;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.infoflow.solver.cfg.InfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.io.*;
import java.util.*;

public class Environment implements PubSub.Handle {

    private static Logger logger = new Logger(Environment.class);

    // Arguments

    public static final String ARG_MODELS            = "models";
    public static final String ARG_APK               = "apk";
    public static final String ARG_OUTPUT            = "output";
    public static final String ARG_VERBOSE           = "verbose";
    public static final String ARG_PLATFORMS         = "platforms";
    public static final String ARG_SOURCES_AND_SINKS = "sourcesAndSinks";

    // Environment variable of ELEGANT

    // k neighbors, used in backward slicing
    public static final int ENV_K_NEIGHBORS = 10;
    // k-indirect-caller, used in call site computing
    // 0-indirect-caller is its direct caller
    public static final int ENV_K_INDIRECT_CALLER = 5;

    // Environments

    // container is the container that Env is in
    private Container container;

    // some converted arguments
    private Set<ApiContext> models;
    private PrintStream     output = System.out;
    private String          androidPlatformsPath = "assets/android-platforms";
    private String          sourcesAndSinksTextPath = "assets/SourcesAndSinks.txt";
    private boolean         verbose = false;

    // default soot settings
    private String[] options = {
            // general options
            "-whole-program",

            // input options
            "-process-dir", "-", // TODO it should be a parameter passed to ELEGANT
            "-src-prec", "apk",
            "-android-jars", androidPlatformsPath,
            "-prepend-classpath",
            "-allow-phantom-refs",
            "-no-bodies-for-excluded",
            "-keep-line-number",

            // output options
            "-output-format", "none",

            // process options
            "-phase-option", "cg.spark", "enabled:true"
    };

    // soot-analysed results
    private SetupApplication app;
    private ProcessManifest  manifest;
    private IInfoflowCFG     interproceduralCFG;

    public Environment(Container container) {
        this.container = container;

        // do some registration work, Env need to get arguments throwed by configurations
        this.container.watchArgs(this);
    }

    public String[] getOptions() {
        return options;
    }

    public SetupApplication getApp() {
        return app;
    }

    public ProcessManifest getManifest() {
        return manifest;
    }

    public PrintStream getOutput() {
        return output;
    }

    public Set<ApiContext> getModels() {
        return models;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public IInfoflowCFG getInterproceduralCFG() {
        return interproceduralCFG == null ? (interproceduralCFG = new InfoflowCFG()) : interproceduralCFG;
    }

    public CallGraph getCallGraph() {
        return Scene.v().getCallGraph();
    }

    public String getAppName() {
        return this.manifest.getApplicationName();
    }

    public String getAppPackage() {
        return this.manifest.getPackageName();
    }

    @Override
    public void handle(PubSub.Message message) {
        if (!(message instanceof Parser.Argument)) {
            return;
        }

        Parser.Argument argument = (Parser.Argument) message;

        switch (argument.getKey()) {
            case ARG_MODELS:
                parseModels(argument.getValue());
                break;
            case ARG_APK:
                parseApk(argument.getValue());
                break;
            case ARG_OUTPUT:
                parseOutput(argument.getValue());
                break;
            case ARG_VERBOSE:
                parseVerbose(argument.getValue());
                break;
            case ARG_PLATFORMS:
                parsePlatforms(argument.getValue());
                break;
            case ARG_SOURCES_AND_SINKS:
                parseSourcesAndSinks(argument.getValue());
                break;
            default: /* we will pass unknown configs */
                break;
        }
    }

    // parseModels will parse the argument ARG_MODELS
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

            // set models
            this.models = new HashSet<>(JSON.parseArray(buffer.toString(), ApiContext.class));
        } catch (IOException e) {
            throw new RuntimeException("Unexpected error generated while reading " + filePath);
        }
    }

    // parseModels will parse the argument ARG_APK
    private void parseApk(@NotNull String apkPath) {
        try {
            if (!apkPath.endsWith(".apk")) {
                throw new RuntimeException("File " + apkPath + " may not be a legal apk file");
            }

            // set manifest
            this.manifest = new ProcessManifest(apkPath);
            // reset options
            this.options[2] = apkPath;  // TODO: 2 should not be hard coded
            // setup default application
            this.app = new SetupApplication(this.androidPlatformsPath, apkPath);
            try {
                this.app.calculateSourcesSinksEntrypoints(this.sourcesAndSinksTextPath);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }

            G.reset();
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    private void parseOutput(@NotNull String filePath) {
        try {
            if ("stderr".equals(filePath)) {
                this.output = System.err;
            } else if ("stdout".equals(filePath)) {
                this.output = System.out;
            } else {
                this.output = new PrintStream(filePath);
            }
        } catch (FileNotFoundException e) {
            logger.w("file \"" + filePath + "\" not found");
            this.output = System.out;
        }
    }

    private void parseVerbose(String verbose) {
        if (null == verbose ||
                "true".equalsIgnoreCase(verbose) ||
                "t".equalsIgnoreCase(verbose)) {
            this.verbose = true;
        } else {
            this.verbose = false;
        }
    }

    // parseModels will parse the argument ARG_PLATFORMS
    private void parsePlatforms(@NotNull String platformsPath) {
        this.androidPlatformsPath = platformsPath;
        this.options[6] = androidPlatformsPath; // TODO: 6 should not be hard coded
        if (this.app != null) {
            this.app = new SetupApplication(this.androidPlatformsPath, this.container.getArg(ARG_APK));
        }
    }

    // parseModels will parse the argument ARG_SOURCES_AND_SINKS
    private void parseSourcesAndSinks(@NotNull String sourcesAndSinksTextPath) {
        this.sourcesAndSinksTextPath = sourcesAndSinksTextPath;
        if (this.app != null) {
            try {
                this.app.calculateSourcesSinksEntrypoints(this.sourcesAndSinksTextPath);
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage());
            }
        }
    }
}
