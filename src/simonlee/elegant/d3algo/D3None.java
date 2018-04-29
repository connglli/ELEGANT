package simonlee.elegant.d3algo;

import java.util.Arrays;
import java.util.List;

public class D3None extends D3AbstractWhiteList {

    private List<String> officialList = Arrays.asList(
        "android",      // android official
        "java",         // java official
        "org.omg",      // extends to java
        "org.w3c",      // extends to java
        "org.xml",      // extends to java
        "org.apache",   // apache organization
        "com.sun",      // sun
        "org.eclipse"   // eclipse
    );

    @Override
    protected List<String> getWhiteList() {
        return officialList;
    }

}
