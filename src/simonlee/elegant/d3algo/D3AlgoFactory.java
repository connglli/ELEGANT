package simonlee.elegant.d3algo;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

public class D3AlgoFactory {

    public static final Logger logger = LoggerFactory.getLogger(D3AlgoFactory.class);

    public static final String D3_NONE      = "d3.none";
    public static final String D3_WHITELIST = "d3.whitelist";
    public static final String D3_LIBSCOUT  = "d3.libscout";

    public static AbstractD3Algo getD3Algo(String id, List<String> args) {
        switch (id) {
        case D3_NONE:
            return new D3None();
        case D3_WHITELIST:
            return new D3WhiteList();
        case D3_LIBSCOUT:
            return new D3LibScout(args.get(0), args.subList(1, args.size()));
        default:
            logger.warn("d3 algorithm `" + id + "' not found, use " + D3_NONE);
            return new D3None();
        }
    }

}
