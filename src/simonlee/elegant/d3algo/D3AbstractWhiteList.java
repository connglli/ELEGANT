package simonlee.elegant.d3algo;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

import java.util.List;

public abstract class D3AbstractWhiteList implements AbstractD3Algo {

    /**
     * getWhiteList returns the white list
     * @return the white list
     */
    protected abstract List<String> getWhiteList();

    @Override
    public boolean is3rdPartyLibClass(SootClass c) {
        return isIn3rdPartyLibrary(c.getJavaPackageName());
    }

    @Override
    public boolean is3rdPartyLibMethod(SootMethod m) {
        return isIn3rdPartyLibrary(m.getDeclaringClass().getJavaPackageName());
    }

    @Override
    public boolean is3rdPartyLibField(SootField f) {
        return isIn3rdPartyLibrary(f.getDeclaringClass().getJavaPackageName());
    }

    // a white list way uses the signatures only
    private boolean isIn3rdPartyLibrary(String signature) {
        List<String> whiteList = getWhiteList();
        for (String s : whiteList) {
            if (signature.startsWith(s)) { return true; }
        }

        return false;
    }
}
