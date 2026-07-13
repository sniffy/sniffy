package io.sniffy.test.contract;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ServletNamespaceContractTest {

    @Test
    public void webArtifactsHaveEquivalentApisAndIsolatedNamespaces() throws IOException {
        compare("sniffy-web", "sniffy-web-javax");
    }

    @Test
    public void springArtifactsHaveEquivalentApisAndIsolatedNamespaces() throws IOException {
        compare("sniffy-spring", "sniffy-spring-javax");
    }

    private static void compare(String jakartaArtifact, String javaxArtifact) throws IOException {
        File jakartaJar = artifact(jakartaArtifact);
        File javaxJar = artifact(javaxArtifact);
        Map<String, List<String>> jakartaApi = api(jakartaJar, "javax/servlet", "javax.servlet");
        Map<String, List<String>> javaxApi = api(javaxJar, "jakarta/servlet", "jakarta.servlet");
        assertFalse("No classes found in " + jakartaJar, jakartaApi.isEmpty());
        assertEquals("Class/API drift between " + jakartaArtifact + " and " + javaxArtifact,
                normalize(jakartaApi), normalize(javaxApi));
    }

    private static File artifact(String artifactId) {
        File[] files = new File("target/artifacts").listFiles();
        assertNotNull("No artifacts copied", files);
        for (File file : files) {
            String name = file.getName();
            boolean siblingPrefix = artifactId.equals("sniffy-web") && name.startsWith("sniffy-web-javax-") ||
                    artifactId.equals("sniffy-spring") && name.startsWith("sniffy-spring-javax-");
            if (!siblingPrefix && name.startsWith(artifactId + "-") && name.endsWith(".jar")) {
                return file;
            }
        }
        throw new AssertionError("Missing copied artifact " + artifactId);
    }

    private static Map<String, List<String>> api(File file, String forbiddenInternal,
                                                  String forbiddenDotted) throws IOException {
        Map<String, List<String>> result = new LinkedHashMap<String, List<String>>();
        JarFile jarFile = new JarFile(file);
        try {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("io/sniffy/") || !entry.getName().endsWith(".class")) {
                    continue;
                }
                byte[] bytes = read(jarFile.getInputStream(entry));
                String content = new String(bytes, "ISO-8859-1");
                assertFalse(file.getName() + "!" + entry.getName() + " leaks " + forbiddenInternal,
                        content.contains(forbiddenInternal));
                assertFalse(file.getName() + "!" + entry.getName() + " leaks " + forbiddenDotted,
                        content.contains(forbiddenDotted));
                ApiVisitor visitor = new ApiVisitor();
                new ClassReader(bytes).accept(visitor, ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                result.put(entry.getName(), visitor.members);
            }
        } finally {
            jarFile.close();
        }
        return result;
    }

    private static Map<String, List<String>> normalize(Map<String, List<String>> api) {
        Map<String, List<String>> normalized = new LinkedHashMap<String, List<String>>();
        for (Map.Entry<String, List<String>> entry : api.entrySet()) {
            List<String> members = new ArrayList<String>();
            for (String member : entry.getValue()) {
                members.add(member.replace("jakarta/servlet", "javax/servlet")
                        .replace("jakarta.servlet", "javax.servlet"));
            }
            Collections.sort(members);
            normalized.put(entry.getKey(), members);
        }
        return normalized;
    }

    private static byte[] read(InputStream inputStream) throws IOException {
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int count;
            while ((count = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, count);
            }
            return outputStream.toByteArray();
        } finally {
            inputStream.close();
        }
    }

    private static class ApiVisitor extends ClassVisitor {
        private final List<String> members = new ArrayList<String>();

        private ApiVisitor() {
            super(Opcodes.ASM9);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName,
                          String[] interfaces) {
            if (visible(access)) {
                members.add("C " + accessMask(access) + " " + name + " " + signature + " " + superName +
                        " " + join(interfaces));
            }
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            if (visible(access)) {
                members.add("F " + accessMask(access) + " " + name + " " + descriptor + " " + signature);
            }
            return null;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature,
                                         String[] exceptions) {
            if (visible(access)) {
                members.add("M " + accessMask(access) + " " + name + " " + descriptor + " " + signature +
                        " " + join(exceptions));
            }
            return null;
        }

        private static boolean visible(int access) {
            return 0 != (access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED));
        }

        private static int accessMask(int access) {
            return access & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL |
                    Opcodes.ACC_ABSTRACT | Opcodes.ACC_INTERFACE | Opcodes.ACC_ANNOTATION | Opcodes.ACC_ENUM);
        }

        private static String join(String[] values) {
            if (null == values) {
                return "";
            }
            StringBuilder result = new StringBuilder();
            for (String value : values) {
                if (result.length() > 0) {
                    result.append(',');
                }
                result.append(value);
            }
            return result.toString();
        }
    }
}
