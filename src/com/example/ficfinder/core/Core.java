package com.example.ficfinder.core;


import com.example.ficfinder.Configs;
import com.example.ficfinder.Env;
import soot.*;
import soot.options.Options;

import java.util.Collections;

public class Core {

    // Singleton

    private static Core instance;
    
    public static Core v() {
        if (instance == null) {
            instance = new Core();
        }

        return instance;
    }

    public void run() {
        this.setUp();
        this.core();
        this.tearDown();
    }

    private void setUp() {
        // gc
        System.gc();

        // options pass to soot/flowdroid
        Options.v().set_src_prec(Options.src_prec_apk);
        Options.v().set_process_dir(Collections.singletonList(Configs.v().getArg(Configs.APK)));
        Options.v().set_android_jars(Env.ANDROID_PLATFORMS_PATH);
        Options.v().set_whole_program(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().setPhaseOption("cg.spark", "on");

        Scene.v().loadNecessaryClasses();

        // fake main created by flowdroid
        SootMethod entryPoint = Env.v().getApp().getEntryPointCreator().createDummyMain();
        Options.v().set_main_class(entryPoint.getSignature());
        Scene.v().setEntryPoints(Collections.singletonList(entryPoint));

        // run it
        PackManager.v().runPacks();
    }

    /**
     * Core Algothrims of fic-fonder
     *
     */
    private void core() {
        System.out.println("Core Algothrims goes here");

//        CallGraph callGraph = Scene.v().getCallGraph();
//        List<ApiContext> models = Env.v().getModels();
//
//        for (ApiContext model : models) {
//            String type = model.getApi().getClass().toString().split(" ")[1];
//
//            switch (type) {
//                case ApiField.TAG:
//                    ApiField apiField = (ApiField) model.getApi();
//                    SootField sootField = Scene.v().getField(apiField.getSiganiture());
//                    break;
//                case ApiMethod.TAG:
//                    ApiMethod apiMethod = (ApiMethod) model.getApi();
//                    SootMethod sootMethod = Scene.v().getMethod(apiMethod.getSiganiture());
//                    break;
//                case ApiIface.TAG:
//                    ApiIface apiIface = (ApiIface) model.getApi(); break;
//            }
//        }
    }

    private void tearDown() {

    }

}
