package simonlee.elegant;

import java.io.File;

public class Dbs {

    public static class Target {
        public static final String ELEGANT = "elegant";
        public static final String SOOT_INFOFLOW = "soot-infoflow";
        public static final String LIB_SCOUT = "libscout";
    }

    // models.json used by elegant
    public static final String MODELS_FILE
            = getResources(Target.ELEGANT, "models.json");
    // android callbacks used by soot-infoflow
    public static final String ANDROID_CALLBACKS_FILE
            = getResources(Target.SOOT_INFOFLOW, "AndroidCallbacks.txt");
    // sources and sinks used by soot-infoflow
    public static final String SOURCES_AND_SINKS_FILE
            = getResources(Target.SOOT_INFOFLOW, "SourcesAndSinks.txt");
    // android callbacks used by soot-infoflow
    public static final String LIB_PROFILES_DIR
            = getResources(Target.LIB_SCOUT, "profiles");

    public static String getResources(String target, String fileName) {
        return "dbs" + File.separator + target + File.separator + fileName;
    }
}
