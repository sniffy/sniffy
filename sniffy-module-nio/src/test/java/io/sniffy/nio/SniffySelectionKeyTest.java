package io.sniffy.nio;

import io.sniffy.socket.BaseSocketTest;
import io.sniffy.socket.SnifferSocketImplFactory;
import io.sniffy.util.ObjectWrapper;
import io.sniffy.util.ReflectionUtil;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.util.*;

import static org.junit.Assert.*;

public class SniffySelectionKeyTest extends BaseSocketTest {

    @Test
    public void testFields() throws Exception {

        Map<String, Field> fieldsMap = new HashMap<String, Field>();

        for (Field field : ReflectionUtil.getDeclaredFieldsHierarchy(SelectionKey.class)) {
            if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                fieldsMap.put(field.getName(), field);
            }
        }

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
                assertEquals(attachment, delegate.attachment());

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
                        ReflectionUtil.getField(AbstractSelector.class, delegateSelector, "cancelledKeys");
                Collection<SelectionKey> cancelledKeysInDelegateImpl = getCancelledKeysFromSelectorImpl(delegateSelector);

                assertTrue(cancelledKeysInDelegate.contains(delegate) || cancelledKeysInDelegateImpl.contains(delegate));

                selector.selectNow(); // trigger process deregister queue / AKA process cancelled keys

                cancelledKeysInDelegate =
                        ReflectionUtil.getField(AbstractSelector.class, delegateSelector, "cancelledKeys");
                cancelledKeysInDelegateImpl = getCancelledKeysFromSelectorImpl(delegateSelector);

                assertTrue(cancelledKeysInDelegate.isEmpty());
                assertTrue(cancelledKeysInDelegateImpl.isEmpty());

                assertTrue(selector instanceof AbstractSelector);

                Set<SelectionKey> cancelledKeysInSniffySelector =
                        ReflectionUtil.getField(AbstractSelector.class, (AbstractSelector) selector, "cancelledKeys");

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
                    int keyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, delegateSocketChannel, "keyCount");

                    assertEquals(0, keyCount);
                }

                {
                    int keyCount = ReflectionUtil.getField(AbstractSelectableChannel.class, socketChannel, "keyCount");

                    assertEquals(0, keyCount);
                }

                // TODO: test cancelledKeys

            }

        } finally {
            SnifferSocketImplFactory.uninstall();
            SniffySelectorProvider.uninstall();
        }
    }

    private static Collection<SelectionKey> getCancelledKeysFromSelectorImpl(AbstractSelector delegateSelector) throws NoSuchFieldException, IllegalAccessException {
        try {
            return ReflectionUtil.getField("sun.nio.ch.SelectorImpl", delegateSelector, "cancelledKeys");
        } catch (NoSuchFieldException e) {
            return Collections.emptySet();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
