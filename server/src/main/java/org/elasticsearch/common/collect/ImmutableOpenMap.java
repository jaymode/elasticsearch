/*
 * Licensed to Elasticsearch under one or more contributor
 * license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright
 * ownership. Elasticsearch licenses this file to you under
 * the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.elasticsearch.common.collect;

import com.carrotsearch.hppc.ObjectCollection;
import com.carrotsearch.hppc.ObjectContainer;
import com.carrotsearch.hppc.ObjectLookupContainer;
import com.carrotsearch.hppc.ObjectObjectAssociativeContainer;
import com.carrotsearch.hppc.ObjectObjectHashMap;
import com.carrotsearch.hppc.ObjectObjectMap;
import com.carrotsearch.hppc.cursors.ObjectCursor;
import com.carrotsearch.hppc.cursors.ObjectObjectCursor;
import com.carrotsearch.hppc.predicates.ObjectObjectPredicate;
import com.carrotsearch.hppc.predicates.ObjectPredicate;
import com.carrotsearch.hppc.procedures.ObjectObjectProcedure;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * An immutable map implementation based on open hash map.
 * <p>
 * Can be constructed using a {@link #builder()}, or using {@link #builder(ImmutableOpenMap)} (which is an optimized
 * option to copy over existing content and modify it).
 */
public final class ImmutableOpenMap<KType, VType> implements Iterable<ObjectObjectCursor<KType, VType>> {

    private final ObjectObjectHashMap<KType, VType> map;

    private ImmutableOpenMap(ObjectObjectHashMap<KType, VType> map) {
        this.map = map;
    }

    /**
     * @return Returns the value associated with the given key or the default value
     * for the key type, if the key is not associated with any value.
     * <p>
     * <b>Important note:</b> For primitive type values, the value returned for a non-existing
     * key may not be the default value of the primitive type (it may be any value previously
     * assigned to that slot).
     */
    public VType get(KType key) {
        return map.get(key);
    }

    /**
     * @return Returns the value associated with the given key or the provided default value if the
     * key is not associated with any value.
     */
    public VType getOrDefault(KType key, VType defaultValue) {
        return map.getOrDefault(key, defaultValue);
    }

    /**
     * Returns <code>true</code> if this container has an association to a value for
     * the given key.
     */
    public boolean containsKey(KType key) {
        return map.containsKey(key);
    }

    /**
     * @return Returns the current size (number of assigned keys) in the container.
     */
    public int size() {
        return map.size();
    }

    /**
     * @return Return <code>true</code> if this hash map contains no assigned keys.
     */
    public boolean isEmpty() {
        return map.isEmpty();
    }

    /**
     * Returns a cursor over the entries (key-value pairs) in this map. The iterator is
     * implemented as a cursor and it returns <b>the same cursor instance</b> on every
     * call to {@link Iterator#next()}. To read the current key and value use the cursor's
     * public fields. An example is shown below.
     * <pre>
     * for (IntShortCursor c : intShortMap)
     * {
     *     System.out.println(&quot;index=&quot; + c.index
     *       + &quot; key=&quot; + c.key
     *       + &quot; value=&quot; + c.value);
     * }
     * </pre>
     * <p>
     * The <code>index</code> field inside the cursor gives the internal index inside
     * the container's implementation. The interpretation of this index depends on
     * to the container.
     */
    @Override
    public Iterator<ObjectObjectCursor<KType, VType>> iterator() {
        return map.iterator();
    }

    /**
     * Returns a specialized view of the keys of this associated container.
     * The view additionally implements {@link ObjectLookupContainer}.
     */
    public ObjectLookupContainer<KType> keys() {
        return map.keys();
    }

