package simonlee.elegant.d3algo;

import soot.SootClass;
import soot.SootField;
import soot.SootMethod;

public interface AbstractD3Algo {

    /**
     * is3rdPartyLibClass checks whether c is a 3rd party class
     * @param c a soot class
     * @return  true if c is
     */
    boolean is3rdPartyLibClass(SootClass c);

    /**
     * is3rdPartyLibMethod checks whether m is a 3rd party method
     * @param m a soot method
     * @return  true if m is
     */
    boolean is3rdPartyLibMethod(SootMethod m);

    /**
     * is3rdPartyLibField checks whether f is a 3rd party field
     * @param f a soot field
     * @return  true if f is
     */
    boolean is3rdPartyLibField(SootField f);
}
