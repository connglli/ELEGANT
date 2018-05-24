package simonlee.elegantcli;

import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import simonlee.elegant.ELEGANT;
import simonlee.elegant.Dbs;
import simonlee.elegant.d3algo.D3AlgoFactory;
import simonlee.elegantcli.reporter.PIssueHandle;
import simonlee.elegantcli.reporter.RIssueHandle;
import simonlee.elegantcli.reporter.Reporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class CLI {

    private static Logger logger = LoggerFactory.getLogger(CLI.class);

    // ELEGANT information
    public static class APP {
        public static final String NAME        = "ELEGANT CLI Tool";
        public static final String DESCRIPTION = "a tool usEd to LocatE fraGmentAtion iNduced compaTibility issues";
        public static final String MAJOR_V     = "1";
        public static final String MINOR_V     = "0";
        public static final String PATCH_V     = "0";
        public static final String VERSION     = MAJOR_V + "." + MINOR_V + "." + PATCH_V;
    }

    // author information
    public static class AUTHOR {
        public static final String NAME = "Simon Lee";
        public static final String EMAIL = "leetsong.lc@gmail.com";
    }

    // cli fullOpts information
    public static class CLI_OPTIONS {
        public static final String OPT_MODELS = "m";
        public static final String OPTL_MODELS = "models";
        public static final String OPT_MODELS_ARG_NAME = "file";
        public static final String OPT_MODELS_DESCRIPTION = "custom api context models, in json format";

        public static final String OPT_PLATFORMS = "p";
        public static final String OPTL_PLATFORMS = "platforms";
        public static final String OPT_PLATFORMS_ARG_NAME = "direcotry";
        public static final String OPT_PLATFORMS_DESCRIPTION = "android platforms";

        public static final String OPT_OUTPUT = "o";
        public static final String OPTL_OUTPUT = "output";
        public static final String OPT_OUTPUT_ARG_NAME = "file";
        public static final String OPT_OUTPUT_DESCRIPTION = "redirect technique report output to <file>";

        public static final String OPT_D3_ALGO = "d3";
        public static final String OPTL_D3_ALGO = "d3-algo";
        public static final String OPT_D3_ALGO_ARG_NAME = "value";
        public static final String OPT_D3_ALGO_DESCRIPTION = "algorithms used in 3rd party library detection, <value> is one of: "
                                                            + D3AlgoFactory.D3_NONE + ", "
                                                            + D3AlgoFactory.D3_WHITELIST + ", "
                                                            + D3AlgoFactory.D3_LIBSCOUT + ".";

        public static final String OPT_VERBOSE = "V";
        public static final String OPTL_VERBOSE = "verbose";
        public static final String OPT_VERBOSE_DESCRIPTION = "print verbose information";

        public static final String OPT_HELP = "h";
        public static final String OPTL_HELP = "help";
        public static final String OPT_HELP_DESCRIPTION = "show help";

        public static final String OPT_VERSION = "v";
        public static final String OPTL_VERSION = "version";
        public static final String OPT_VERSION_DESCRIPTION = "show version";
    }

    // GlobalOptions stores all parsed options
    public static class GlobalOptions {
        private String apk = null;
        private String models = null != ELEGANT.DEFAULT_OPTS.MODELS_PATH
                                ? ELEGANT.DEFAULT_OPTS.MODELS_PATH
                                // defaults to the default models.json
                                : Dbs.MODELS_FILE;
        private String platforms =  null != ELEGANT.DEFAULT_OPTS.PLATFORMS_PATH
                                    ? ELEGANT.DEFAULT_OPTS.PLATFORMS_PATH
                                    // defaults to $ANDROID_HOME/platforms
                                    : System.getenv("ANDROID_HOME") + File.separator + "platforms";
        private String d3Algo = null != ELEGANT.DEFAULT_OPTS.D3_ALGO
                                ? ELEGANT.DEFAULT_OPTS.D3_ALGO
                                // defaults to none
                                : D3AlgoFactory.D3_WHITELIST;
        private boolean verbose = false; // defaults to no verbose
        private PrintStream output = System.out; // defaults to stdout

        public String getApk() {
            return apk;
        }

        public void setApk(String apk) {
            this.apk = apk;
        }

        public String getModels() {
            return models;
        }

        public void setModels(String models) {
            this.models = models;
        }

        public String getPlatforms() {
            return platforms;
        }

        public void setPlatforms(String platforms) {
            this.platforms = platforms;
        }

        public String getD3Algo() {
            return d3Algo;
        }

        public void setD3Algo(String d3Algo) {
            this.d3Algo = d3Algo;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }

        public PrintStream getOutput() {
            return output;
        }

        public void setOutput(PrintStream output) {
            this.output = output;
        }
    }

    // the usage header
    public static final String USAGE_HEADER = "java -jar elegant-cli.jar [option ...] <apk>";

    // unparsed options
    private String[] unparsedOpts;
    // options' full information
    private Options fullOpts;
    // global parsed options
    private GlobalOptions globalParsedOpts = new GlobalOptions();

    public CLI(String[] args) {
        this.unparsedOpts = args;
    }

    public void start() {
        parseCLIOptions();

        // construct an ELEGANT instance
        ELEGANT.Builder builder = new ELEGANT.Builder();
        ELEGANT elegant = builder
                .withApkPath(globalParsedOpts.getApk())
                .withModelsPath(globalParsedOpts.getModels())
                .withPlatformsPath(globalParsedOpts.getPlatforms())
                .withD3Algo(globalParsedOpts.d3Algo)
                .build();

        // watch and report issues
        Reporter reporter = new Reporter(elegant, globalParsedOpts);
        elegant.watchIssues(new PIssueHandle(reporter));
        elegant.watchIssues(new RIssueHandle(reporter));

        // here we go
        elegant.run();

        // report issues
        reporter.report(globalParsedOpts.getOutput());
    }

    public void stop(int status) {
        System.exit(status);
    }

    public static void main(String[] args) {
        new CLI(args).start();
    }

    // parseCLIOptions will parse unparsed fullOpts
    private void parseCLIOptions() {
        try {
            CommandLineParser cliParser = new BasicParser();
            CommandLine cli = cliParser.parse(setUpOptions(), this.unparsedOpts);

            if (cli.hasOption(CLI_OPTIONS.OPT_HELP)) {
                usage();
                stop(0);
            }

            if (cli.hasOption(CLI_OPTIONS.OPT_VERSION)) {
                version();
                stop(0);
            }

            if (cli.hasOption(CLI_OPTIONS.OPT_VERBOSE)) {
                globalParsedOpts.setVerbose(true);
            }

            if (cli.hasOption(CLI_OPTIONS.OPT_MODELS)) {
                globalParsedOpts.setModels(cli.getOptionValue(CLI_OPTIONS.OPT_MODELS));
            }

            if (cli.hasOption(CLI_OPTIONS.OPT_PLATFORMS)) {
                globalParsedOpts.setPlatforms(cli.getOptionValue(CLI_OPTIONS.OPT_PLATFORMS));
            }

            if (cli.hasOption(CLI_OPTIONS.OPT_OUTPUT)) {
                String o = cli.getOptionValue(CLI_OPTIONS.OPT_OUTPUT);
                if ("stderr".equals(o)) {
                    globalParsedOpts.setOutput(System.err);
                } else if ("stdout".equals(o)) {
                    globalParsedOpts.setOutput(System.out);
                } else {
                    try {
                        File f = new File(o);

                        if (f.isDirectory()) {
                            System.err.println("Output file `" + o + "' has to be a file");
                            stop(1);
                        } else {
                            globalParsedOpts.setOutput(new PrintStream(o));
                        }
                    } catch (FileNotFoundException e) {
                        System.err.println("Output file " + o + " does not exist, use stdout instead");
                    }
                }
            }

            if (cli.hasOption(CLI_OPTIONS.OPT_D3_ALGO)) {
                globalParsedOpts.setD3Algo(cli.getOptionValue(CLI_OPTIONS.OPT_D3_ALGO));
            }

            String[] args = cli.getArgs();
            if (0 == args.length) {
                System.err.println("<apk> is missed");
                usage();
                stop(1);
            } else {
                globalParsedOpts.setApk(args[0]);
            }
        } catch (ParseException e) {
            System.err.println("Some required options are missed!");
            usage();
            stop(1);
        }
    }

    @SuppressWarnings("static-access")
    private Options setUpOptions() {
        fullOpts = new Options();

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_MODELS)
                .hasArg(true)
                .withArgName(CLI_OPTIONS.OPT_MODELS_ARG_NAME)
                .withDescription(CLI_OPTIONS.OPT_MODELS_DESCRIPTION)
                .create(CLI_OPTIONS.OPT_MODELS));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_OUTPUT)
                .hasArg(true)
                .withArgName(CLI_OPTIONS.OPT_OUTPUT_ARG_NAME)
                .withDescription(CLI_OPTIONS.OPT_OUTPUT_DESCRIPTION)
                .isRequired(false)
                .create(CLI_OPTIONS.OPT_OUTPUT));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_PLATFORMS)
                .hasArg(true)
                .withArgName(CLI_OPTIONS.OPT_PLATFORMS_ARG_NAME)
                .withDescription(CLI_OPTIONS.OPT_PLATFORMS_DESCRIPTION)
                .isRequired(false)
                .create(CLI_OPTIONS.OPT_PLATFORMS));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_D3_ALGO)
                .hasArg(true)
                .withArgName(CLI_OPTIONS.OPT_D3_ALGO_ARG_NAME)
                .withDescription(CLI_OPTIONS.OPT_D3_ALGO_DESCRIPTION)
                .create(CLI_OPTIONS.OPT_D3_ALGO));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_VERBOSE)
                .withDescription(CLI_OPTIONS.OPT_VERBOSE_DESCRIPTION)
                .isRequired(false)
                .create(CLI_OPTIONS.OPT_VERBOSE));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_HELP)
                .withDescription(CLI_OPTIONS.OPT_HELP_DESCRIPTION)
                .isRequired(false)
                .create(CLI_OPTIONS.OPT_HELP));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLI_OPTIONS.OPTL_VERSION)
                .withDescription(CLI_OPTIONS.OPT_VERSION_DESCRIPTION)
                .isRequired(false)
                .create(CLI_OPTIONS.OPT_VERSION));

        return fullOpts;
    }

    private void version() {
        System.out.printf("%s, %s\n", APP.NAME, APP.DESCRIPTION);
        System.out.printf("version %s\n", APP.VERSION);
    }

    private void usage() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp(USAGE_HEADER, "option:", fullOpts, "");
    }

}
