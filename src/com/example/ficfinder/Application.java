package com.example.ficfinder;

import com.example.ficfinder.core.Core;

import java.util.Arrays;

public class Application {

    public static void main(String[] args) {
        Configs.v().parse(Arrays.asList(args));
        Core.v().run();
        System.out.println(Env.v());
    }

}
