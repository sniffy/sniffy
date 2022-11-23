package io.sniffy.nio;

import io.sniffy.reflection.field.NonStaticFieldRef;
import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.ObjectWrapper;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.*;

import static io.sniffy.reflection.Unsafe.$;
import static org.junit.Assert.*;

public class SniffySelectionKeyTest extends BaseSocketTest {

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
        classesToProcess.add(SelectionKey.class);

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

        Set<MethodDescriptor> sniffySelectionKeyMethods = new HashSet<MethodDescriptor>();

        for (Method method : SniffySelectionKey.class.getDeclaredMethods()) {
            sniffySelectionKeyMethods.add(new MethodDescriptor(method));
        }

        for (MethodDescriptor method : overrideableMethods) {
            if (!sniffySelectionKeyMethods.contains(method)) {
                fail("Method " + method + " is not overridden in SniffySelectionKey");
            }
        }

    }

    @Test
    public void testNoUnknownFields() throws Exception {

        Map<String, NonStaticFieldRef<? super SelectionKey,Object>> fieldsMap = $(SelectionKey.class).findNonStaticFields(null, true);


        fieldsMap.remove("attachment");
        fieldsMap.remove("attachmentUpdater");

        assertTrue(fieldsMap + " should be empty",fieldsMap.isEmpty());

    }

    @Test
    public void testSelectionKeys() throws Exception {

        SnifferSocketImplFactory.uninstall();
        SnifferSocketImplFactory.install();

        SniffySelectorProviderModule.initialize();
        SniffySelectorProvider.uninstall();
        SniffySelectorProvider.install();

        try {
            Selector selector = Selector.open();

            assertTrue(selector instanceof ObjectWrapper);
            //noinspection unchecked
            AbstractSelector delegateSelector = ((ObjectWrapper<AbstractSelector>) selector).getDelegate();

            try (SocketChannel socketChannel = SocketChannel.open()) {
                socketChannel.configureBlocking(false);
                socketChannel.connect(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

                Object attachment = new Object();

                SelectionKey sniffySelectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT, attachment);

                assertTrue(sniffySelectionKey instanceof ObjectWrapper);
                //noinspection unchecked
                SelectionKey delegate = ((ObjectWrapper<SelectionKey>) sniffySelectionKey).getDelegate();

                assertEquals(attachment, sniffySelectionKey.attachment());
                //assertEquals(attachment, delegate.attachment());

                Object newAttachment = new Object();
                assertEquals(attachment, sniffySelectionKey.attach(newAttachment));
                assertEquals(newAttachment, sniffySelectionKey.attachment());
                attachment = newAttachment;

                // start loop
                ByteBuffer responseBuffer = ByteBuffer.allocate(BaseSocketTest.RESPONSE.length);

                selectorLoop:
                while (true) {
                    // Wait for an event one of the registered channels
                    selector.select();

                    // Iterate over the set of keys for which events are available
                    Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
                    while (selectedKeys.hasNext()) {
                        SelectionKey key = selectedKeys.next();
                        assertEquals(attachment, key.attachment());
                        selectedKeys.remove();

                        if (!key.isValid()) {
                            continue;
                        }

                        // Check what event is available and deal with it
                        if (key.isConnectable()) {
                            SocketChannel channel = (SocketChannel) key.channel();

                            // Finish the connection. If the connection operation failed
                            // this will raise an IOException.
                            try {
                                channel.finishConnect();
                            } catch (IOException e) {
                                // Cancel the channel's registration with our selector
                                e.printStackTrace();
                                key.cancel();
                                break selectorLoop;
                            }

                            // Register an interest in writing on this channel
                            //key.interestOps(SelectionKey.OP_WRITE);
                            key.interestOps(0);
                            newAttachment = new Object();
                            SelectionKey sk = channel.register(selector, SelectionKey.OP_WRITE, newAttachment);
                            assertEquals(newAttachment, sk.attachment());
                            attachment = newAttachment;

                            assertTrue(sk.isValid());
                            /*sk.cancel(); // TODO: troubleshoot why is it failing - may be by design
                            assertFalse(sk.isValid());

                            sk = channel.register(selector, SelectionKey.OP_WRITE, attachment);
                            assertEquals(attachment, sk.attachment());*/

                        } else if (key.isReadable()) {

                            SocketChannel channel = (SocketChannel) key.channel();

                            // Attempt to read off the channel
                            int numRead;
                            try {
                                numRead = channel.read(responseBuffer);
                            } catch (IOException e) {
                                // The remote forcibly closed the connection, cancel
                                // the selection key and close the channel.
                                key.cancel();
                                channel.close();
                                break selectorLoop;
                            }

                            if (!responseBuffer.hasRemaining()) {
                                // Entire response consumed
                                key.channel().close();
                                key.cancel();
                                break selectorLoop;
                            }

                            if (numRead == -1) {
                                // Remote entity shut the socket down cleanly. Do the
                                // same from our end and cancel the channel.
                                key.channel().close();
                                key.cancel();
                                break selectorLoop;
                            }

                        } else if (key.isWritable()) {
                            SocketChannel channel = (SocketChannel) key.channel();

                            ByteBuffer requestBuffer = ByteBuffer.wrap(BaseSocketTest.REQUEST);

                            while (requestBuffer.remaining() > 0) {
                                channel.write(requestBuffer);
                            }

                            key.interestOps(0);

                            newAttachment = new Object();
                            SelectionKey sk = channel.register(selector, SelectionKey.OP_READ, newAttachment);
                            assertEquals(newAttachment, sk.attachment());
                            attachment = newAttachment;

                            assertTrue(sk.isValid());

                        }

                    }

                }

                Assert.assertArrayEquals(BaseSocketTest.RESPONSE, responseBuffer.array());

                // end loop

            }

            // test key cancel

            try (SocketChannel socketChannel = SocketChannel.open()) {
                socketChannel.configureBlocking(false);
                socketChannel.connect(new InetSocketAddress(BaseSocketTest.localhost, echoServerRule.getBoundPort()));

                Object attachment = new Object();

                SelectionKey sniffySelectionKey = socketChannel.register(selector, SelectionKey.OP_CONNECT, attachment);

                assertTrue(sniffySelectionKey instanceof ObjectWrapper);
                //noinspection unchecked
                SelectionKey delegate = ((ObjectWrapper<SelectionKey>) sniffySelectionKey).getDelegate();

                //noinspection unchecked
                SocketChannel delegateSocketChannel = ((ObjectWrapper<SocketChannel>) socketChannel).getDelegate();
                {
                    SelectionKey sk = socketChannel.keyFor(selector);
                    SelectionKey originalSk = delegateSocketChannel.keyFor(delegateSelector);

                    assertNotNull(sk);
                    assertNotNull(originalSk);
                }

                assertTrue(sniffySelectionKey.isValid());
                assertTrue(delegate.isValid());

                sniffySelectionKey.cancel();

                assertFalse(sniffySelectionKey.isValid());
                assertFalse(delegate.isValid());

                // TODO: add more assertions

                Set<SelectionKey> cancelledKeysInDelegate =
                        $(AbstractSelector.class).<Set<SelectionKey>>getNonStaticField("cancelledKeys").get(delegateSelector);
                Collection<SelectionKey> cancelledKeysInDelegateImpl = getCancelledKeysFromSelectorImpl(delegateSelector);

                assertTrue(cancelledKeysInDelegate.contains(delegate) || cancelledKeysInDelegateImpl.contains(delegate));

                selector.selectNow(); // trigger process deregister queue / AKA process cancelled keys

                cancelledKeysInDelegate =
                        $(AbstractSelector.class).<Set<SelectionKey>>getNonStaticField("cancelledKeys").get(delegateSelector);
                cancelledKeysInDelegateImpl = getCancelledKeysFromSelectorImpl(delegateSelector);

                assertTrue(cancelledKeysInDelegate.isEmpty());
                assertTrue(cancelledKeysInDelegateImpl.isEmpty());

                assertTrue(selector instanceof AbstractSelector);

                Set<SelectionKey> cancelledKeysInSniffySelector =
                        $(AbstractSelector.class).<Set<SelectionKey>>getNonStaticField("cancelledKeys").get((AbstractSelector) selector);

                assertTrue(null == cancelledKeysInSniffySelector || cancelledKeysInSniffySelector.isEmpty());

                // now test channel

                {
                    SelectionKey sk = socketChannel.keyFor(selector);
                    SelectionKey originalSk = delegateSocketChannel.keyFor(delegateSelector);

                    assertNull(sk);
                    assertNull(originalSk);
                }

                // not-mandatory test for SniffySocketChannel not containing canceled key

                {
                    int keyCount = $(AbstractSelectableChannel.class).<Integer>getNonStaticField("keyCount").get(delegateSocketChannel);

                    assertEquals(0, keyCount);
                }

                {
                    int keyCount = $(AbstractSelectableChannel.class).<Integer>getNonStaticField("keyCount").get(socketChannel);

                    assertEquals(0, keyCount);
                }

                // TODO: test cancelledKeys

            }

        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }
    }

    private static Collection<SelectionKey> getCancelledKeysFromSelectorImpl(AbstractSelector delegateSelector) {
        return $("sun.nio.ch.SelectorImpl", AbstractSelector.class).
                <Collection<SelectionKey>>tryGetNonStaticField("cancelledKeys").
                getOrDefault(delegateSelector, Collections.emptySet());
    }

}
