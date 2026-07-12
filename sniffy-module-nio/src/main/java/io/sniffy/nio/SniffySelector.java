package io.sniffy.nio;

import io.sniffy.log.Polyglog;
import io.sniffy.log.PolyglogFactory;
import io.sniffy.util.*;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.spi.AbstractSelectableChannel;
import java.nio.channels.spi.AbstractSelector;
import java.nio.channels.spi.SelectorProvider;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Predicate;


/**
 * @since 3.1.7
 */
public class SniffySelector extends AbstractSelector {

    private static final Polyglog LOG = PolyglogFactory.log(SniffySelector.class);

    private final AbstractSelector delegate;

    /*
     * Public selection/close lock order is this selector, then selectedKeysWrapper.
     * registrationLifecycle owns link states only; it is never held while delegate or
     * wrapper-channel cleanup runs, so registration's channel -> selector calls cannot
     * deadlock with selector cleanup.
     */
    private final Object registrationLifecycle = new Object();
    private final Set<SelectionKeyLink> activeLinks = new HashSet<SelectionKeyLink>();
    private SelectorState selectorState = SelectorState.OPEN;
    private int registrationsInFlight;

    private volatile Set<SelectionKey> keysWrapper = null;
    private volatile Set<SelectionKey> selectedKeysWrapper = null;

    public SniffySelector(SelectorProvider provider, AbstractSelector delegate) {
        super(provider);
        if (delegate instanceof SniffySelector) {
            throw new IllegalArgumentException("SniffySelector requires an original selector delegate");
        }
        this.delegate = delegate;
        if (delegate != null) {
            this.keysWrapper = new SelectionKeySetView(delegate.keys(), false);
            this.selectedKeysWrapper = new SelectionKeySetView(delegate.selectedKeys(), true);
        }
        LOG.trace("Created new SniffySelector(" + provider + ", " + delegate + ") = " + this);
    }

    public SniffySelectionKey wrap(SelectionKey delegate, SniffySelector sniffySelector, SelectableChannel sniffySocketChannel) {
        if (sniffySelector != this) {
            throw new IllegalArgumentException("SelectionKey cannot be wrapped by a different SniffySelector");
        }
        return requireLinkedKey(delegate);
    }

