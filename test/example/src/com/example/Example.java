package com.example;

import com.example.os.*;

public class Example {

    public static final int MAX_SIZE = 100;

    public boolean isCompatible() {
        return Build.SDK_VERSION > 26;
    }

    public int go() {
        int mul = 1;

        for (int i = 1; i <= MAX_SIZE; i ++) {
            mul *= i;
        }

        return mul;
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
        Example d = new Example();
        d.goWithoutChecking();
        d.goWithChecking();
    }

}
