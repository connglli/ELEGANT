package com.example;

public class DozeChecker {

    public boolean isCompatible() {
        return os.Bulid.SDK_VERSION > 26;
    }

    public void go() {

    }

    public void goWithChecking() {
        if (isCompatible()) {
            go();
        }
    }

    public void goWithoutChecking() {
        go();
    }

    public static void main(String args[]) {
        DozeChecker d = new DozeChecker();
        d.goWithoutChecking();
        d.goWithChecking();
    }

}

class os {
    public static class Bulid {
        public static final int SDK_VERSION = 1;
    }
}