package simonlee.elegant;

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

    public static class Builder {
        private String apkPath       = "";
        private String modelsPath    = "";
        private String platformsPath = "";

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
                return new ELEGANT(apkPath, modelsPath, platformsPath);
            }
        }
    }

    // soot-unaware container-unaware components
    private OptParser optParser;
    private Tracker   tracker;

    // soot-aware container-aware components
    private Finder  finder;
    private Environ environ;

    {
        optParser = new OptParser();
        tracker   = new Tracker();

        environ  = new Environ(this);
        finder   = new Finder(this);
    }

    private ELEGANT(String apkPath, String modelsPath, String platformsPath) {
        optParser.putOpt(OptParser.OPT_APK_PATH, apkPath);
        optParser.putOpt(OptParser.OPT_MODELS_PATH, modelsPath);
        optParser.putOpt(OptParser.OPT_PLATFORMS_PATH, platformsPath);
    }

    public void run() {
        optParser.parse();
        finder.run();
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
}
