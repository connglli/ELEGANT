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

    public static boolean containsIgnoreCase(String src, String ...k) {
        String lowerSrc = src.toLowerCase();
        for (int i = 0, l = k.length; i < l; i ++) {
            if (lowerSrc.contains(k[i].toLowerCase())) {
                return true;
            }
        }

        return false;
    }


}
