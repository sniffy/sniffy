package io.sniffy.socket;

import io.sniffy.Sniffer;
import io.sniffy.Spy;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

public class TcpExpectationTest extends BaseSocketTest {

    @Test
    public void testExactConnections() {

        try (Spy<?> s = Sniffer.expect(TcpConnections.exact(2))) {

            performSocketOperation();
            performSocketOperation();

        }


    }

}
