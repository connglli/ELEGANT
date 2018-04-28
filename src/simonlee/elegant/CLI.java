package simonlee.elegant;

import org.apache.commons.cli.*;
import simonlee.elegant.reporter.PIssueHandle;
import simonlee.elegant.reporter.RIssueHandle;
import simonlee.elegant.reporter.Reporter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class CLI {

    // ELEGANT information
    public static class APP {
        public static final String NAME    = "ELEGANT CLI Tool";
        public static final String MAJOR_V = "1";
        public static final String MINOR_V = "0";
        public static final String PATCH_V = "0";
        public static final String VERSION = MAJOR_V + "." + MINOR_V + "." + PATCH_V;
    }

    // author information
    public static class AUTHOR {
        public static final String NAME = "Simon Lee";
        public static final String EMAIL = "leetsong.lc@gmail.com";
    }

    // cli fullOpts information
    public static class CLIOptions {
        public static final String OPT_MODELS = "m";
        public static final String OPTL_MODELS = "models";
        public static final String OPT_MODELS_ARG_NAME = "file";
        public static final String OPT_MODELS_DESCRIPTION = "custom api context models, in json format";

        public static final String OPT_PLATFORMS = "p";
        public static final String OPTL_PLATFORMS = "platforms";
        public static final String OPT_PLATFORMS_ARG_NAME = "direcotry";
        public static final String OPT_PLATFORMS_DESCRIPTION = "android platforms, defaults to $ANDROID_HOME" + File.separator + "platforms";

        public static final String OPT_OUTPUT = "o";
        public static final String OPTL_OUTPUT = "output";
        public static final String OPT_OUTPUT_ARG_NAME = "file";
        public static final String OPT_OUTPUT_DESCRIPTION = "redirect technique report output to <file>";

        public static final String OPT_VERBOSE = "v";
        public static final String OPTL_VERBOSE = "verbose";
        public static final String OPT_VERBOSE_DESCRIPTION = "print verbose information";

        public static final String OPT_HELP = "h";
        public static final String OPTL_HELP = "help";
        public static final String OPT_HELP_DESCRIPTION = "show help";
    }

    // GlobalOptions stores all parsed options
    public static class GlobalOptions {
        private String apk;
        private String models = "models.json"; // defaults to the default models.json
        private String platforms = System.getenv("ANDROID_HOME") + File.separator + "platforms"; // defaults to $ANDROID_HOME/platforms
        private boolean verbose = false; // defaults to not verbose
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
    public static final String USAGE_HEADER = "java -jar ele-cli.jar [options ...] <apk>";

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

            if (cli.hasOption(CLIOptions.OPT_HELP)) {
                usage();
                stop(0);
            }

            if (cli.hasOption(CLIOptions.OPT_VERBOSE)) {
                globalParsedOpts.setVerbose(true);
            }

            if (cli.hasOption(CLIOptions.OPT_MODELS)) {
                globalParsedOpts.setModels(cli.getOptionValue(CLIOptions.OPT_MODELS));
            }

            if (cli.hasOption(CLIOptions.OPT_PLATFORMS)) {
                globalParsedOpts.setPlatforms(cli.getOptionValue(CLIOptions.OPT_PLATFORMS));
            }

            if (cli.hasOption(CLIOptions.OPT_OUTPUT)) {
                String o = cli.getOptionValue(CLIOptions.OPT_OUTPUT);
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
                .withLongOpt(CLIOptions.OPTL_MODELS)
                .hasArg(true)
                .withArgName(CLIOptions.OPT_MODELS_ARG_NAME)
                .withDescription(CLIOptions.OPT_MODELS_DESCRIPTION)
                .create(CLIOptions.OPT_MODELS));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLIOptions.OPTL_OUTPUT)
                .hasArg(true)
                .withArgName(CLIOptions.OPT_OUTPUT_ARG_NAME)
                .withDescription(CLIOptions.OPT_OUTPUT_DESCRIPTION)
                .isRequired(false)
                .create(CLIOptions.OPT_OUTPUT));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLIOptions.OPTL_PLATFORMS)
                .hasArg(true)
                .withArgName(CLIOptions.OPT_PLATFORMS_ARG_NAME)
                .withDescription(CLIOptions.OPT_PLATFORMS_DESCRIPTION)
                .isRequired(false)
                .create(CLIOptions.OPT_PLATFORMS));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLIOptions.OPTL_VERBOSE)
                .withDescription(CLIOptions.OPT_VERBOSE_DESCRIPTION)
                .isRequired(false)
                .create(CLIOptions.OPT_VERBOSE));

        fullOpts.addOption(OptionBuilder
                .withLongOpt(CLIOptions.OPTL_HELP)
                .withDescription(CLIOptions.OPT_HELP_DESCRIPTION)
                .isRequired(false)
                .create(CLIOptions.OPT_HELP));

        return fullOpts;
    }

    private void usage() {
        HelpFormatter hf = new HelpFormatter();
        hf.printHelp(USAGE_HEADER, "options:", fullOpts, "");
    }

}
