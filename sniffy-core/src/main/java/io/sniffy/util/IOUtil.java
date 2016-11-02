package io.sniffy.util;

public class IOUtil {

    private static final String MAIN_CLASS_PROPERTY_NAME = "sun.java.command";
    private static final String WORKING_DIRECTORY_PROPERTY_NAME = "user.dir";
    private static final String JAVA_CLASS_PATH_PROPERTY_NAME = "java.class.path";

    public static String getProcessID() {

        StringBuilder sb = new StringBuilder();

        String mainClassName = System.getProperty(MAIN_CLASS_PROPERTY_NAME);
        if (null == mainClassName) mainClassName = "SniffyApplication";

        sb.append(mainClassName).append('.');

        String workingDir = System.getProperty(WORKING_DIRECTORY_PROPERTY_NAME);
        String classPath = System.getProperty(JAVA_CLASS_PATH_PROPERTY_NAME);

        int hashCode = workingDir != null ? workingDir.hashCode() : 0;
        hashCode = 31 * hashCode + (classPath != null ? classPath.hashCode() : 0);

        sb.append(Integer.toString(hashCode));

        return sb.toString();

    }

}
