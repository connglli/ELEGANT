package simonlee.elegant.core.reporter;

import simonlee.elegant.ELEGANT;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.models.context.Context;
import simonlee.elegant.utils.CallPoint;

import java.io.PrintStream;
import java.util.*;

// Reporter is the controller that controls the format of the output technique report
public class Reporter {

    // templates
    private static final String REPORT_HEAD =
            "%s %s, a tool usEd to LocatE fraGmentAtion iNduced compaTibility issues.\n" +
            "Copyright (C) 2017-2018, by %s\n" +
            "All rights reserved\n";
    private static final String REPORT_SUMMARY =
            "SUMMARY:\n" +
            "  total unrecommended manners: %d apis, %d usages\n" +
            "  ignore them if they are reported imprecisely\n";
    private static final String REPORT_API_SIGNATURE =
            "%s\n";
    private static final String REPORT_API_CONTEXT_INCOMPAT_VERSIONS =
            "    incompatible android versions: [~, %d) v (%d, ~]\n";
    private static final String REPORT_API_CONTEXT_INCOMPAT_DEVICES =
            "    incompatible devices: %s\n";
    private static final String REPORT_API_CONTEXT_MESSAGE =
            "    message: %s\n";
    private static final String REPORT_API_USAGE_SUMMARY =
            "  unrecommended usages: %d\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN =
            "  (%d) call chain length: %d (or larger)\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN_AT =
            "    at %s (%s:%d)\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN_BY =
            "    by %s (%s:%d)\n";
    private static final String REPORT_FOOT =
            "Any questions? Contact %s for help\n";

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
        ps.printf(REPORT_HEAD, ELEGANT.APP.NAME, ELEGANT.APP.VERSION, ELEGANT.AUTHOR.NAME);
        ps.println();

        ps.printf(REPORT_SUMMARY, acCount, infoCount);
        ps.println();

        for (Map.Entry<ApiContext, List<Information>> entry : sections.entrySet()) {
            ApiContext        acpair = entry.getKey();
            List<Information> infos  = entry.getValue();

            reportApiContext(ps, acpair);
            reportAllInfomation(ps, infos);
            ps.println();
        }

        ps.printf(REPORT_FOOT, ELEGANT.AUTHOR.EMAIL);
    }

    private void reportApiContext(PrintStream ps, ApiContext acpair) {
        Context context = acpair.getContext();

        int minApiLevel = context.getMinApiLevel();
        int maxApiLevel = context.getMaxApiLevel();

        String[] badDevices = context.getBadDevices();
        String message = context.getMessage();

        ps.printf(REPORT_API_SIGNATURE, acpair.getApi().getSignature());
        ps.printf(REPORT_API_CONTEXT_INCOMPAT_VERSIONS, minApiLevel, maxApiLevel);
        if (null != badDevices && 0 != badDevices.length) {
            ps.printf(REPORT_API_CONTEXT_INCOMPAT_DEVICES, Arrays.toString(badDevices));
        }
        if (null != message) {
            ps.printf(REPORT_API_CONTEXT_MESSAGE, message);
        }
    }

    private void reportAllInfomation(PrintStream ps, List<Information> infos) {
        ps.printf(REPORT_API_USAGE_SUMMARY, infos.size());
        ps.println();

        for (int i = 0; i < infos.size(); i ++) {
            Information info = infos.get(i);
            CallPoint   at   = info.get(info.size() - 1);
            CallPoint   by;

            ps.printf(REPORT_API_USAGE_CALL_CHAIN, i, info.size());
            ps.printf(REPORT_API_USAGE_CALL_CHAIN_AT, at.getMethod(), at.getSrcFile(), at.getStartLineNumber());
            for (int j = info.size() - 2; j >= 0; j --) {
                by = info.get(j);
                ps.printf(REPORT_API_USAGE_CALL_CHAIN_BY, by.getMethod(), by.getSrcFile(), by.getStartLineNumber());
            }
        }
    }

}
