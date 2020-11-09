package io.sniffy.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.Set;

public class SetWrapper<W extends E, E> extends AbstractSet<E> implements Set<E> {

    private final Set<E> delegateSet;
    private final WrapperFactory<E, W> wrapperFactory;

    public SetWrapper(Set<E> delegateSet, WrapperFactory<E, W> wrapperFactory) {
        this.delegateSet = delegateSet;
        this.wrapperFactory = wrapperFactory;
    }

    @Override
    public int size() {
        return delegateSet.size();
    }

    @Override
    public Iterator<E> iterator() {
        return new IteratorWrapper();
    }

    private class IteratorWrapper implements Iterator<E> {

        private final Iterator<E> delegateIterator = delegateSet.iterator();

        @Override
        public boolean hasNext() {
            return delegateIterator.hasNext();
        }

        @Override
        public W next() {
            return wrapperFactory.wrap(delegateIterator.next());
        }

        @Override
        public void remove() {
            delegateIterator.remove();
        }

    }

}
