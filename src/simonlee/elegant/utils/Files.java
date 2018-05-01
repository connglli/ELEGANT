package simonlee.elegant.utils;

public class Files {

    /**
     * getAbsPathFromJar gets the absolute path of a file in jar
     * @param fileName filename in the jar
     * @return         the absolute path of fileName
     */
    public static String getAbsPathFromJar(String fileName) {
        return Files.class.getClassLoader().getResource(fileName).getPath();
    }

}
