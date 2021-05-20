package io.sniffy.tls;

import io.sniffy.test.junit.SniffyRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(SniffyRunner.class)
public class DecryptBouncyCastleGoogleTrafficTest {

    @SuppressWarnings("CharsetObjectCanBeUsed")
    @Test
    public void testGoogleTraffic() throws Exception {

        // https://github.com/sniffy/sniffy/issues/478
        // if signed jars (like BC) are loaded before Sniffy, it would cause issues/478
        // hence we're moving BC logic to a separate test class helper
        DecryptBouncyCastleGoogleTrafficTestHelper.testGoogleTrafficImpl();

    }

}
