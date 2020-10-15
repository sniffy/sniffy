package io.sniffy.nio;

import io.sniffy.util.ExceptionUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.ProtocolFamily;
import java.nio.channels.*;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;

public class SniffySelectorProvider extends SelectorProvider {

    private final SelectorProvider delegate;

    public SniffySelectorProvider(SelectorProvider delegate) {
        this.delegate = delegate;
    }

    public static void install() throws IOException {
        SelectorProvider delegate = SelectorProvider.provider();

        if (null != delegate && SniffySelectorProvider.class.equals(delegate.getClass())) {
            return;
        }

        try {
            initializeUsingHolderSubClass(new SniffySelectorProvider(delegate));
        } catch (IOException ex) {
            try {
                Class<?> clazz = Class.forName("java.nio.channels.spi.SelectorProvider");

                Field lockField = clazz.getDeclaredField("lock");
                lockField.setAccessible(true);

                Object lock = lockField.get(null);

                Field instanceField = clazz.getDeclaredField("provider");
                instanceField.setAccessible(true);

                Field modifiersField = getModifiersField();
                modifiersField.setAccessible(true);
                modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

                synchronized (lock) {
                    instanceField.set(null, new SniffySelectorProvider(delegate));
                }

            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to initialize SniffySelectorProvider", e);
            } catch (NoSuchFieldException e) {
                throw new IOException("Failed to initialize SniffySelectorProvider", e);
            } catch (IllegalAccessException e) {
                throw new IOException("Failed to initialize SniffySelectorProvider", e);
            }
        }
    }

    public static void uninstall() throws IOException {

        // TODO: save default to static field and restore here

        /*SelectorProvider delegate = SelectorProvider.provider();
        try {
            initializeUsingHolderSubClass(delegate);
        } catch (IOException ex) {
            try {
                Class<?> clazz = Class.forName("java.nio.channels.spi.SelectorProvider");

                Field lockField = clazz.getDeclaredField("lock");
                lockField.setAccessible(true);

                Object lock = lockField.get(null);

                Field instanceField = clazz.getDeclaredField("provider");
                instanceField.setAccessible(true);

                Field modifiersField = getModifiersField();
                modifiersField.setAccessible(true);
                modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

                synchronized (lock) {
                    instanceField.set(null, delegate);
                }

            } catch (ClassNotFoundException e) {
                throw new IOException("Failed to initialize SniffySelectorProvider", e);
            } catch (NoSuchFieldException e) {
                throw new IOException("Failed to initialize SniffySelectorProvider", e);
            } catch (IllegalAccessException e) {
                throw new IOException("Failed to initialize SniffySelectorProvider", e);
            }
        }*/

    }

    private static void initializeUsingHolderSubClass(SelectorProvider provider) throws IOException {
        try {
            Class<?> holderClass = Class.forName("java.nio.channels.spi.SelectorProvider$Holder");

            Field instanceField = holderClass.getDeclaredField("INSTANCE");
            instanceField.setAccessible(true);

            Field modifiersField = getModifiersField();
            modifiersField.setAccessible(true);
            modifiersField.setInt(instanceField, instanceField.getModifiers() & ~Modifier.FINAL);

            instanceField.set(null, provider);

        } catch (ClassNotFoundException e) {
            throw new IOException("Failed to initialize SniffySelectorProvider", e);
        } catch (NoSuchFieldException e) {
            throw new IOException("Failed to initialize SniffySelectorProvider", e);
        } catch (IllegalAccessException e) {
            throw new IOException("Failed to initialize SniffySelectorProvider", e);
        }
    }

    // TODO: move to ReflectionUtils
    private static Field getModifiersField() throws NoSuchFieldException {
        try {
            return Field.class.getDeclaredField("modifiers");
        } catch (NoSuchFieldException e) {
            try {
                Method getDeclaredFields0 = Class.class.getDeclaredMethod("getDeclaredFields0", boolean.class);
                getDeclaredFields0.setAccessible(true);
                Field[] fields = (Field[]) getDeclaredFields0.invoke(Field.class, false);
                for (Field field : fields) {
                    if ("modifiers".equals(field.getName())) {
                        return field;
                    }
                }
            } catch (ReflectiveOperationException ex) {
                e.addSuppressed(ex);
            }
            throw e;
        }
    }

    @Override
    public DatagramChannel openDatagramChannel() throws IOException {
        return delegate.openDatagramChannel();
    }

    @Override
    public DatagramChannel openDatagramChannel(ProtocolFamily family) throws IOException {
        return delegate.openDatagramChannel(family);
    }

    @Override
    public Pipe openPipe() throws IOException {
        return delegate.openPipe();
    }

    @Override
    public AbstractSelector openSelector() throws IOException {
        return delegate.openSelector();
    }

    @Override
    public ServerSocketChannel openServerSocketChannel() throws IOException {
        return delegate.openServerSocketChannel();
    }

    @Override
    public SocketChannel openSocketChannel() throws IOException {
        return new SniffySocketChannel(this, delegate.openSocketChannel());
    }

    @Override
    public Channel inheritedChannel() throws IOException {
        return delegate.inheritedChannel();
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public SocketChannel openSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return new SniffySocketChannel(
                    this,
                    (SocketChannel) method(SelectorProvider.class, "openSocketChannel", ProtocolFamily.class).invoke(delegate, family)
            );
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    public ServerSocketChannel openServerSocketChannel(ProtocolFamily family) throws IOException {
        try {
            return (ServerSocketChannel) method(SelectorProvider.class, "openServerSocketChannel", ProtocolFamily.class).invoke(delegate, family);
        } catch (NoSuchMethodException e) {
            throw ExceptionUtil.processException(e);
        } catch (IllegalAccessException e) {
            throw ExceptionUtil.processException(e);
        } catch (InvocationTargetException e) {
            throw ExceptionUtil.processException(e);
        }
    }

    private static Method method(Class<?> clazz, String methodName, Class<?>... argumentTypes) throws NoSuchMethodException {
        Method method = clazz.getDeclaredMethod(methodName, argumentTypes);
        method.setAccessible(true);
        return method;
    }

}
