package simonlee.elegant;

import simonlee.elegant.d3algo.AbstractD3Algo;
import simonlee.elegant.d3algo.D3AlgoFactory;
import simonlee.elegant.environ.OptParser;
import simonlee.elegant.environ.Environ;
import simonlee.elegant.finder.Finder;
import simonlee.elegant.tracker.Tracker;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.utils.PubSub;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.infoflow.android.manifest.ProcessManifest;
import soot.jimple.infoflow.solver.cfg.IInfoflowCFG;
import soot.jimple.toolkits.callgraph.CallGraph;

import java.util.Map;
import java.util.Set;

// ELEGANT is a delegate following delegating pattern
// All component inside Container are unaware of each other
// what they know is only the Container.
public class ELEGANT {

    // default options to ELEGANT
    public static class DEFAULT_OPTS {
        // required
        public static final String APK_PATH = null;
        // required
        public static final String MODELS_PATH = null;
        // required
        public static final String PLATFORMS_PATH = null;
        // optional
        public static final String D3_ALGO = D3AlgoFactory.D3_WHITELIST;
    }

    // Builder helps to create an ELEGANT instance more easily
    public static class Builder {
        private String  apkPath       = DEFAULT_OPTS.APK_PATH;
        private String  modelsPath    = DEFAULT_OPTS.MODELS_PATH;
        private String  platformsPath = DEFAULT_OPTS.PLATFORMS_PATH;
        private String  d3Algo        = DEFAULT_OPTS.D3_ALGO;

        public Builder withApkPath(String apkPath) {
            this.apkPath = apkPath;
            return this;
        }

        public Builder withModelsPath(String modelsPath) {
            this.modelsPath = modelsPath;
            return this;
        }

        public Builder withPlatformsPath(String platformsPath) {
            this.platformsPath = platformsPath;
            return this;
        }

        public Builder withD3Algo(String d3Algo) {
            this.d3Algo = d3Algo;
            return this;
        }

        public ELEGANT build() {
            if ("".equals(apkPath)) {
                throw new RuntimeException(
                        "path to your apk is missed, remember to use builder.withApkPath(...)");
            } else if ("".equals(modelsPath)) {
                throw new RuntimeException(
                        "path to your models is missed, remember to use builder.withModelsPath(...)");
            } else if("".equals(platformsPath)) {
                throw new RuntimeException(
                        "path to your platforms is missed, remember to use builder.withPlatformsPath(...)");
            } else {
                return new ELEGANT(apkPath, modelsPath, platformsPath, d3Algo);
            }
        }
    }

    // soot-unaware container-unaware components
    private OptParser      optParser;
    private Tracker        tracker;

    // soot-aware container-aware components
    private Finder  finder;
    private Environ environ;

    {
        optParser = new OptParser();
        tracker   = new Tracker();

        environ  = new Environ(this);
        finder   = new Finder(this);
    }

    public void run() {
        optParser.parse();
        finder.find();
    }

    // delegate Environment

    public String[] getOptions() {
        return environ.getOptions();
    }

    public SetupApplication getApp() {
        return environ.getApp();
    }

    public ProcessManifest getManifest() {
        return environ.getManifest();
    }

    public Set<ApiContext> getModels() {
        return environ.getModels();
    }

    public AbstractD3Algo getD3Algo() {
        return environ.getD3Algo();
    }

    public IInfoflowCFG getInterproceduralCFG() {
        return environ.getInterproceduralCFG();
    }

    public CallGraph getCallGraph() {
        return environ.getCallGraph();
    }

    public String getAppName() {
        return environ.getAppName();
    }

    public String getAppPackage() {
        return environ.getAppPackage();
    }

    // delegate OptParser, parser is a publisher, so delegate it

    public Map<String, Object> getOpts() {
        return optParser.getOpts();
    }

    public Object getOpt(String key) {
        return optParser.getOpt(key);
    }

    public int watchOpts(PubSub.Handle handle) {
        return optParser.subscribe(handle);
    }

    public void unwatchOpts(int handler) {
        optParser.unsubscribe(handler);
    }

    // delegate Tracker, tracker is a publisher, so delegate it

    public void emitIssue(PubSub.Message message) {
        this.tracker.publish(message);
    }

    public int watchIssues(PubSub.Handle handle) {
        return tracker.subscribe(handle);
    }

    public void unwatchIssues(int handler) {
        tracker.unsubscribe(handler);
    }

    // constructors

    private ELEGANT(String apkPath, String modelsPath, String platformsPath) {
        this(apkPath, modelsPath, platformsPath, DEFAULT_OPTS.D3_ALGO);
    }

    private ELEGANT(String apkPath, String modelsPath, String platformsPath, String d3Algo) {
        optParser.putOpt(OptParser.OPT_APK_PATH, apkPath);
        optParser.putOpt(OptParser.OPT_MODELS_PATH, modelsPath);
        optParser.putOpt(OptParser.OPT_PLATFORMS_PATH, platformsPath);
        optParser.putOpt(OptParser.OPT_D3_ALGO, d3Algo);
    }
}
