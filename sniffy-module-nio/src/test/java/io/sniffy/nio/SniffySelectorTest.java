package io.sniffy.nio;

import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.ReflectionUtil;
import org.junit.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelector;
import java.util.*;

import static org.junit.Assert.*;

public class SniffySelectorTest {

    private static class MethodDescriptor {

        private final String methodName;
        private final Object[] parameterTypes;

        public MethodDescriptor(Method method) {
            this(method.getName(), method.getParameterTypes());
        }

        public MethodDescriptor(String methodName, Object[] parameterTypes) {
            this.methodName = methodName;
            this.parameterTypes = parameterTypes;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            MethodDescriptor that = (MethodDescriptor) o;

            if (!methodName.equals(that.methodName)) return false;
            // Probably incorrect - comparing Object[] arrays with Arrays.equals
            return Arrays.equals(parameterTypes, that.parameterTypes);
        }

        @Override
        public int hashCode() {
            int result = methodName.hashCode();
            result = 31 * result + Arrays.hashCode(parameterTypes);
            return result;
        }

        @Override
        public String toString() {
            return "MethodDescriptor{" +
                    "methodName='" + methodName + '\'' +
                    ", parameterTypes=" + Arrays.toString(parameterTypes) +
                    '}';
        }
    }

    @Test
    public void testAllAvailableMethodsAreOverridden() {

        Set<MethodDescriptor> overrideableMethods = new HashSet<MethodDescriptor>();
        Set<MethodDescriptor> nonOverrideableMethods = new HashSet<MethodDescriptor>();

        List<Class<?>> classesToProcess = new LinkedList<Class<?>>();
        classesToProcess.add(AbstractSelector.class);

        while (!classesToProcess.isEmpty()) {
            Class<?> clazz = classesToProcess.remove(0);
            if (clazz.getSuperclass() != Object.class && !clazz.isInterface()) {
                classesToProcess.add(clazz.getSuperclass());
            }
            classesToProcess.addAll(Arrays.asList(clazz.getInterfaces()));

            for (Method method : clazz.getDeclaredMethods()) {
                if (
                                !Modifier.isStatic(method.getModifiers()) &&
                                (Modifier.isProtected(method.getModifiers()) ||
                                Modifier.isPublic(method.getModifiers())) &&
                                !method.isSynthetic()
                ) {
                    if (Modifier.isFinal(method.getModifiers())) {
                        nonOverrideableMethods.add(new MethodDescriptor(method));
                    } else {
                        overrideableMethods.add(new MethodDescriptor(method));
                    }
                }
            }
        }

        overrideableMethods.removeAll(nonOverrideableMethods);

        Set<MethodDescriptor> sniffySelectionKeyMethods = new HashSet<MethodDescriptor>();

        for (Method method : SniffySelector.class.getDeclaredMethods()) {
            sniffySelectionKeyMethods.add(new MethodDescriptor(method));
        }

        for (MethodDescriptor method : overrideableMethods) {
            if (!sniffySelectionKeyMethods.contains(method)) {
                fail("Method " + method + " is not overridden in SniffySelector");
            }
        }

    }

    @Test
    public void testFields() throws Exception {

        Map<String, Field> fieldsMap = new HashMap<String, Field>();

        for (Field field : ReflectionUtil.getDeclaredFieldsHierarchy(AbstractSelector.class)) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fieldsMap.put(field.getName(), field);
            }
        }

        fieldsMap.remove("closed");
        fieldsMap.remove("selectorOpen");

        fieldsMap.remove("interruptor");
        fieldsMap.remove("provider");
        fieldsMap.remove("cancelledKeys");

        assertTrue(fieldsMap + " should be empty",fieldsMap.isEmpty());

    }

    @Test
    public void testCloseSelector() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try (Selector selector = Selector.open()) {
            selector.close();
            assertFalse(selector.isOpen());
            assertTrue(selector instanceof ObjectWrapper);
            //noinspection unchecked
            AbstractSelector delegate = ((ObjectWrapper<AbstractSelector>) selector).getDelegate();
            assertFalse(delegate.isOpen());
        }

    }

}
