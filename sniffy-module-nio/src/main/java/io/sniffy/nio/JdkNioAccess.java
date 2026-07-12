package io.sniffy.nio;

import sun.misc.Unsafe;
import sun.nio.ch.SelChImpl;
import sun.nio.ch.SelectionKeyImpl;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.channels.spi.SelectorProvider;
import java.nio.channels.SelectionKey;
import java.nio.channels.spi.AbstractSelectableChannel;

import static io.sniffy.util.ReflectionUtil.invokeMethod;

/**
 * Resolved-once access to the small set of JDK-private facilities required by NIO instrumentation.
 * All private member names, module opening and Unsafe use for NIO belong in this compatibility class.
 */
final class JdkNioAccess implements NioProviderAccess {

    private static volatile JdkNioAccess resolved;

    private final Unsafe unsafe;
    private final Object providerFieldBase;
    private final long providerFieldOffset;
    private final String providerFieldDescription;
    private final MethodHandle removeChannelKey;

    private JdkNioAccess(Unsafe unsafe, Field providerField, MethodHandles.Lookup trustedLookup) throws Exception {
        this.unsafe = unsafe;
        this.providerFieldBase = unsafe.staticFieldBase(providerField);
        this.providerFieldOffset = unsafe.staticFieldOffset(providerField);
        this.providerFieldDescription = providerField.getDeclaringClass().getName() + "." + providerField.getName();
        this.removeChannelKey = trustedLookup.findVirtual(AbstractSelectableChannel.class, "removeKey",
                MethodType.methodType(void.class, SelectionKey.class));
    }

    static JdkNioAccess resolve() throws JdkNioAccessException {
        JdkNioAccess access = resolved;
        if (access != null) {
            return access;
        }
        synchronized (JdkNioAccess.class) {
            access = resolved;
            if (access == null) {
                access = resolveUncached();
                resolved = access;
            }
            return access;
        }
    }

    private static JdkNioAccess resolveUncached() throws JdkNioAccessException {
        try {
            SelectorProvider.provider(); // initialize the JDK provider holder before resolving its slot
            Unsafe unsafe = resolveUnsafe();
            Field providerField = resolveProviderField();
            MethodHandles.Lookup trustedLookup = resolveTrustedLookup(unsafe);
            openSunNioChannelPackage(trustedLookup);
            Class.forName("sun.nio.ch.SelChImpl", false, JdkNioAccess.class.getClassLoader());
            return new JdkNioAccess(unsafe, providerField, trustedLookup);
        } catch (Throwable e) {
            throw new JdkNioAccessException("Required JDK NIO access is unavailable", e);
        }
    }

    private static Unsafe resolveUnsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }

    private static Field resolveProviderField() throws Exception {
        try {
            Class<?> holder = Class.forName("java.nio.channels.spi.SelectorProvider$Holder");
            Field field = holder.getDeclaredField("INSTANCE");
            if (Modifier.isStatic(field.getModifiers()) && SelectorProvider.class.isAssignableFrom(field.getType())) {
                return field;
            }
        } catch (ClassNotFoundException e) {
            // Java 8 and some alternate runtimes store the provider on SelectorProvider itself.
        } catch (NoSuchFieldException e) {
            // Fall through to the legacy layout.
        }

        Field field = SelectorProvider.class.getDeclaredField("provider");
        if (!Modifier.isStatic(field.getModifiers()) || !SelectorProvider.class.isAssignableFrom(field.getType())) {
            throw new NoSuchFieldException("SelectorProvider provider slot has an unsupported type");
        }
        return field;
    }

    private static MethodHandles.Lookup resolveTrustedLookup(Unsafe unsafe) throws Exception {
        Field trustedLookupField = MethodHandles.Lookup.class.getDeclaredField("IMPL_LOOKUP");
        return (MethodHandles.Lookup) unsafe.getObject(
                unsafe.staticFieldBase(trustedLookupField), unsafe.staticFieldOffset(trustedLookupField));
    }

    private static void openSunNioChannelPackage(MethodHandles.Lookup trustedLookup) throws Throwable {
        Class<?> moduleClass;
        try {
            moduleClass = Class.forName("java.lang.Module");
        } catch (ClassNotFoundException e) {
            return; // Java 8 has no module boundaries.
        }

        Class<?> selChImplClass = Class.forName("sun.nio.ch.SelChImpl");
        Method getModule = Class.class.getMethod("getModule");
        Object javaBaseModule = getModule.invoke(selChImplClass);
        MethodHandle addOpens = trustedLookup.findVirtual(moduleClass, "implAddOpens",
                MethodType.methodType(void.class, String.class));
        addOpens.invoke(javaBaseModule, "sun.nio.ch");
    }

    @Override
    public SelectorProvider getSelectorProvider() {
        return (SelectorProvider) unsafe.getObjectVolatile(providerFieldBase, providerFieldOffset);
    }

    @Override
    public void setSelectorProvider(SelectorProvider provider) {
        unsafe.putObjectVolatile(providerFieldBase, providerFieldOffset, provider);
    }

    @Override
    public String describeProviderSlot() {
        return providerFieldDescription;
    }

    void removeChannelKey(AbstractSelectableChannel channel, SelectionKey key) throws JdkNioAccessException {
        try {
            removeChannelKey.invoke(channel, key);
        } catch (Throwable e) {
            throw new JdkNioAccessException("Failed to remove a Sniffy key from wrapper channel " + channel, e);
        }
    }

    void translateAndSetInterestOps(SelChImpl channel, int ops, SelectionKeyImpl key) throws JdkNioAccessException {
        try {
            invokeMethod(SelChImpl.class, channel, "translateAndSetInterestOps",
                    Integer.TYPE, ops, SelectionKeyImpl.class, key, Void.TYPE);
        } catch (Exception e) {
            throw new JdkNioAccessException("Failed to update delegate selector interest operations", e);
        }
    }

    int translateInterestOps(SelChImpl channel, int ops) throws JdkNioAccessException {
        try {
            return invokeMethod(SelChImpl.class, channel, "translateInterestOps",
                    Integer.TYPE, ops, Integer.TYPE);
        } catch (Exception e) {
            throw new JdkNioAccessException("Failed to translate delegate selector interest operations", e);
        }
    }

    void park(SelChImpl channel, int event, long nanos) throws JdkNioAccessException {
        try {
            invokeMethod(SelChImpl.class, channel, "park",
                    Integer.TYPE, event, Long.TYPE, nanos, Void.TYPE);
        } catch (Exception e) {
            throw new JdkNioAccessException("Failed to park a delegate NIO channel", e);
        }
    }

    void park(SelChImpl channel, int event) throws JdkNioAccessException {
        try {
            invokeMethod(SelChImpl.class, channel, "park", Integer.TYPE, event, Void.TYPE);
        } catch (Exception e) {
            throw new JdkNioAccessException("Failed to park a delegate NIO channel", e);
        }
    }

    static final class JdkNioAccessException extends Exception {
        JdkNioAccessException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
