package sun.nio.ch;

import java.io.FileDescriptor;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;

public abstract class ServerSocketChannelDelegate extends ServerSocketChannel implements SelChImpl {

    private final SelChImpl delegate;

    private final static Method TRANSLATE_INTEREST_OPS_METHOD;
    private final static Method TRANSLATE_AND_SET_INTEREST_OPS_METHOD;
    private final static Method PARK_METHOD;
    private final static Method PARK_NANOS_METHOD;

    static {
        Method translateInterestOpsMethod;
        try {
            translateInterestOpsMethod = SelChImpl.class.getMethod("translateInterestOps", Integer.TYPE);
        } catch (Exception e) {
            translateInterestOpsMethod = null;
        }
        TRANSLATE_INTEREST_OPS_METHOD = translateInterestOpsMethod;

        Method translateAndSetInterestOpsMethod;
        try {
            // This method is absent in some JREs
            //noinspection JavaReflectionMemberAccess
            translateAndSetInterestOpsMethod = SelChImpl.class.getMethod("translateAndSetInterestOps", Integer.TYPE, SelectionKeyImpl.class);
        } catch (Exception e) {
            translateAndSetInterestOpsMethod = null;
        }
        TRANSLATE_AND_SET_INTEREST_OPS_METHOD = translateAndSetInterestOpsMethod;

        Method parkMethod;
        try {
            parkMethod = SelChImpl.class.getMethod("park", Integer.TYPE);
        } catch (Exception e) {
            parkMethod = null;
        }
        PARK_METHOD = parkMethod;

        Method parkNanosMethod;
        try {
            parkNanosMethod = SelChImpl.class.getMethod("park", Integer.TYPE, Long.TYPE);
        } catch (Exception e) {
            parkNanosMethod = null;
        }
        PARK_NANOS_METHOD = parkNanosMethod;
    }


    public ServerSocketChannelDelegate(SelectorProvider provider, Object delegate) {
        super(provider);
        this.delegate = (SelChImpl) delegate;
    }

    @Override
    public FileDescriptor getFD() {
        return delegate.getFD();
    }

    @Override
    public int getFDVal() {
        return delegate.getFDVal();
    }

    @Override
    public boolean translateAndUpdateReadyOps(int ops, SelectionKeyImpl sk) {
        return delegate.translateAndUpdateReadyOps(ops, sk);
    }

    @Override
    public boolean translateAndSetReadyOps(int ops, SelectionKeyImpl sk) {
        return delegate.translateAndSetReadyOps(ops, sk);
    }

    // @Override - this method in absent in some JREs
    public int translateInterestOps(int ops) {
        if (null != TRANSLATE_INTEREST_OPS_METHOD) {
            try {
                return (Integer) TRANSLATE_INTEREST_OPS_METHOD.invoke(delegate, ops);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    // @Override - this method in absent in some JREs
    public void translateAndSetInterestOps(int ops, SelectionKeyImpl sk) {
        if (null != TRANSLATE_AND_SET_INTEREST_OPS_METHOD) {
            try {
                TRANSLATE_AND_SET_INTEREST_OPS_METHOD.invoke(delegate, ops, sk);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void kill() throws IOException {
        delegate.kill();
    }

    // @Override - this method in absent in some JREs
    public void park(int event, long nanos) throws IOException {
        if (null != PARK_NANOS_METHOD) {
            try {
                PARK_NANOS_METHOD.invoke(delegate, event, nanos);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

    // @Override - this method in absent in some JREs
    public void park(int event) throws IOException {
        if (null != PARK_METHOD) {
            try {
                PARK_METHOD.invoke(delegate, event);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
    }

}
