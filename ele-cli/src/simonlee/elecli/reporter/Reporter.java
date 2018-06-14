package simonlee.elecli.reporter;

import simonlee.elegant.ELEGANT;
import simonlee.elecli.CLI;
import simonlee.elegant.models.ApiContext;
import simonlee.elegant.models.context.Context;
import simonlee.elegant.utils.CallPoint;
import soot.jimple.infoflow.android.manifest.ProcessManifest;

import java.io.File;
import java.io.PrintStream;
import java.util.*;

// Reporter is the controller that controls the format of the output technique report
public class Reporter {

    // templates
    private static final String REPORT_HEAD_TEMPLATE =
            "%s %s, %s\n" +
            "Copyright (C) 2017-2018, by %s\n" +
            "All rights reserved\n";
    private static final String REPROT_APP_TEMPLATE =
            "APPLICATION:\n" +
            "  file:       %s\n" +
            "  name:       %s\n" +
            "  package:    %s\n" +
            "  version:    %s\n" +
            "  min sdk:    %s\n" +
            "  target sdk: %s\n" +
            "  activities: %s\n" +
            "  services:   %s\n" +
            "  receivers:  %s\n" +
            "  providers:  %s\n";
    private static final String REPORT_SUMMARY_TEMPLATE =
            "SUMMARY:\n" +
            "  total unrecommended manners: %d apis, %d/%d usages (call-sites/call-chains)\n" +
            "  ignore them if they are reported imprecisely\n";
    private static final String REPORT_API_SIGNATURE_TEMPLATE =
            "%s\n";
    private static final String REPORT_API_CONTEXT_INCOMPAT_VERSIONS_TEMPLATE =
            "    incompatible android versions: [~, %d) v (%d, ~]\n";
    private static final String REPORT_API_CONTEXT_INCOMPAT_DEVICES_TEMPLATE =
            "    incompatible devices: %s\n";
    private static final String REPORT_API_CONTEXT_MESSAGE_TEMPLATE =
            "    message: %s\n";
    private static final String REPORTER_API_CONTEXT_IMPORTANT_TEMPLATE =
            "    IMPORTANT!!!!\n";
    private static final String REPORT_API_USAGE_SUMMARY_TEMPLATE =
            "  unrecommended usages: %d/%d (call-sites/call-chains)\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN_USAGE_TEMPLATE =
            "  (%d) %s, usages: %d (call-chains)\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN_LENGTH_TEMPLATE =
            "  (%d) call chain length: %d (or larger)\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN_AT_TEMPLATE =
            "    at %s (%s:%d)\n";
    private static final String REPORT_API_USAGE_CALL_CHAIN_BY_TEMPLATE =
            "    by %s (%s:%d)\n";
    private static final String REPORT_FOOT_VERBOSE_HELP_TEMPLATE =
            "Use --verbose to show more details\n";
    private static final String REPORT_FOOT_TEMPLATE =
            "Any questions? Contact %s for help\n";

    // every Information represents a call chain of a model
    public static class Information extends ArrayList<CallPoint> {

        /**
         * at returns the call site call point
         * @return
         */
        public CallPoint at() {
            if (this.size() == 0) {
                return null;
            } else {
                return this.get(this.size() - 1);
            }
        }

        /**
         * by returns the idx-th caller, 1 based
         * @param idx
         * @return
         */
        public CallPoint by(int idx) {
            int ridx = this.size() - 1 - idx;
            return ridx < 0 ? null : this.get(ridx);
        }

        public int getByCount() {
            return this.size() == 0 ? 0 : this.size() - 1;
        }

    }

    // every section classifies the call chains by their call site
    private static class Section {

        private Map<CallPoint, List<Information>> parts;
        private int callSiteCount  = 0;
        private int callChainCount = 0;

        public Section() {
            parts = new HashMap<>();
        }

        public Map<CallPoint, List<Information>> getParts() {
            return parts;
        }

        /**
         * put puts an info into this section
         * @param i
         * @return  1 if new call site added, or 0
         */
        public int put(Information i) {
            if (parts.containsKey(i.at())) {
                parts.get(i.at()).add(i);
                callChainCount ++;
                return 0;
            } else {
                List<Information> l = new ArrayList<>();
                l.add(i);
                parts.put(i.at(), l);
                callSiteCount ++;
                callChainCount ++;
                return 1;
            }
        }

        public int getCallSiteCount() {
            return callSiteCount;
        }

        public int getCallChainCount() {
            return callChainCount;
        }
    }

    // the elegant instance
    private ELEGANT elegant;
    // the command line options
    private CLI.GlobalOptions cliOpts;

    // sections saves all information submitted
    private Map<ApiContext, Section> sections = new HashMap<>();
    private int acpairCount = 0;
    private int callSiteCount = 0;
    private int callChainCount = 0;

    public Reporter(ELEGANT elegant, CLI.GlobalOptions cliOpts) {
        this.elegant = elegant;
        this.cliOpts = cliOpts;
    }

    /**
     * submit will submit an information to model
     *
     * @param model the related model
     * @param info  the information to be submit
     */
    public void submit(ApiContext model, Information info) {
        if (!sections.containsKey(model)) {
            sections.put(model, new Section());
            acpairCount++;
        }

        callSiteCount += sections.get(model).put(info);
        callChainCount++;
    }

