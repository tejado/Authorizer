package com.box.androidsdk.content.models;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Class that represents a group of Box users.
 */
public class BoxGroup extends BoxCollaborator {

    private static final long serialVersionUID = 5872741782856508553L;
    public static final String TYPE = "group";

    /**
     * Constructs an empty BoxGroup.
     */
    public BoxGroup() {
        super();
    }

    /**
     * Constructs a BoxGroup with the provided map values.
     * @param map   map of keys and values of the object.
     */
    public BoxGroup(Map<String, Object> map) {
        super(map);
    }

    /**
     * A convenience method to create an empty group with just the id and type fields set. This allows
     * the ability to interact with the content sdk in a more descriptive and type safe manner
     *
     * @param groupId  the id of group to create
     * @return an empty BoxGroup object that only contains id and type information
     */
    public static BoxGroup createFromId(String groupId) {
        LinkedHashMap<String, Object> groupMap = new LinkedHashMap<String, Object>();
        groupMap.put(BoxCollaborator.FIELD_ID, groupId);
        groupMap.put(BoxCollaborator.FIELD_TYPE, BoxUser.TYPE);
        return new BoxGroup(groupMap);
    }

}
