package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.clazz.ClassRef;
import io.sniffy.reflection.field.UnresolvedNonStaticFieldRef;
import io.sniffy.reflection.method.UnresolvedNonStaticMethodRef;
import io.sniffy.util.ExceptionUtil;

import java.nio.channels.spi.AbstractInterruptibleChannel;
import java.nio.channels.spi.AbstractSelectableChannel;

import static io.sniffy.reflection.Unsafe.$;

public class NioDelegateHelper {

    private static final Polyglog LOG = PolyglogFactory.log(NioDelegateHelper.class);
    public static final ClassRef<AbstractInterruptibleChannel> ABSTRACT_INTERRUPTIBLE_CHANNEL = $(AbstractInterruptibleChannel.class);
    public static final UnresolvedNonStaticFieldRef<AbstractInterruptibleChannel, Boolean> OPEN =
            ABSTRACT_INTERRUPTIBLE_CHANNEL.getNonStaticField("open");
    public static final UnresolvedNonStaticFieldRef<AbstractInterruptibleChannel, Boolean> CLOSED =
            ABSTRACT_INTERRUPTIBLE_CHANNEL.getNonStaticField("closed");
    public static final UnresolvedNonStaticFieldRef<AbstractInterruptibleChannel, Object> CLOSED_LOCK =
            ABSTRACT_INTERRUPTIBLE_CHANNEL.getNonStaticField("closeLock");
    public static final ClassRef<AbstractSelectableChannel> ABSTRACT_SELECTABLE_CHANNEL = $(AbstractSelectableChannel.class);
    public static final UnresolvedNonStaticMethodRef<AbstractSelectableChannel> IMPL_CLOSE_SELECTABLE_CHANNEL =
            ABSTRACT_SELECTABLE_CHANNEL.getNonStaticMethod("implCloseSelectableChannel");

    public static void implCloseSelectableChannel(final AbstractSelectableChannel delegate) {

        try {

            boolean changed = false;

            assert CLOSED_LOCK.isResolved() : CLOSED_LOCK.getResolveException();

            synchronized (CLOSED_LOCK.getNotNullOrDefault(delegate, delegate)) {

                if (CLOSED.isResolved()) {
                    changed = CLOSED.compareAndSet(delegate, false, true);
                } else {
                    if (OPEN.isResolved()) {
                        changed = OPEN.compareAndSet(delegate, true, false);
                    } else {
                        assert false : "Couldn't find neither closed nor open field in AbstractInterruptibleChannel";
                    }
                }

            }

            if (changed) {
                IMPL_CLOSE_SELECTABLE_CHANNEL.invoke(delegate);
            }

            assert CLOSED.getOrDefault(delegate, false) || !OPEN.getOrDefault(delegate, true);

        } catch (Exception e) {
            LOG.error(e);
            throw ExceptionUtil.processException(e);
        }
    }

}
