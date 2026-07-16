package io.sniffy.tls;

import org.junit.Test;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.Assert.fail;

public class SniffySSLEngineBytecodeTest {

    @Test
    public void compiledBytecodeDoesNotUseJava9ByteBufferFluentSetters() throws Exception {
        assertNoJava9ByteBufferSetter(SniffySSLEngine.class);
    }

    private static void assertNoJava9ByteBufferSetter(Class<?> type) throws IOException {
        DataInputStream input = new DataInputStream(openClassBytes(type));
        input.readInt();
        input.readUnsignedShort();
        input.readUnsignedShort();
        Object[] constantPool = new Object[input.readUnsignedShort()];
        for (int i = 1; i < constantPool.length; i++) {
            int tag = input.readUnsignedByte();
            switch (tag) {
                case 1:
                    constantPool[i] = input.readUTF();
                    break;
                case 3:
                case 4:
                    input.readInt();
                    break;
                case 5:
                case 6:
                    input.readLong();
                    i++;
                    break;
                case 7:
                case 8:
                case 16:
                    constantPool[i] = input.readUnsignedShort();
                    break;
                case 9:
                case 10:
                case 11:
                case 12:
                case 18:
                    constantPool[i] = new int[]{input.readUnsignedShort(), input.readUnsignedShort()};
                    break;
                case 15:
                    input.readUnsignedByte();
                    input.readUnsignedShort();
                    break;
                default:
                    fail("Unsupported constant pool tag " + tag + " in " + type.getName());
            }
        }
        for (int i = 1; i < constantPool.length; i++) {
            Object entry = constantPool[i];
            if (entry instanceof int[]) {
                int[] pair = (int[]) entry;
                String owner = className(constantPool, pair[0]);
                String[] nameAndType = nameAndType(constantPool, pair[1]);
                if ("java/nio/ByteBuffer".equals(owner)
                        && ("position".equals(nameAndType[0]) || "limit".equals(nameAndType[0]))
                        && "(I)Ljava/nio/ByteBuffer;".equals(nameAndType[1])) {
                    fail(type.getName() + " links to Java 9+ ByteBuffer." + nameAndType[0]
                            + "(int):ByteBuffer instead of Java 8 Buffer descriptor");
                }
            }
        }
    }

    private static InputStream openClassBytes(Class<?> type) throws IOException {
        String resource = "/" + type.getName().replace('.', '/') + ".class";
        InputStream input = type.getResourceAsStream(resource);
        if (input == null) throw new IOException("Cannot find " + resource);
        return input;
    }

    private static String className(Object[] constantPool, int index) {
        Object classEntry = constantPool[index];
        if (classEntry instanceof Integer) return (String) constantPool[(Integer) classEntry];
        return null;
    }

    private static String[] nameAndType(Object[] constantPool, int index) {
        Object nameAndType = constantPool[index];
        if (!(nameAndType instanceof int[])) return new String[]{null, null};
        int[] pair = (int[]) nameAndType;
        return new String[]{(String) constantPool[pair[0]], (String) constantPool[pair[1]]};
    }

}
