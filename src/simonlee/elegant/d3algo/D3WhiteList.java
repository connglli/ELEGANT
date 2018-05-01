package simonlee.elegant.d3algo;

import java.util.Arrays;
import java.util.List;

public class D3WhiteList extends D3AbstractWhiteList {

    private List<String> whiteList = Arrays.asList(
        "android",                           // android official
        "java",                              // java official
        "com.jakewharton",                   // butterknife
        "com.github",                        // github related, glide, etc.
        "com.squareup",                      // okhttp, retrofit, etc.
        "com.google",                        // google related, gson, guava, etc.
        "com.handmark.pulltorefresh",        // Android Pull-to-Refresh
        "com.facebook",                      // facebook related sdk
        "com.afollestad.materialdialogs",    // material dialogs
        "com.bumptech.glide",                // glide
        "com.viewpagerindicator",            // ViewPager related
        "com.actionbarsherlock",             // ActionBar related
        "org.openintents",                   // Intent related
        "com.melnykov.fab",                  // FloatingActionButton related
        "com.getbase.floatingactionbutton",  // FloatingActionButton related
        "com.helpshift",                     // help shift android sdk
        "com.crashlytcics",                  // firebase
        "com.makeramen.roundedimageview",    // image view
        "com.nispok.snackbar",               // snackbar
        "com.nineoldandroids",               // android animation library for os prior to Honeycomb
        "com.nostra13.universalimageloader", // image loader
        "rx",                                // reactivex java
        "org.omg",                           // extends to java
        "org.w3c",                           // extends to java
        "org.xml",                           // extends to java
        "org.apache",                        // apache organization
        "com.sun",                           // sun
        "org.eclipse"                        // eclipse
    );

    @Override
    protected List<String> getWhiteList() {
        return whiteList;
    }
}
