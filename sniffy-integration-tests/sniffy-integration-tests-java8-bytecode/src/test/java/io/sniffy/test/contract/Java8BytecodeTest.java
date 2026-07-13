package io.sniffy.test.contract;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertTrue;

public class Java8BytecodeTest {

    @Test
    public void legacyAndAggregateArtifactsRemainJava8Bytecode() throws IOException {
        File directory = new File("target/artifacts");
        File[] jars = directory.listFiles();
        assertTrue("No contract artifacts copied", null != jars && jars.length == 4);
        int inspected = 0;
        for (File jar : jars) {
            if (!jar.getName().endsWith(".jar")) {
                continue;
            }
            JarFile jarFile = new JarFile(jar);
            try {
                Enumeration<JarEntry> entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    JarEntry entry = entries.nextElement();
                    if (!entry.getName().startsWith("io/sniffy/") || !entry.getName().endsWith(".class")) {
                        continue;
                    }
                    InputStream inputStream = jarFile.getInputStream(entry);
                    try {
                        byte[] header = new byte[8];
                        int offset = 0;
                        while (offset < header.length) {
                            int count = inputStream.read(header, offset, header.length - offset);
                            if (count < 0) {
                                break;
                            }
                            offset += count;
                        }
                        assertTrue("Invalid class header in " + jar.getName() + "!" + entry.getName(), offset == 8);
                        int major = (header[6] & 0xff) << 8 | header[7] & 0xff;
                        assertTrue(jar.getName() + "!" + entry.getName() + " has class major " + major,
                                major <= 52);
                        inspected++;
                    } finally {
                        inputStream.close();
                    }
                }
            } finally {
                jarFile.close();
            }
        }
        assertTrue("No Sniffy classes inspected", inspected > 100);
    }
}
