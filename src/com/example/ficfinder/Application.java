package com.example.ficfinder;

import com.example.ficfinder.finder.Finder;
import com.example.ficfinder.tracker.IssueHandle;
import com.example.ficfinder.tracker.Tracker;

import java.util.Arrays;

public class Application {

    public static void main(String[] args) {
        // subscribe some handles
        Tracker.v().subscribe(new IssueHandle());

        // parse args
        Configs.v().parse(Arrays.asList(args));

        // run finder
        Finder.v().run();
    }

}