    /**
     * report will report all information saved in reported via ps
     *
     * @param ps a print stream
     */
    public void report(PrintStream ps) {
        ps.printf(REPORT_HEAD_TEMPLATE,
                CLI.APP.NAME, CLI.APP.VERSION, CLI.APP.DESCRIPTION, CLI.AUTHOR.NAME);
        ps.println();

        reportAppInfo(ps);
        ps.println();

        ps.printf(REPORT_SUMMARY_TEMPLATE,
                acpairCount, callSiteCount, callChainCount);
        ps.println();

        for (Map.Entry<ApiContext, Section> entry : sections.entrySet()) {
            ApiContext acpair = entry.getKey();
            Section    section = entry.getValue();

            reportApiContext(ps, acpair);
            reportAllInfomation(ps, section);
            ps.println();
        }

        if (!this.cliOpts.isVerbose()) {
            ps.printf(REPORT_FOOT_VERBOSE_HELP_TEMPLATE);
        }
        ps.printf(REPORT_FOOT_TEMPLATE, CLI.AUTHOR.EMAIL);
    }

    private void reportAppInfo(PrintStream ps) {
        ProcessManifest m = this.elegant.getManifest();
        String file    = "[not provided]";
        String appName = "[not provided]";
        String version = "[not provided]";
        String pkg     = "[not provided]";
        String minSdk  = "[not provided]";
        String tgtSdk  = "[not provided]";
        String nr_a    = "[not provided]";
        String nr_s    = "[not provided]";
        String nr_r    = "[not provided]";
        String nr_p    = "[not provided]";

        try {
            String[] fullPath = m.getApk().getAbsolutePath().split(File.separator);
            file = fullPath[fullPath.length - 1];
        } catch (Exception e) {}
        try {
            String[] fullNameTokens = m.getApplicationName().split("\\.");
            appName = fullNameTokens[fullNameTokens.length - 1];
        } catch (Exception e) {}
        try { pkg = m.getPackageName(); } catch (Exception e) {}
        try { version = String.valueOf(m.getVersionName()); } catch (Exception e) {}
        try { minSdk  = String.valueOf(m.getMinSdkVersion()); } catch (Exception e) {}
        try { tgtSdk  = String.valueOf(m.targetSdkVersion()); } catch (Exception e) {}
        try { nr_a    = String.valueOf(m.getActivities().size()); } catch (Exception e) {}
        try { nr_s    = String.valueOf(m.getServices().size()); } catch (Exception e) {}
        try { nr_r    = String.valueOf(m.getReceivers().size()); } catch (Exception e) {}
        try { nr_p    = String.valueOf(m.getProviders().size()); } catch (Exception e) {}

        ps.printf(REPROT_APP_TEMPLATE,
                file, appName, pkg, version, minSdk, tgtSdk, nr_a, nr_s, nr_r, nr_p);
    }

    private void reportApiContext(PrintStream ps, ApiContext acpair) {
        Context context = acpair.getContext();

        int minApiLevel = context.getMinApiLevel();
        int maxApiLevel = context.getMaxApiLevel();

        String[] badDevices = context.getBadDevices();
        String message = context.getMessage();

        ps.printf(REPORT_API_SIGNATURE_TEMPLATE, acpair.getApi().getSignature());
        ps.printf(REPORT_API_CONTEXT_INCOMPAT_VERSIONS_TEMPLATE, minApiLevel, maxApiLevel);
        if (null != badDevices && 0 != badDevices.length) {
            ps.printf(REPORT_API_CONTEXT_INCOMPAT_DEVICES_TEMPLATE, Arrays.toString(badDevices));
        }
        if (null != message) {
            ps.printf(REPORT_API_CONTEXT_MESSAGE_TEMPLATE, message);
        }
        if (acpair.isImportant()) {
            ps.printf(REPORTER_API_CONTEXT_IMPORTANT_TEMPLATE);
        }
    }

    private void reportAllInfomation(PrintStream ps, Section section) {
        ps.printf(REPORT_API_USAGE_SUMMARY_TEMPLATE, section.getCallSiteCount(), section.getCallChainCount());
        ps.println();

        if (this.cliOpts.isVerbose()) {
            int i = 0;
            for (Map.Entry<CallPoint, List<Information>> part : section.getParts().entrySet()) {
                List<Information> infos = part.getValue();
                for (int j = 0; j < infos.size(); j ++) {
                    Information info = infos.get(j);
                    CallPoint   at   = info.at();
                    CallPoint   by;

                    ps.printf(REPORT_API_USAGE_CALL_CHAIN_LENGTH_TEMPLATE, i, info.size());
                    ps.printf(REPORT_API_USAGE_CALL_CHAIN_AT_TEMPLATE,
                            at.getMethod(), at.getSrcFile(), at.getStartLineNumber());
                    for (int k = 1; k <= info.getByCount(); k ++) {
                        by = info.by(k);
                        ps.printf(REPORT_API_USAGE_CALL_CHAIN_BY_TEMPLATE,
                                by.getMethod(), by.getSrcFile(), by.getStartLineNumber());
                    }
                    i ++;
                }
            }
        } else {
            int i = 0;
            for (Map.Entry<CallPoint, List<Information>> part : section.getParts().entrySet()) {
                CallPoint         at    = part.getKey();
                List<Information> infos = part.getValue();
                ps.printf(REPORT_API_USAGE_CALL_CHAIN_USAGE_TEMPLATE, i, at.getMethod(), infos.size());
                ps.printf(REPORT_API_USAGE_CALL_CHAIN_AT_TEMPLATE,
                        at.getMethod(), at.getSrcFile(), at.getStartLineNumber());
                i ++;
            }
        }

    }

}
