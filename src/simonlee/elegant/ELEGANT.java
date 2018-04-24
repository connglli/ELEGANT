package simonlee.elegant;

import java.util.Arrays;

public class ELEGANT {

    // ELEGANT information
    public static class APP {
        public static String NAME    = "ELEGANT";
        public static String MAJOR_V = "1";
        public static String MINOR_V = "0";
        public static String PATCH_V = "0";
        public static String VERSION = MAJOR_V + "." + MINOR_V + "." + PATCH_V;
    }

    // author information
    public static class AUTHOR {
        public static String NAME = "Simon Lee";
        public static String EMAIL = "leetsong.lc@gmail.com";
    }

    public static void main(String[] args) {
        new Container().run(Arrays.asList(args));
    }

}