    /**
     * Returns a direct iterator over the keys.
     */
    public Iterator<KType> keysIt() {
        final Iterator<ObjectCursor<KType>> iterator = map.keys().iterator();
        return new Iterator<KType>() {
            @Override
            public boolean hasNext() { return iterator.hasNext(); }

            @Override
            public KType next() {
                return iterator.next().value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * @return Returns a container with all values stored in this map.
     */
    public ObjectContainer<VType> values() {
        return map.values();
    }

    /**
     * Returns a direct iterator over the keys.
     */
    public Iterator<VType> valuesIt() {
        final Iterator<ObjectCursor<VType>> iterator = map.values().iterator();
        return new Iterator<VType>() {
            @Override
            public boolean hasNext() { return iterator.hasNext(); }

            @Override
            public VType next() {
                return iterator.next().value;
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    @Override
    public String toString() {
        return map.toString();
    }

    @Override
    @SuppressWarnings("rawtypes")
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ImmutableOpenMap that = (ImmutableOpenMap) o;

        if (!map.equals(that.map)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return map.hashCode();
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static final ImmutableOpenMap EMPTY = new ImmutableOpenMap(new ObjectObjectHashMap());

    @SuppressWarnings("unchecked")
    public static <KType, VType> ImmutableOpenMap<KType, VType> of() {
        return EMPTY;
    }

    /**
     * @return  An immutable copy of the given map
     */
    public static <KType, VType> ImmutableOpenMap<KType, VType> copyOf(ObjectObjectMap<KType, VType> map) {
        Builder<KType, VType> builder = builder();
        builder.putAll(map);
        return builder.build();
    }

    public static <KType, VType> Builder<KType, VType> builder() {
        return new Builder<>();
    }

    public static <KType, VType> Builder<KType, VType> builder(int size) {
        return new Builder<>(size);
    }

    public static <KType, VType> Builder<KType, VType> builder(ImmutableOpenMap<KType, VType> map) {
        return new Builder<>(map);
    }

    public static class Builder<KType, VType> implements ObjectObjectMap<KType, VType> {
        private ObjectObjectHashMap<KType, VType> map;

        @SuppressWarnings("unchecked")
        public Builder() {
            this(EMPTY);
        }

        public Builder(int size) {
            this.map = new ObjectObjectHashMap<>(size);
        }

        public Builder(ImmutableOpenMap<KType, VType> map) {
            this.map = map.map.clone();
        }

        /**
         * Builds a new instance of the
         */
        public ImmutableOpenMap<KType, VType> build() {
            ObjectObjectHashMap<KType, VType> map = this.map;
            this.map = null; // nullify the map, so any operation post build will fail! (hackish, but safest)
            return new ImmutableOpenMap<>(map);
        }



        /**
         * Puts all the entries in the map to the builder.
         */
        public Builder<KType, VType> putAll(Map<KType, VType> map) {
            for (Map.Entry<KType, VType> entry : map.entrySet()) {
                this.map.put(entry.getKey(), entry.getValue());
            }
            return this;
        }

        /**
         * A put operation that can be used in the fluent pattern.
         */
        public Builder<KType, VType> fPut(KType key, VType value) {
            map.put(key, value);
            return this;
        }

        @Override
        public VType put(KType key, VType value) {
            return map.put(key, value);
        }

        @Override
        public VType get(KType key) {
            return map.get(key);
        }

        @Override
        public VType getOrDefault(KType kType, VType vType) {
            return map.getOrDefault(kType, vType);
        }

        @Override
        public int putAll(ObjectObjectAssociativeContainer<? extends KType, ? extends VType> container) {
            return map.putAll(container);
        }

        @Override
        public int putAll(Iterable<? extends ObjectObjectCursor<? extends KType, ? extends VType>> iterable) {
            return map.putAll(iterable);
        }

        /**
         * Remove that can be used in the fluent pattern.
         */
        public Builder<KType, VType> fRemove(KType key) {
            map.remove(key);
            return this;
        }

        @Override
        public VType remove(KType key) {
            return map.remove(key);
        }

        @Override
        public Iterator<ObjectObjectCursor<KType, VType>> iterator() {
            return map.iterator();
        }

        @Override
        public boolean containsKey(KType key) {
            return map.containsKey(key);
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        public int removeAll(ObjectContainer<? super KType> container) {
            return map.removeAll(container);
        }

        @Override
        public int removeAll(ObjectPredicate<? super KType> predicate) {
            return map.removeAll(predicate);
        }

        @Override
        public <T extends ObjectObjectProcedure<? super KType, ? super VType>> T forEach(T procedure) {
            return map.forEach(procedure);
        }

        @Override
        public void clear() {
            map.clear();
        }

        @Override
        public ObjectCollection<KType> keys() {
            return map.keys();
        }

        @Override
        public ObjectContainer<VType> values() {
            return map.values();
        }

        @SuppressWarnings("unchecked")
        public <K, V> Builder<K, V> cast() {
            return (Builder) this;
        }

        @Override
        public int removeAll(ObjectObjectPredicate<? super KType, ? super VType> predicate) {
            return map.removeAll(predicate);
        }

        @Override
        public <T extends ObjectObjectPredicate<? super KType, ? super VType>> T forEach(T predicate) {
            return map.forEach(predicate);
        }

        @Override
        public int indexOf(KType key) {
            return map.indexOf(key);
        }

        @Override
        public boolean indexExists(int index) {
            return map.indexExists(index);
        }

        @Override
        public VType indexGet(int index) {
            return map.indexGet(index);
        }

        @Override
        public VType indexReplace(int index, VType newValue) {
            return map.indexReplace(index, newValue);
        }

        @Override
        public void indexInsert(int index, KType key, VType value) {
            map.indexInsert(index, key, value);
        }

        @Override
        public void release() {
            map.release();
        }

        @Override
        public String visualizeKeyDistribution(int characters) {
            return map.visualizeKeyDistribution(characters);
        }
    }

    /**
     * @return a wrapper around this {@link ImmutableOpenMap} that exposes the contents as a
     * {@link Map}
     */
    public Map<KType, VType> asMap() {
        return new ImmutableOpenMapJdkMap<>(this);
    }

    /**
     * @return a map with the result of the computation from the remapping function. If the
     * computation returns the same value as the existing value and the key is in the map, the
     * same instance of the map will be returned.
     */
    public ImmutableOpenMap<KType, VType> compute(KType key, BiFunction<? super KType, ? super VType, ? extends VType> remappingFunction) {
        Objects.requireNonNull(remappingFunction);
        VType oldValue = get(key);

        VType newValue = remappingFunction.apply(key, oldValue);
        if (newValue == oldValue) {
            if (containsKey(key)) {
                return this;
            } else {
                return copyAndModify(mapCopy -> mapCopy.put(key, newValue));
            }
        }


        final Consumer<ObjectObjectHashMap<KType, VType>> modifier;
        if (newValue == null && containsKey(key)) {
            modifier = mapCopy -> mapCopy.remove(key);
        } else {
            modifier = mapCopy -> mapCopy.put(key, newValue);
        }
        return copyAndModify(modifier);
    }

    private ImmutableOpenMap<KType, VType> copyAndModify(Consumer<ObjectObjectHashMap<KType, VType>> modifier) {
        ObjectObjectHashMap<KType, VType> copiedMap = new ObjectObjectHashMap<>(map);
        modifier.accept(copiedMap);
        return new ImmutableOpenMap<>(copiedMap);
    }

    private static class ImmutableOpenMapJdkMap<KType, VType> implements Map<KType, VType> {

        private final ImmutableOpenMap<KType, VType> map;

        private ImmutableOpenMapJdkMap(ImmutableOpenMap<KType, VType> map) {
            this.map = map;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean containsKey(Object key) {
            return map.containsKey((KType) key);
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean containsValue(Object value) {
            return map.values().contains(((VType) value));
        }

        @Override
        @SuppressWarnings("unchecked")
        public VType get(Object key) {
            return map.get((KType) key);
        }

        @Override
        public VType put(KType key, VType value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public VType remove(Object key) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void putAll(Map<? extends KType, ? extends VType> m) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Set<KType> keySet() {
            return new ImmutableObjectContainerSet<>(map.keys());
        }

        @Override
        public Collection<VType> values() {
            return new ImmutableObjectContainerSet<>(map.values());
        }

        @Override
        public Set<Entry<KType, VType>> entrySet() {
            return new ImmutableOpenMapEntrySet<>(map);
        }
    }

    private static class ImmutableOpenMapEntrySet<KType, VType> implements Set<Entry<KType, VType>> {

        private final ImmutableOpenMap<KType, VType> map;

        private ImmutableOpenMapEntrySet(ImmutableOpenMap<KType, VType> map) {
            this.map = map;
        }

        @Override
        public int size() {
            return map.size();
        }

        @Override
        public boolean isEmpty() {
            return map.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object o) {
            if (o instanceof Entry) {
                KType key = ((Entry<KType, VType>) o).getKey();
                if (map.containsKey(key)) {
                    VType value = ((Entry<KType, VType>) o).getValue();
                    return Objects.equals(map.get(key), value);
                }
            }
            return false;
        }

        @Override
        public Iterator<Entry<KType, VType>> iterator() {
            final Iterator<ObjectObjectCursor<KType, VType>> iterator = map.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public Entry<KType, VType> next() {
                    final ObjectObjectCursor<KType, VType> cursor = iterator.next();
                    return new Entry<>() {
                        @Override
                        public KType getKey() {
                            return cursor.key;
                        }

                        @Override
                        public VType getValue() {
                            return cursor.value;
                        }

                        @Override
                        public VType setValue(VType value) {
                            throw new UnsupportedOperationException();
                        }
                    };
                }
            };
        }

        @Override
        public Object[] toArray() {
            final Object[] array = new Object[size()];
            final Iterator<Entry<KType, VType>> iterator = iterator();
            int i = 0;
            while (iterator.hasNext()) {
                if (i >= array.length) {
                    throw new IllegalStateException("size() is less than number of values returned using iterator");
                }
                array[i++] = iterator.next();
            }
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final int size = size();
            final T[] array = a.length >= size ? a :
                (T[])java.lang.reflect.Array
                    .newInstance(a.getClass().getComponentType(), size);
            final Iterator<Entry<KType, VType>> iterator = iterator();
            int i = 0;
            while (iterator.hasNext()) {
                if (i >= array.length) {
                    throw new IllegalStateException("size() is less than number of values returned using iterator");
                }
                array[i++] = (T) iterator.next();
            }
            return array;
        }

        @Override
        public boolean add(Entry<KType, VType> kTypeVTypeEntry) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.stream().allMatch(this::contains);
        }

        @Override
        public boolean addAll(Collection<? extends Entry<KType, VType>> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException();
        }
    }

    private static class ImmutableObjectContainerSet<IType> implements Set<IType> {

        private final ObjectContainer<IType> container;

        private ImmutableObjectContainerSet(ObjectContainer<IType> container) {
            this.container = container;
        }

        @Override
        public int size() {
            return container.size();
        }

        @Override
        public boolean isEmpty() {
            return container.isEmpty();
        }

        @Override
        @SuppressWarnings("unchecked")
        public boolean contains(Object o) {
            return container.contains((IType) o);
        }

        @Override
        public Iterator<IType> iterator() {
            final Iterator<ObjectCursor<IType>> iterator = container.iterator();
            return new Iterator<>() {
                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public IType next() {
                    return iterator.next().value;
                }
            };
        }

        @Override
        public Object[] toArray() {
            final Object[] array = new Object[size()];
            final Iterator<IType> iterator = iterator();
            int i = 0;
            while (iterator.hasNext()) {
                if (i >= array.length) {
                    throw new IllegalStateException("size() is less than number of values returned using iterator");
                }
                array[i++] = iterator.next();
            }
            return array;
        }

        @Override
        @SuppressWarnings("unchecked")
        public <T> T[] toArray(T[] a) {
            final int size = size();
            final T[] array = a.length >= size ? a :
                (T[])java.lang.reflect.Array
                    .newInstance(a.getClass().getComponentType(), size);
            final Iterator<IType> iterator = iterator();
            int i = 0;
            while (iterator.hasNext()) {
                if (i >= array.length) {
                    throw new IllegalStateException("size() is less than number of values returned using iterator");
                }
                array[i++] = (T) iterator.next();
            }
            return array;
        }

        @Override
        public boolean add(IType value) {
            throw new UnsupportedOperationException("cannot add value to immutable set");
        }

        @Override
        public boolean remove(Object o) {
            throw new UnsupportedOperationException("cannot remove value from immutable set");
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.stream().allMatch(this::contains);
        }

        @Override
        public boolean addAll(Collection<? extends IType> c) {
            throw new UnsupportedOperationException("cannot add values to immutable set");
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            throw new UnsupportedOperationException("cannot retain values in immutable set");
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            throw new UnsupportedOperationException("cannot remove values from immutable set");
        }

        @Override
        public void clear() {
            throw new UnsupportedOperationException("cannot clear immutable set");
        }
    }
}
