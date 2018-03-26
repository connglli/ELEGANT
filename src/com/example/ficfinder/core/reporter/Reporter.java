package com.example.ficfinder.core.reporter;

import com.example.ficfinder.models.ApiContext;
import com.example.ficfinder.models.context.Context;
import com.example.ficfinder.utils.CallPoint;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Reporter is the controller that controls the format of the output technique report
public class Reporter {

    private static final String REPORT_HEAD =
            "%s, a FIC (Fragmentation Induced Compatibility) issues detector.\n" +
            "Copyright (C) 2017-2018, by Simon Lee\n" +
            "All rights reserved\n";

    private static final String REPORT_FOOT =
            "Any questions? Contact leetsong.lc@gmail.com for help\n";

    // every Information represents a call chain of a model
    public static class Information extends ArrayList<CallPoint> { }

    // sections saves all information submitted
    private Map<ApiContext, List<Information>> sections = new HashMap<>();
    private int acCount   = 0;
    private int infoCount = 0;

    /**
     * submit will submit an information to model
     *
     * @param model the related model
     * @param info  the information to be submit
     */
    public void submit(ApiContext model, Information info) {
        if (!sections.containsKey(model)) {
            sections.put(model, new ArrayList<>());
            acCount ++;
        }

        sections.get(model).add(info);
        infoCount ++;
    }

    /**
     * report will report all information saved in reported via ps
     *
     * @param ps a print stream
     */
    public void report(PrintStream ps) {
        ps.printf(REPORT_HEAD, "FicFinder"); // TODO should be extracted out as a static property in somewhere
        ps.println();

        ps.println("SUMMARY:");
        ps.printf ("  total unrecommended manners: %d apis, %d usages\n", acCount, infoCount);
        ps.println("  ignore them if they are reported imprecisely");
        ps.println();

        for (Map.Entry<ApiContext, List<Information>> entry : sections.entrySet()) {
            ApiContext        acpair = entry.getKey();
            List<Information> infos  = entry.getValue();

            ps.println(acpair.getApi().getSignature());
            reportApiContext(ps, acpair);
            reportAllInfomation(ps, infos);
            ps.println();
        }

        ps.println(REPORT_FOOT);
    }

    private void reportApiContext(PrintStream ps, ApiContext acpair) {
        Context context = acpair.getContext();

        int minApiLevel = context.getMinApiLevel();
        int maxApiLevel = context.getMaxApiLevel();

        String[] badDevices = context.getBadDevices();
        String message = context.getMessage();

        ps.printf("    incompatible android versions: [~, %d) v (%d, ~]\n", minApiLevel, maxApiLevel);
        if (null != badDevices && 0 != badDevices.length) {
            ps.printf("    incompatible devices: %s\n", badDevices.toString());
        }
        if (null != message) {
            ps.printf("    message: %s\n", message);
        }
    }

    private void reportAllInfomation(PrintStream ps, List<Information> infos) {
        ps.printf("  unrecommended usages: %d\n", infos.size());
        ps.println();

        for (int i = 0; i < infos.size(); i ++) {
            Information info = infos.get(i);

            ps.printf("  (%d) call chain length: %d (or larger)\n", i, info.size());

            CallPoint at = info.get(info.size() - 1);
            ps.printf("    at %s (%s:%d)\n", at.getMethod(), at.getSrcFile(), at.getStartLineNumber());

            CallPoint by;
            for (int j = info.size() - 2; j >= 0; j --) {
                by = info.get(j);
                ps.printf("    by %s (%s:%d)\n", by.getMethod(), by.getSrcFile(), by.getStartLineNumber());
            }
        }
    }

}
