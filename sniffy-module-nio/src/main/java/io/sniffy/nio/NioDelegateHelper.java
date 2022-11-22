package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.AssertUtil;
import io.sniffy.util.ExceptionUtil;

import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import static io.sniffy.reflection.Unsafe.$;

public class NioDelegateHelper {

    private static final Polyglog LOG = PolyglogFactory.log(NioDelegateHelper.class);

    public static void implCloseSelectableChannel(final AbstractSelectableChannel delegate) {

        try {

            boolean changed = false;

            synchronized ($(AbstractInterruptibleChannel.class).field("closedLock").getNotNullOrDefault(delegate, delegate)) {

                if ($(AbstractInterruptibleChannel.class).field("closed").isResolved()) {
                    changed = $(AbstractInterruptibleChannel.class).field("closed").compareAndSet(delegate, false, true);
                } else {
                    if ($(AbstractInterruptibleChannel.class).field("open").isResolved()) {
                        changed = $(AbstractInterruptibleChannel.class).field("open").compareAndSet(delegate, true, false);
                    } else {
                        AssertUtil.logAndThrowException(LOG, "Couldn't find neither closed nor open field in AbstractInterruptibleChannel", new IllegalStateException());
                    }
                }

            }

            if (changed) {
                $(AbstractSelectableChannel.class).method("implCloseSelectableChannel").invoke(delegate); // or selectable
            } else {
                if (AssertUtil.isTestingSniffy()) {
                    if ($(AbstractInterruptibleChannel.class).field("closed").isResolved()) {
                        if (!$(AbstractInterruptibleChannel.class).<Boolean>field("closed").get(delegate)) {
                            AssertUtil.logAndThrowException(LOG, "Failed to close delegate selector", new IllegalStateException());
                        }
                    } else {
                        if ($(AbstractInterruptibleChannel.class).field("open").isResolved()) {
                            if ($(AbstractInterruptibleChannel.class).<Boolean>field("open").get(delegate)) {
                                AssertUtil.logAndThrowException(LOG, "Failed to close delegate selector", new IllegalStateException());
                            }
                        } else {
                            AssertUtil.logAndThrowException(LOG, "Couldn't find neither closed nor open field in AbstractInterruptibleChannel", new IllegalStateException());
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.processException(e);
        }
    }

}
