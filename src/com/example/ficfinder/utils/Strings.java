package com.example.ficfinder.utils;

public class Strings {

    public static boolean contains(String src, String ...k) {
        for (int i = 0, l = k.length; i < l; i ++) {
            if (src.contains(k[i])) {
                return true;
            }
        }

        return false;
    }

}
