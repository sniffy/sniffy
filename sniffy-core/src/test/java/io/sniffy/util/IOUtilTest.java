package io.sniffy.util;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Created by bedrin on 02.11.2016.
 */
public class IOUtilTest {

    private Properties backup;

    @Before
    public void backupSystemProperties() {
        backup = new Properties();
        backup.putAll(System.getProperties());
    }

    @After
    public void restoreSystemProperties() {
        System.setProperties(backup);
    }

    @Test
    public void getProcessID() throws Exception {
        assertNotNull(IOUtil.getProcessID());
    }

    @Test
    public void getProcessIDDependsOnWorkingDirectory() throws Exception {
        System.setProperty(IOUtil.WORKING_DIRECTORY_PROPERTY_NAME, "foo");
        String pid1 = IOUtil.getProcessID();
        System.setProperty(IOUtil.WORKING_DIRECTORY_PROPERTY_NAME, "bar");
        String pid2 = IOUtil.getProcessID();
        assertNotNull(pid1, pid2);
    }

    @Test
    public void getProcessIDDependsOnClassPath() throws Exception {
        System.setProperty(IOUtil.JAVA_CLASS_PATH_PROPERTY_NAME, "foo");
        String pid1 = IOUtil.getProcessID();
        System.setProperty(IOUtil.JAVA_CLASS_PATH_PROPERTY_NAME, "bar");
        String pid2 = IOUtil.getProcessID();
        assertNotNull(pid1, pid2);
    }

    @Test
    public void getProcessIDStartsWithMainClassName() throws Exception {
        System.setProperty(IOUtil.MAIN_CLASS_PROPERTY_NAME, "foo.bar.baz");
        assertTrue(IOUtil.getProcessID().startsWith("foo.bar.baz."));
    }

}