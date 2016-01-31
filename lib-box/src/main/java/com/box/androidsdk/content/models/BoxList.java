package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A collection that contains a subset of items that are a part of a larger collection. The items within a partial collection begin at an offset within the full
 * collection and end at a specified limit. Note that the actual size of a partial collection may be less than its limit since the limit only specifies the
 * maximum size. For example, if there's a full collection with a size of 3, then a partial collection with offset 0 and limit 3 would be equal to a partial
 * collection with offset 0 and limit 100.
 *
 * @param <E> the type of elements in this partial collection.
 */
public class BoxList<E extends BoxJsonObject> extends BoxJsonObject implements Collection<E> {

    private static final long serialVersionUID = 8036181424029520417L;
    protected final Collection<E> collection = new ArrayList<E>(){
        @Override
        public boolean add(E object) {
            addCollectionToProperties();
            return super.add(object);
        }

        @Override
        public void add(int index, E object) {
            addCollectionToProperties();
            super.add(index, object);
        }

        @Override
        public boolean addAll(Collection<? extends E> collection) {
            addCollectionToProperties();
            return super.addAll(collection);
        }

        @Override
        public boolean addAll(int index, Collection<? extends E> collection) {
            addCollectionToProperties();
            return super.addAll(index, collection);
        }
    };

    /**
     * Add the collection to the properties map if this has not been added already.
     */
    protected void addCollectionToProperties(){
        if (! collectionInProperties){
            mProperties.put(FIELD_ENTRIES, collection);
            collectionInProperties = true;
        }
    }

    protected transient boolean collectionInProperties = false;

    public static final String FIELD_ORDER = "order";

    public BoxList() {
        super();
    }

    /**
     * Constructs a BoxList with the provided map values.
     *
     * @param map map of keys and values of the object.
     */
    public BoxList(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets the offset within the full collection where this collection's items begin.
     *
     * @return the offset within the full collection where this collection's items begin.
     */
    public Long offset() {
        return (Long) mProperties.get(FIELD_OFFSET);
    }

    /**
     * Gets the maximum number of items within the full collection that begin at {@link #offset}.
     *
     * @return the maximum number of items within the full collection that begin at the offset.
     */
    public Long limit() {
        return (Long) mProperties.get(FIELD_LIMIT);
    }

    /**
     * Gets the size of the full collection that this partial collection is based off of.
     *
     * @return the size of the full collection that this partial collection is based off of.
     */
    public Long fullSize() {
        return (Long) mProperties.get(FIELD_TOTAL_COUNT);
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_ORDER)) {
            mProperties.put(FIELD_ORDER, parseOrder(value));
            return;
        } else if (memberName.equals(FIELD_TOTAL_COUNT)) {
            this.mProperties.put(FIELD_TOTAL_COUNT, value.asLong());
            return;
        } else if (memberName.equals(FIELD_OFFSET)) {
            this.mProperties.put(FIELD_OFFSET, value.asLong());
            return;
        } else if (memberName.equals(FIELD_LIMIT)) {
            this.mProperties.put(FIELD_LIMIT, value.asLong());
            return;
        } else if (memberName.equals(FIELD_ENTRIES)) {
            addCollectionToProperties();
            JsonArray entries = value.asArray();
            for (JsonValue entry : entries) {
                JsonObject obj = entry.asObject();
                collection.add((E) BoxEntity.createEntityFromJson(obj));
            }
            return;
        }

        super.parseJSONMember(member);
    }

    private ArrayList<BoxOrder> parseOrder(JsonValue jsonObject) {
        JsonArray entries = jsonObject.asArray();
        ArrayList<BoxOrder> orders = new ArrayList<BoxOrder>(entries.size());
        for (JsonValue entry : entries) {
            BoxOrder order = new BoxOrder();
            order.createFromJson(entry.asObject());
            orders.add(order);
        }
        return orders;
    }

    @Override
    protected JsonValue parseJsonObject(Map.Entry<String, Object> entry) {
        if (entry.getKey().equals(FIELD_ENTRIES)) {
            JsonArray jsonArr = new JsonArray();
            Collection<E> collection = (Collection) entry.getValue();
            for (E obj : collection) {
                jsonArr.add(obj.toJsonObject());
            }
            return jsonArr;
        }
        return super.parseJsonObject(entry);
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

    public ArrayList<BoxOrder> getSortOrders() {
        return (ArrayList<BoxOrder>) mProperties.get(FIELD_ORDER);
    }

    public static final String FIELD_TOTAL_COUNT = "total_count";
    public static final String FIELD_ENTRIES = "entries";
    public static final String FIELD_OFFSET = "offset";
    public static final String FIELD_LIMIT = "limit";

}
