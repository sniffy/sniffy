package io.sniffy.util;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by bedrin on 02.11.2016.
 */
public class IOUtilTest {

    @Test
    public void getProcessID() throws Exception {
        assertNotNull(IOUtil.getProcessID());
    }

}