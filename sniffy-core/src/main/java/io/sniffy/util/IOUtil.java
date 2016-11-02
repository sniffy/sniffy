package io.sniffy.util;

import sun.misc.IOUtils;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

public class IOUtil {

    protected static final String MAIN_CLASS_PROPERTY_NAME = "sun.java.command";
    protected static final String WORKING_DIRECTORY_PROPERTY_NAME = "user.dir";
    protected static final String JAVA_CLASS_PATH_PROPERTY_NAME = "java.class.path";
    protected static final String TEMP_DIRECTORY_PROPERTY_NAME = "java.io.tmpdir";

    protected static volatile String applicationId;
    protected static volatile File applicationSniffyFolder;

    public static void closeSilently(Closeable closeable) {
        if (null != closeable) {
            try {
                closeable.close();
            } catch (IOException e) {
                // TODO: some logging maybe?
            }
        }
    }

    public static File getApplicationSniffyFolder() {

        if (null == applicationSniffyFolder) {
            synchronized (IOUtils.class) {
                if (null == applicationSniffyFolder) {
                    File applicationSniffyFolder = new File(new File(System.getProperty(TEMP_DIRECTORY_PROPERTY_NAME)), getApplicationId());
                    applicationSniffyFolder.mkdirs();
                    IOUtil.applicationSniffyFolder = applicationSniffyFolder;
                }
            }
        }

        return applicationSniffyFolder;

    }

    public static String getApplicationId() {

        if (null == applicationId) {
            synchronized (IOUtils.class) {
                if (null == applicationId) {
                    StringBuilder sb = new StringBuilder();

                    String mainClassName = System.getProperty(MAIN_CLASS_PROPERTY_NAME);
                    if (null == mainClassName) mainClassName = "SniffyApplication";

                    sb.append(mainClassName).append('.');

                    String workingDir = System.getProperty(WORKING_DIRECTORY_PROPERTY_NAME);
                    String classPath = System.getProperty(JAVA_CLASS_PATH_PROPERTY_NAME);

                    int hashCode = workingDir != null ? workingDir.hashCode() : 0;
                    hashCode = 31 * hashCode + (classPath != null ? classPath.hashCode() : 0);

                    sb.append(Integer.toString(hashCode));

                    applicationId = sb.toString();
                }
            }
        }

        return applicationId;

    }

}
