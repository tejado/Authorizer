package com.box.androidsdk.content.models;

import com.box.androidsdk.content.utils.IStreamPosition;
import com.eclipsesource.json.JsonArray;
import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Collection;
import java.util.HashSet;

/**
 * Class representing a list of events fired off by the Box events API.
 */
public class BoxListEvents extends BoxList<BoxEvent> implements IStreamPosition {

    private static final long serialVersionUID = 2397451459829964208L;
    public static final String FIELD_CHUNK_SIZE = "chunk_size";
    public static final String FIELD_NEXT_STREAM_POSITION = "next_stream_position";

    private boolean mFilterDuplicates = true;
    private final HashSet<String> mEventIds = new HashSet<String>();


    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_NEXT_STREAM_POSITION)) {
            mProperties.put(FIELD_NEXT_STREAM_POSITION, value.asLong());
            return;
        } else if (memberName.equals(FIELD_CHUNK_SIZE)) {
            mProperties.put(FIELD_CHUNK_SIZE, value.asLong());
            return;
        } else if (memberName.equals(FIELD_ENTRIES)) {
            JsonArray entries = value.asArray();
            for (JsonValue entry : entries) {
                BoxEvent event = new BoxEvent();
                JsonObject obj = entry.asObject();
                event.createFromJson(obj);
                add(event);
            }
            mProperties.put(FIELD_ENTRIES, collection);
            return;
        }
        super.parseJSONMember(member);
    }

    @Override
    public boolean add(BoxEvent boxEvent) {
        if (mFilterDuplicates && mEventIds.contains(boxEvent.getEventId())) {
            return false;
        }
        mEventIds.add(boxEvent.getEventId());
        return super.add(boxEvent);
    }

    @Override
    public boolean remove(Object o) {
        if (o instanceof BoxEvent) {
            mEventIds.remove(((BoxEvent) o).getEventId());
        }
        return super.remove(o);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        for (Object o : c) {
            if (o instanceof BoxEvent) {
                mEventIds.remove(((BoxEvent) o).getEventId());
            }
        }
        return super.removeAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends BoxEvent> c) {
        boolean addSuccessful = true;
        for (BoxEvent event : c) {
            addSuccessful &= add(event);
        }

        return addSuccessful;
    }

    @Override
    public void clear() {
        mEventIds.clear();
        super.clear();
    }

    /**
     * Gets the number of event records returned in this chunk.
     *
     * @return number of event records returned.
     */
    public Long getChunkSize() {
        return (Long) mProperties.get(FIELD_CHUNK_SIZE);
    }

    /**
     * Gets the next position in the event stream that you should request in order to get the next events.
     *
     * @return next position in the event stream to request in order to get the next events.
     */
    public Long getNextStreamPosition() {
        return (Long) mProperties.get(FIELD_NEXT_STREAM_POSITION);
    }

    /**
     * Sets whether or not to filter out duplicate events which can occur due to syncing issues.
     *
     * @param filterDuplicates whether or not to filter out duplicate events.
     */
    public void setFilterDuplicates(boolean filterDuplicates) {
        mFilterDuplicates = filterDuplicates;
    }


}
