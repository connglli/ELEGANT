package com.example.ficfinder.utils;

public class Logger {

    public static final int ERROR = 1;

    public static final int WARNING = 2;

    public static final int INFORMATION = 3;

    public static final int EVERYTHING = 4;

    public static int level = EVERYTHING;

    private Class<?> cls;

    public Logger(Class<?> cls) {
        this.cls = cls;
    }

    public void i(String log) {
        if (level > INFORMATION) {
            System.out.println(cls  + ": " + "@INFORMATION: " + log);
        }
    }

    public void w(String log) {
        if (level > WARNING) {
            System.out.println(cls  + ": " + "@WARNING: " + log);
        }
    }

    public void e(String log) {
        if (level > ERROR) {
            System.out.println(cls  + ": " + "@ERROR: " + log);
        }
    }
}
