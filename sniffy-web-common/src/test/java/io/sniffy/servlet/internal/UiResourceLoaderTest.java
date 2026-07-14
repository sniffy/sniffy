package io.sniffy.servlet.internal;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class UiResourceLoaderTest {

    @Test
    public void allGeneratedResourcesAreAvailableFromStablePaths() throws Exception {
        assertResource("sniffy.js");
        assertResource("sniffy.min.js");
        assertResource("sniffy.map");
    }

    private static void assertResource(String resourceName) throws Exception {
        assertNotNull(UiResourceLoader.class.getResource(UiResourceLoader.RESOURCE_PREFIX + resourceName));
        assertTrue(UiResourceLoader.load(resourceName).length > 0);
    }

}
