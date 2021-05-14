package io.sniffy.test.junit;

import io.sniffy.Sniffy;
import io.sniffy.configuration.SniffyConfiguration;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.TestClass;

/**
 * JUnit4 Runner implementation which initializes Sniffy; can be used in corner cases
 *
 * @since 3.1.12
 */
public class SniffyRunner extends BlockJUnit4ClassRunner {

    static {

        SniffyConfiguration.INSTANCE.setDecryptTls(true);
        SniffyConfiguration.INSTANCE.setMonitorSocket(true);
        SniffyConfiguration.INSTANCE.setMonitorNio(true);
        SniffyConfiguration.INSTANCE.setMonitorJdbc(true);
        Sniffy.initialize();

    }

    public SniffyRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    public SniffyRunner(TestClass testClass) throws InitializationError {
        super(testClass);
    }

}