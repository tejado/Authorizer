package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonArray;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * A collection that contains BoxJsonObject items
 *
 * @param <E> the type of elements in this partial collection.
 */
public class BoxArray<E extends BoxJsonObject> implements Collection<E> {

    protected final Collection<E> collection = new ArrayList<E>();

    public BoxArray() {
        super();
    }

    public String toJson() {
        JsonArray array = new JsonArray();
        for (int i = 0; i < size(); i++) {
            array.add(get(i).toJsonObject());
        }
        return array.toString();
    }

    @Override
    public boolean add(E e) {
        return this.collection.add(e);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return this.collection.addAll(c);
    }

    @Override
    public void clear() {
        this.collection.clear();
    }

    @Override
    public boolean contains(Object o) {
        return this.collection.contains(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return this.collection.containsAll(c);
    }

    @Override
    public boolean equals(Object o) {
        return this.collection.equals(o);
    }

    @Override
    public int hashCode() {
        return this.collection.hashCode();
    }

    @Override
    public boolean isEmpty() {
        return this.collection.isEmpty();
    }

    @Override
    public Iterator<E> iterator() {
        return this.collection.iterator();
    }

    @Override
    public boolean remove(Object o) {
        return this.collection.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return this.collection.removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return this.collection.retainAll(c);
    }

    @Override
    public int size() {
        return this.collection.size();
    }

    @Override
    public Object[] toArray() {
        return this.collection.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return this.collection.toArray(a);
    }

    public E get(int index) {
        if (collection instanceof List) {
            return (E) ((List) collection).get(index);
        }
        if (index < 0) {
            throw new IndexOutOfBoundsException();
        }
        Iterator<E> iterator = iterator();
        int i = 0;
        while (iterator.hasNext()) {
            if (index == i) {
                return iterator.next();
            }
            iterator.next();
        }
        throw new IndexOutOfBoundsException();
    }
}
