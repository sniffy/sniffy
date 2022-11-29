package io.sniffy.tls;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.reflection.UnresolvedRefException;
import io.sniffy.reflection.UnsafeInvocationException;
import io.sniffy.util.StackTraceExtractor;
import sun.security.jca.ProviderList;
import sun.security.jca.Providers;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLContextSpi;
import java.security.Provider;
import java.util.Map;

import static io.sniffy.reflection.Unsafe.$;
import static io.sniffy.tls.SniffySecurityUtil.SSLCONTEXT;
import static io.sniffy.tls.SniffyThreadLocalProviderList.wrapProviderList;

// TODO: fix synchronization
public class ProvidersWrapper extends ThreadLocal<ProviderList> {

    private static final Polyglog LOG = PolyglogFactory.log(ProvidersWrapper.class);

    private static final ThreadLocal<ProviderList> threadLists =
            new InheritableThreadLocal<ProviderList>();

    // number of threads currently using thread-local provider lists
    // tracked to allow an optimization if == 0
    private static volatile int threadListsUsed;

    // current system-wide provider list
    // Note volatile immutable object, so no synchronization needed.
    private static volatile ProviderList providerList;

    public ProvidersWrapper() throws UnresolvedRefException, UnsafeInvocationException {
        providerList = $(Providers.class).<ProviderList>getStaticField("providerList").get();
    }

    @Override
    public synchronized ProviderList get() {
        if (StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "beginThreadProviderList")) {
            threadListsUsed++;
            return getThreadProviderList();
        } else if (
                StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "getThreadProviderList") &&
                !StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "getProviderList") &&
                !StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "setProviderList") &&
                !StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "getFullProviderList")
        ) {
            return getThreadProviderList();
        } else {
            ProviderList list = getThreadProviderList();
            if (list == null) {
                list = getSystemProviderList();
            }
            assert null != list;
            return list;
        }
    }

    @Override
    public synchronized void set(ProviderList newList) {
        LOG.trace("Intercepted Providers.changeThreadProviderList() call with " + newList);
        if (StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "beginThreadProviderList")) {
            changeThreadProviderList(newList);
        } else if (StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "endThreadProviderList")) {
            changeThreadProviderList(newList);
            threadListsUsed--;
        } else {
            if (getThreadProviderList() == null) {
                setSystemProviderList(newList);
            } else {
                changeThreadProviderList(newList);
            }
        }

    }

    @Override
    public synchronized void remove() {
        if (StackTraceExtractor.hasClassAndMethodInStackTrace(Providers.class.getName(), "endThreadProviderList")) {
            threadLists.remove();
            threadListsUsed--;
        }
    }

    private static ProviderList getSystemProviderList() {
        return providerList;
    }

    private static void setSystemProviderList(ProviderList list) {
        providerList = processProviderList(list);
    }

    private static ProviderList getThreadProviderList() {
        // avoid accessing the threadlocal if none are currently in use
        // (first use of ThreadLocal.get() for a Thread allocates a Map)
        if (threadListsUsed == 0) {
            return null;
        }
        return threadLists.get();
    }

    // Change the thread local provider list. Use only if the current thread
    // is already using a thread local list and you want to change it in place.
    // In other cases, use the begin/endThreadProviderList() methods.
    private static void changeThreadProviderList(ProviderList list) {
        threadLists.set(processProviderList(list));
    }

    private static ProviderList processProviderList(ProviderList value) {
        try {

            LOG.info("Wrapping ProviderList " + value);

            Map.Entry<ProviderList, SniffySSLContextSpiProvider> tuple = wrapProviderList(value);

            LOG.info("Wrapped ProviderList " + tuple.getKey());
            LOG.info("First SniffySSLContextSpiProvider with default SSLContextSPI is " + tuple.getValue());

            if (value == tuple.getKey()) {
                LOG.info("ProviderList wasn't changed - setting original ProviderList");
            } else {
                value = tuple.getKey();
                LOG.info("ProviderList was changed - setting wrapped list");

                SniffySSLContextSpiProvider firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi = tuple.getValue();

                if (null != firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi) {
                    Provider.Service defaultSniffySSLContextSpiProviderService =
                            firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi.getService(SSLCONTEXT, "Default");
                    if (null != defaultSniffySSLContextSpiProviderService) {
                        try {
                            SniffySSLContext defaultSniffySSLContext = new SniffySSLContext(
                                    (SSLContextSpi) defaultSniffySSLContextSpiProviderService.newInstance(null),
                                    firstSniffySSLContextSpiProviderWithDefaultSSLContextSpi,
                                    "Default"
                            );
                            LOG.info("Setting SSLContext.default to " + defaultSniffySSLContext);
                            // TODO: why do we do it here? we already doing it in SniffySecurityUtil
                            SSLContext.setDefault(defaultSniffySSLContext);

                        } catch (Throwable e) {
                            // TODO: do the same in other dangerous places
                            LOG.error(e);
                        }
                    }
                }

            }

            return value;

        } catch (UnresolvedRefException e) {
            throw new RuntimeException(e);
        } catch (UnsafeInvocationException e) {
            throw new RuntimeException(e);
        }
    }

}
