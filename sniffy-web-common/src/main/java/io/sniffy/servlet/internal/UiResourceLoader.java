package io.sniffy.servlet.internal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * Loads the namespace-neutral profiler resources generated from the private frontend workspace.
 */
public final class UiResourceLoader {

    public static final String RESOURCE_PREFIX = "/io/sniffy/ui/";

    private UiResourceLoader() {
    }

    public static byte[] load(String resourceName) throws IOException {
        InputStream inputStream = UiResourceLoader.class.getResourceAsStream(RESOURCE_PREFIX + resourceName);
        if (null == inputStream) {
            throw new IOException("Missing Sniffy UI resource " + resourceName);
        }
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } finally {
            inputStream.close();
        }
    }

}
