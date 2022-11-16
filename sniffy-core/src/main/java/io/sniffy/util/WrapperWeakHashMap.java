package io.sniffy.util;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;
import java.util.WeakHashMap;

@SuppressWarnings({"Convert2Diamond", "ConstantConditions"})
public class WrapperWeakHashMap<K, V extends ObjectWrapper<? extends K>> { // V extends K and ObjectWrapper<K>

    @SuppressWarnings("EqualsReplaceableByObjectsCall")
    private static class Entry<T> {

        private final T object;

        public Entry(T object) {
            this.object = object;
        }

        public T getValue() {
            return object;
        }

        @Override
        public int hashCode() {
            if (null == object) return 0;
            if (object instanceof ObjectWrapper) {
                Object delegate = ((ObjectWrapper<?>) object).getDelegate();
                return null == delegate ? 0 : delegate.hashCode();
            } else {
                return object.hashCode();
            }
        }

        @Override
        public boolean equals(Object that) {
            if (null == that) return false;
            if (that instanceof Entry<?>) {

                Object obj1 = this.object;
                if (obj1 instanceof ObjectWrapper) {
                    obj1 = ((ObjectWrapper<?>) obj1).getDelegate();
                }

                Object obj2 = ((Entry<?>) that).object;
                if (obj2 instanceof ObjectWrapper) {
                    obj2 = ((ObjectWrapper<?>) obj2).getDelegate();
                }

                return (obj1 == obj2) || (obj1 != null && obj1.equals(obj2));
            } else {
                return false;
            }
        }

    }

    // V.getDelegate().equals(K) == true
    private final WeakHashMap<Entry<V>, WeakReference<Entry<V>>> weakHashMap
            = new WeakHashMap<Entry<V>, WeakReference<Entry<V>>>();

    public int size() {
        int size;
        try {
            size = weakHashMap.size();
        } catch (Exception e) {
            synchronized (this) {
                size = weakHashMap.size();
            }
        }
        return size;
    }

    public V get(K key) {
        return getOrWrap(key, null);
    }

    public V put(V value) {
        Entry<V> entry = new Entry<V>(value);
        synchronized (this) {
            WeakReference<Entry<V>> vWeakReference = weakHashMap.put(entry, new WeakReference<Entry<V>>(entry));
            return vWeakReference == null ? null :
                    (vWeakReference.get() == null ? null :
                            vWeakReference.get().object);
        }
    }

    public Set<V> values() {
        Set<V> result = new HashSet<V>();
        synchronized (this) {
            for (WeakReference<Entry<V>> vWeakReference : weakHashMap.values()) {
                Entry<V> entry = null == vWeakReference ? null : vWeakReference.get();
                if (null != entry && null != entry.object) {
                    result.add(entry.object);
                }
            }
        }
        return result;
    }

    public V getOrWrap(K key, WrapperFactory<K, V> wrapperFactory) {

        WeakReference<Entry<V>> valueWeakReference;
        try {
            //noinspection SuspiciousMethodCalls
            valueWeakReference = weakHashMap.get(new Entry<K>(key));
        } catch (Exception e) {
            synchronized (this) {
                //noinspection SuspiciousMethodCalls
                valueWeakReference = weakHashMap.get(new Entry<K>(key));
            }
        }

        if (null == valueWeakReference) {
            synchronized (this) {
                //noinspection SuspiciousMethodCalls
                valueWeakReference = weakHashMap.get(new Entry<K>(key));
                if (null == valueWeakReference) {
                    if (null == wrapperFactory) {
                        return null;
                    } else {
                        Entry<V> entry = new Entry<V>(wrapperFactory.wrap(key));
                        valueWeakReference = new WeakReference<Entry<V>>(entry);
                        weakHashMap.put(entry, valueWeakReference);
                    }
                }
            }
        }

        return valueWeakReference.get() == null ? null : valueWeakReference.get().object;

    }

}
