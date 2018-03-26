package com.example.ficfinder.utils;

import java.util.Arrays;

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

    public static String camelToUnderline(String camel) {
        if (null == camel) { return null; }

        char          c;
        int           l = camel.length();
        StringBuilder s = new StringBuilder(2 * l);

        for (int i = 0; i < l; i ++) {
            c = camel.charAt(i);
            if ('A' <= c &&  c <= 'Z') {
                s.append('_');
                s.append(Character.toLowerCase(c));
            } else {
                s.append(c);
            }
        }

        return s.toString();
    }

    public static String underlineToCamel(String underline) {
        if (null == underline) { return null; }

        char          c;
        int           l = underline.length();
        StringBuilder s = new StringBuilder(l);

        int i = 0;
        while (i < l) {
            c = underline.charAt(i);

            if ('_' == c) {
                while ('_' == c) {
                    i += 1; c = underline.charAt(i);
                }
                s.append(Character.toUpperCase(c));
            } else {
                s.append(c);
            }

            i += 1;
        }

        return s.toString();
    }

}