    private SniffySelectionKey requireLinkedKey(SelectionKey delegate) {
        Object association = delegate.attachment();
        if (!(association instanceof SelectionKeyLink)) {
            String message = "Delegate SelectionKey attachment no longer contains Sniffy's internal link: " + association;
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        SelectionKeyLink link = (SelectionKeyLink) association;
        if (!link.belongsTo(this)) {
            String message = "Delegate SelectionKey is linked to a different Sniffy selector";
            LOG.error(message);
            throw new IllegalStateException(message);
        }
        return link.wrapper(delegate);
    }

    @SuppressWarnings("RedundantThrows")
    @Override
    protected void implCloseSelector() throws IOException {
        Throwable failure = null;
        synchronized (this) {
            Set<SelectionKey> selectedKeys = selectedKeysWrapper;
            synchronized (selectedKeys) {
                // Mark closing before touching the delegate: registering threads must roll back
                // instead of publishing a wrapper key after close has taken ownership.
                synchronized (registrationLifecycle) {
                    selectorState = SelectorState.CLOSING;
                }
                try {
                    delegate.close();
                } catch (Throwable e) {
                    failure = e;
                }
                try {
                    awaitRegistrations();
                    cleanupLinks(true);
                } catch (Throwable e) {
                    if (failure == null) {
                        failure = e;
                    } else {
                        failure.addSuppressed(e);
                    }
                } finally {
                    synchronized (registrationLifecycle) {
                        selectorState = SelectorState.CLOSED;
                    }
                }
            }
        }
        if (failure != null) {
            rethrowSelectionFailure(failure);
        }
    }

    private final class SelectionKeySetView extends AbstractSet<SelectionKey> {

        private final Set<SelectionKey> delegates;
        private final boolean removalAllowed;

        private SelectionKeySetView(Set<SelectionKey> delegates, boolean removalAllowed) {
            this.delegates = delegates;
            this.removalAllowed = removalAllowed;
        }

        @Override
        public Iterator<SelectionKey> iterator() {
            final Iterator<SelectionKey> iterator = delegates.iterator();
            return new Iterator<SelectionKey>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public SelectionKey next() {
                    return requireLinkedKey(iterator.next());
                }

                @Override
                public void remove() {
                    ensureRemovalAllowed();
                    iterator.remove();
                }
            };
        }

        @Override
        public int size() {
            return delegates.size();
        }

        @Override
        public boolean contains(Object candidate) {
            SelectionKey delegateKey = unwrap(candidate);
            return delegateKey != null && delegates.contains(delegateKey) && requireLinkedKey(delegateKey) == candidate;
        }

        @Override
        public boolean remove(Object candidate) {
            ensureRemovalAllowed();
            SelectionKey delegateKey = unwrap(candidate);
            return delegateKey != null
                    && delegates.contains(delegateKey)
                    && requireLinkedKey(delegateKey) == candidate
                    && delegates.remove(delegateKey);
        }

        @Override
        public boolean add(SelectionKey selectionKey) {
            throw new UnsupportedOperationException("Selector key views do not support addition");
        }

        @Override
        public boolean addAll(Collection<? extends SelectionKey> collection) {
            throw new UnsupportedOperationException("Selector key views do not support addition");
        }

        @Override
        public boolean removeAll(Collection<?> collection) {
            ensureRemovalAllowed();
            Objects.requireNonNull(collection, "collection");
            boolean modified = false;
            Iterator<SelectionKey> iterator = iterator();
            while (iterator.hasNext()) {
                if (collection.contains(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean retainAll(Collection<?> collection) {
            ensureRemovalAllowed();
            Objects.requireNonNull(collection, "collection");
            boolean modified = false;
            Iterator<SelectionKey> iterator = iterator();
            while (iterator.hasNext()) {
                if (!collection.contains(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public boolean removeIf(Predicate<? super SelectionKey> filter) {
            ensureRemovalAllowed();
            Objects.requireNonNull(filter, "filter");
            boolean modified = false;
            Iterator<SelectionKey> iterator = iterator();
            while (iterator.hasNext()) {
                if (filter.test(iterator.next())) {
                    iterator.remove();
                    modified = true;
                }
            }
            return modified;
        }

        @Override
        public void clear() {
            ensureRemovalAllowed();
            delegates.clear();
        }

        private SelectionKey unwrap(Object candidate) {
            if (!(candidate instanceof SniffySelectionKey)) {
                return null;
            }
            SniffySelectionKey wrapper = (SniffySelectionKey) candidate;
            return wrapper.selector() == SniffySelector.this ? wrapper.getDelegate() : null;
        }

        private void ensureRemovalAllowed() {
            if (!removalAllowed) {
                throw new UnsupportedOperationException("Selector.keys() does not support removal");
            }
        }
    }

    /** Registers the delegate channel while publishing the wrapper key through its attachment link. */
    @Override
    protected SelectionKey register(AbstractSelectableChannel ch, int ops, Object att) {
        SelectionKeyLink link = new SelectionKeyLink(this, ch, att);
        beginRegistration(link);
        try {
            AbstractSelectableChannel chDelegate = ch;
            if (ch instanceof SelectableChannelWrapper) {
                chDelegate = ((SelectableChannelWrapper<?>) ch).getDelegate();
            }
            registrationPoint(RegistrationPoint.BEFORE_DELEGATE_REGISTRATION, link);
            SelectionKey selectionKeyDelegate = chDelegate.register(delegate, ops, link);
            registrationPoint(RegistrationPoint.AFTER_DELEGATE_REGISTRATION, link);
            SniffySelectionKey wrapper = link.wrapper(selectionKeyDelegate);
            registrationPoint(RegistrationPoint.BEFORE_ACTIVATION, link);
            boolean activated = false;
            synchronized (registrationLifecycle) {
                if (selectorState == SelectorState.OPEN) {
                    link.activate();
                    activated = true;
                } else {
                    link.beginCleaning();
                }
            }
            if (activated) {
                registrationPoint(RegistrationPoint.ACTIVATION_FINALIZED, link);
                return wrapper;
            }
            java.nio.channels.ClosedSelectorException closed = new java.nio.channels.ClosedSelectorException();
            try {
                rollbackRegistration(link);
            } catch (Throwable cleanupFailure) {
                closed.addSuppressed(cleanupFailure);
            }
            throw closed;
        } catch (Throwable e) {
            Throwable failure = e;
            boolean cleanupRequired;
            synchronized (registrationLifecycle) {
                cleanupRequired = link.beginCleaning();
            }
            if (cleanupRequired) {
                try {
                    rollbackRegistration(link);
                } catch (Throwable cleanupFailure) {
                    failure.addSuppressed(cleanupFailure);
                }
            }
            throw ExceptionUtil.processException(failure);
        } finally {
            endRegistration();
        }
    }

    private void beginRegistration(SelectionKeyLink link) {
        synchronized (registrationLifecycle) {
            if (selectorState != SelectorState.OPEN) {
                throw new java.nio.channels.ClosedSelectorException();
            }
            activeLinks.add(link);
            registrationsInFlight++;
        }
    }

    private void endRegistration() {
        synchronized (registrationLifecycle) {
            registrationsInFlight--;
            registrationLifecycle.notifyAll();
        }
    }

    private void awaitRegistrations() {
        boolean interrupted = false;
        synchronized (registrationLifecycle) {
            while (registrationsInFlight != 0) {
                try {
                    registrationLifecycle.wait();
                } catch (InterruptedException e) {
                    interrupted = true;
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void rollbackRegistration(SelectionKeyLink link) {
        Throwable failure = null;
        SelectionKey delegateKey = link.delegate();
        if (delegateKey != null) {
            try {
                delegateKey.cancel();
            } catch (Throwable e) {
                failure = e;
            }
        }
        try {
            removeWrapperKey(link);
        } catch (Throwable e) {
            if (failure == null) failure = e; else failure.addSuppressed(e);
        }
        synchronized (registrationLifecycle) {
            if (failure == null) {
                link.removed();
                activeLinks.remove(link);
            } else {
                link.retryCleanup();
            }
        }
        if (failure != null) {
            throw ExceptionUtil.throwException(failure);
        }
    }

    @Override
    public Set<SelectionKey> keys() {
        if (!isOpen() || !delegate.isOpen()) throw new java.nio.channels.ClosedSelectorException();
        return keysWrapper;
    }

    @Override
    public Set<SelectionKey> selectedKeys() {
        if (!isOpen() || !delegate.isOpen()) throw new java.nio.channels.ClosedSelectorException();
        return selectedKeysWrapper;
    }

    @Override
    public int selectNow() throws IOException {
        return selectWithPublicLocks(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectNowDelegate();
            }
        });
    }

    @Override
    public int select(final long timeout) throws IOException {
        return selectWithPublicLocks(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectDelegate(timeout);
            }
        });
    }

    @Override
    public int select() throws IOException {
        return selectWithPublicLocks(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return selectDelegate();
            }
        });
    }

    @Override
    public Selector wakeup() {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            delegate.wakeup();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
        return this;
    }

    private int selectNowDelegate() throws IOException {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return delegate.selectNow();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private int selectDelegate(long timeout) throws IOException {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return delegate.select(timeout);
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private int selectDelegate() throws IOException {
        SniffySelectorProvider.enterDelegateSelectorConstruction();
        try {
            return delegate.select();
        } finally {
            SniffySelectorProvider.exitDelegateSelectorConstruction();
        }
    }

    private void cleanupLinks(boolean all) throws IOException {
        IOException failure = null;
        final List<SelectionKeyLink> links;
        synchronized (registrationLifecycle) {
            links = new ArrayList<SelectionKeyLink>(activeLinks);
        }
        for (SelectionKeyLink link : links) {
            SelectionKey delegateKey = link.delegate();
            SniffySelectionKey wrapper = link.existingWrapper();
            if (!all && (delegateKey == null || (wrapper == null ? delegateKey.isValid() : wrapper.isValid()))) {
                continue;
            }
            synchronized (registrationLifecycle) {
                if (link.state() != SelectionKeyLink.State.ACTIVE || !link.beginCleaning()) {
                    continue;
                }
            }
            try {
                removeWrapperKey(link);
                synchronized (registrationLifecycle) {
                    link.removed();
                    activeLinks.remove(link); // ownership ends only after channel cleanup succeeds
                }
            } catch (Throwable e) {
                synchronized (registrationLifecycle) {
                    link.retryCleanup();
                }
                LOG.error("Failed to reconcile a deregistered NIO selection key; cleanup will be retried", e);
                IOException ioe = new IOException("Failed to reconcile a deregistered NIO selection key", e);
                if (failure == null) {
                    failure = ioe;
                } else {
                    failure.addSuppressed(ioe);
                }
            }
        }
        if (failure != null) {
            throw failure;
        }
    }

    int activeLinkCount() {
        synchronized (registrationLifecycle) {
            return activeLinks.size();
        }
    }

    private void removeWrapperKey(SelectionKeyLink link) throws JdkNioAccess.JdkNioAccessException {
        SniffySelectionKey wrapper = link.existingWrapper();
        AbstractSelectableChannel channel = link.channel();
        if (wrapper != null && channel != null && channel.keyFor(this) == wrapper) {
            // AbstractSelectableChannel.removeKey takes keyLock. Registration already owns
            // keyLock until its outer register call publishes the returned wrapper key, so
            // this acquisition also waits for publication before removing it.
            JdkNioAccess.resolve().removeChannelKey(channel, wrapper);
        }
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(final Consumer<SelectionKey> action, final long timeout) throws IOException {
        if (timeout < 0) throw new IllegalArgumentException("Negative timeout");
        Objects.requireNonNull(action, "action");
        return selectWithPublicLocks(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return consumeSelectedKeys(action, new SelectionOperation() {
                    @Override
                    public int execute() throws Exception {
                        return selectDelegate(timeout);
                    }
                });
            }
        });
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int select(final Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action, "action");
        return selectWithPublicLocks(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return consumeSelectedKeys(action, new SelectionOperation() {
                    @Override
                    public int execute() throws Exception {
                        return selectDelegate();
                    }
                });
            }
        });
    }

    // Note: this method was absent in earlier JDKs so we cannot use @Override annotation
    //@Override
    @SuppressWarnings({"RedundantThrows", "Since15"})
    public int selectNow(final Consumer<SelectionKey> action) throws IOException {
        Objects.requireNonNull(action, "action");
        return selectWithPublicLocks(new SelectionOperation() {
            @Override
            public int execute() throws Exception {
                return consumeSelectedKeys(action, new SelectionOperation() {
                    @Override
                    public int execute() throws Exception {
                        return selectNowDelegate();
                    }
                });
            }
        });
    }

    private int selectWithPublicLocks(SelectionOperation operation) throws IOException {
        synchronized (this) {
            Set<SelectionKey> selectedKeys = selectedKeys();
            synchronized (selectedKeys) {
                return selectWithCleanup(operation);
            }
        }
    }

    private int consumeSelectedKeys(Consumer<SelectionKey> action, SelectionOperation selection) throws Exception {
        Set<SelectionKey> selected = selectedKeys();
        selected.clear();
        selection.execute(); // delegate monitors and provider scope are gone before application code runs
        List<SelectionKey> ready = new ArrayList<SelectionKey>(selected);
        for (SelectionKey key : ready) {
            action.accept(key);
            if (!isOpen()) {
                throw new java.nio.channels.ClosedSelectorException();
            }
        }
        return ready.size();
    }

    private int selectWithCleanup(SelectionOperation operation) throws IOException {
        Throwable primaryFailure = null;
        try {
            return operation.execute();
        } catch (Throwable e) {
            primaryFailure = e;
            rethrowSelectionFailure(primaryFailure);
            return 0;
        } finally {
            try {
                cleanupLinks(false);
            } catch (Throwable cleanupFailure) {
                if (primaryFailure != null) {
                    primaryFailure.addSuppressed(cleanupFailure);
                } else {
                    rethrowSelectionFailure(cleanupFailure);
                }
            }
        }
    }

    private static void rethrowSelectionFailure(Throwable failure) throws IOException {
        if (failure instanceof IOException) {
            throw (IOException) failure;
        }
        throw ExceptionUtil.throwException(failure);
    }

    private interface SelectionOperation {
        int execute() throws Exception;
    }

    private enum SelectorState {
        OPEN, CLOSING, CLOSED
    }

    enum RegistrationPoint {
        BEFORE_DELEGATE_REGISTRATION,
        AFTER_DELEGATE_REGISTRATION,
        BEFORE_ACTIVATION,
        ACTIVATION_FINALIZED
    }

    void registrationPoint(RegistrationPoint point, SelectionKeyLink link) {
        // Test subclasses provide deterministic race pause points; production has no work here.
    }

}
