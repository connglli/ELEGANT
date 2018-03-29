package simonlee.elegant.utils;

import java.util.ArrayList;
import java.util.List;

public class Logger {

    public static class Formatter {

        public static int BOLD      = 1;
        public static int UNDERLINE = 4;
        public static int RED       = 31;
        public static int GREEN     = 32;
        public static int YELLOW    = 33;

        List<Integer> formats;

        public Formatter() {
            this.formats = new ArrayList<>();
        }

        public Formatter bold() {
            this.formats.add(BOLD);
            return this;
        }

        public Formatter underline() {
            this.formats.add(UNDERLINE);
            return this;
        }

        public Formatter red() {
            this.formats.add(RED);
            return this;
        }

        public Formatter green() {
            this.formats.add(GREEN);
            return this;
        }

        public Formatter yellow() {
            this.formats.add(YELLOW);
            return this;
        }

        public Formatter append(int x) {
            this.formats.add(x);
            return this;
        }

        public String build(String s) {
            if (0 == this.formats.size()) { return s; }

            StringBuilder buf = new StringBuilder(s.length());

            buf.append("\033[");
            buf.append(this.formats.get(0));
            for (int i = 1; i < this.formats.size(); i ++) {
                buf.append(";");
                buf.append(this.formats.get(i));
            }
            buf.append("m");
            buf.append(s);
            buf.append("\033[0m");

            return buf.toString();
        }

    }

    public static final int ERROR = 1;

    public static final int WARNING = 2;

    public static final int INFORMATION = 3;

    public static final int EVERYTHING = 4;

    public static int level = EVERYTHING;

    public static boolean willPrintClass = false;

    private Class<?> cls;

    public Logger(Class<?> cls) {
        this.cls = cls;
    }

    public void c(String log) {
        System.out.println(new Formatter().bold().build(log));
    }

    public void i(String log) {
        if (level > INFORMATION) {
            System.out.println(new Formatter().green().bold().build((
                    (willPrintClass ? (cls + ": ") : "") + "@INFORMATION: " + log)));
        }
    }

    public void w(String log) {
        if (level > WARNING) {
            System.out.println(new Formatter().yellow().bold().build((
                    (willPrintClass ? (cls + ": ") : "") + "@WARNING: " + log)));
        }
    }

    public void e(String log) {
        if (level > ERROR) {
            System.out.println(new Formatter().red().bold().build((
                    (willPrintClass ? (cls + ": ") : "") + "@ERROR: " + log)));
        }
    }
}