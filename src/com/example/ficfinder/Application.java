package com.example.ficfinder;

import soot.*;
import soot.jimple.infoflow.android.SetupApplication;
import soot.jimple.toolkits.callgraph.CallGraph;
import soot.options.Options;

import java.util.Collections;

public class Application {

    public static final String ANDROID_SDK_PATH = "/Users/user/Desktop/test/fic-finder/assets/android-platforms";

    public static final String ANDROID_APK_PATH = "/Users/user/Desktop/test/fic-finder/assets/CSipSimple.apk";

    public static final String SOURCES_AND_SINKS_TEXT_PATH = "/Users/user/Desktop/test/fic-finder/assets/SoucesAndSinks.txt";

    public static void main(String[] args) {
        String appPath = ANDROID_APK_PATH;
        String androidPlatformPath = ANDROID_SDK_PATH;

        SetupApplication app = new SetupApplication(androidPlatformPath, appPath);
        try {
            app.calculateSourcesSinksEntrypoints(SOURCES_AND_SINKS_TEXT_PATH);
        } catch (Exception e) {
            e.printStackTrace();
        }
        G.reset();

        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(appPath));
        Options.v().set_android_jars(androidPlatformPath);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().setPhaseOption("cg.spark", "on");

        Scene.v().loadNecessaryClasses();

        SootMethod entryPoint = app.getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

        PackManager.v().runPacks();

        CallGraph appCallGraph = Scene.v().getCallGraph();

        System.out.println(appCallGraph.size());
    }

}
