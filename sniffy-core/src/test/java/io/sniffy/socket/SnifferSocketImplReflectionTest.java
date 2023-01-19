package io.sniffy.socket;

import io.sniffy.reflection.field.NonStaticFieldRef;
import io.sniffy.reflection.method.MethodKey;
import io.sniffy.reflection.method.NonStaticMethodRef;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.net.SocketImpl;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class SnifferSocketImplReflectionTest {

    @Test
    public void testNoUnknownFields() {

        Map<String, NonStaticFieldRef<SocketImpl, Object>> fieldsMap = $(SocketImpl.class).findNonStaticFields(null, true);

        fieldsMap.remove("socket"); // TODO: can we handle it nicely ?
        fieldsMap.remove("serverSocket");

        fieldsMap.remove("fd");

        fieldsMap.remove("address");

        fieldsMap.remove("port");
        fieldsMap.remove("localport");

        assertTrue(fieldsMap + " should be empty", fieldsMap.isEmpty());

    }

    @Test
    public void testAllMethodsDelegated() {

        final Set<MethodKey> nonOverrideableMethods = new HashSet<>();

        Map<MethodKey, NonStaticMethodRef<SocketImpl>> overrideableMethods = $(SocketImpl.class).getNonStaticMethods(
                null,
                (methodKey, method) -> {
                    if (
                            (Modifier.isProtected(method.getModifiers()) ||
                                    Modifier.isPublic(method.getModifiers()))
                    ) {
                        if (Modifier.isFinal(method.getModifiers())) {
                            nonOverrideableMethods.add(methodKey);
                        } else {
                            return !nonOverrideableMethods.contains(methodKey);
                        }
                    }
                    return false;
                }
        );

        Class<? extends SocketImpl> sniffySocketImplClass = (new SnifferSocketImplFactory().createSocketImpl()).getClass();
        Map<MethodKey, ? extends NonStaticMethodRef<? extends SocketImpl>> declaredMethods =
                $(sniffySocketImplClass).getNonStaticMethods(SocketImpl.class, null);

        overrideableMethods.forEach((methodKey, methodRef) -> {
            if (!declaredMethods.containsKey(methodKey)) {
                fail("Method " + methodKey + " is not wrapped by Sniffy");
            }
        });

    }

}
