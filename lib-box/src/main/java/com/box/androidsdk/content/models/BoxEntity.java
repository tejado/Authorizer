package com.box.androidsdk.content.models;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.JsonValue;

import java.util.Map;

/**
 * Class that represents an entity with a type and ID on Box.
 */
public class BoxEntity extends BoxJsonObject {

    private static final long serialVersionUID = 1626798809346520004L;
    public static final String FIELD_ID = "id";
    public static final String FIELD_TYPE = "type";
    public static final String FIELD_ITEM_TYPE = "item_type";
    public static final String FIELD_ITEM_ID = "item_id";

    /**
     * Constructs an empty BoxEntity object.
     */
    public BoxEntity() {
        super();
    }


    /**
     * Constructs a BoxEntity with the provided map values.
     * @param map   map of keys and values of the object.
     */
    public BoxEntity(Map<String, Object> map) {
        super(map);
    }

    /**
     * Gets the id.
     *
     * @return the id of the entity.
     */
    public String getId() {
        String id =  (String) mProperties.get(FIELD_ID);
        if (id == null){
            return (String) mProperties.get(FIELD_ITEM_ID);
        }
        return id;
    }

    /**
     * Gets the type of the entity.
     *
     * @return the entity type.
     */
    public String getType() {
        String type =  (String) mProperties.get(FIELD_TYPE);
        if (type == null){
            return (String) mProperties.get(FIELD_ITEM_TYPE);
        }
        return type;
    }

    @Override
    protected void parseJSONMember(JsonObject.Member member) {
        String memberName = member.getName();
        JsonValue value = member.getValue();
        if (memberName.equals(FIELD_ID)) {
            this.mProperties.put(FIELD_ID, value.asString());
            return;
        } else if (memberName.equals(FIELD_TYPE)) {
            this.mProperties.put(FIELD_TYPE, value.asString());
            return;
        } else if (memberName.equals(FIELD_ITEM_TYPE)) {
            this.mProperties.put(FIELD_ITEM_TYPE, value.asString());
            return;
        } else if (memberName.equals(FIELD_ITEM_ID)) {
            this.mProperties.put(FIELD_ITEM_ID, value.asString());
            return;
        }

        super.parseJSONMember(member);
    }


    /**
     * Helper method that will parse into a known child of BoxEntity.
     * @param json json representing a BoxEntity or one of its known children.
     * @return a BoxEntity or one of its known children.
     */
    public static BoxEntity createEntityFromJson(final String json){
        BoxEntity createdByEntity = new BoxEntity();
        createdByEntity.createFromJson(json);
        if (createdByEntity.getType() == null){
            return createdByEntity;
        }
        if (createdByEntity.getType().equals(BoxCollection.TYPE)) {
            BoxCollection collection = new BoxCollection();
            collection.createFromJson(json);
            return collection;
        } else if (createdByEntity.getType().equals(BoxComment.TYPE)) {
            BoxComment comment = new BoxComment();
            comment.createFromJson(json);
            return comment;
        } else if (createdByEntity.getType().equals(BoxCollaboration.TYPE)) {
            BoxCollaboration collaboration = new BoxCollaboration();
            collaboration.createFromJson(json);
            return collaboration;
        } else if (createdByEntity.getType().equals(BoxEnterprise.TYPE)) {
            BoxEnterprise enterprise = new BoxEnterprise();
            enterprise.createFromJson(json);
            return enterprise;
        } else if (createdByEntity.getType().equals(BoxFileVersion.TYPE)) {
            BoxFileVersion version = new BoxFileVersion();
            version.createFromJson(json);
            return version;
        } else if (createdByEntity.getType().equals(BoxEvent.TYPE)) {
            // because enterprise events are a superset of BoxEvent create this version if necessary.
            BoxEnterpriseEvent version = new BoxEnterpriseEvent();
            version.createFromJson(json);
            return version;
        }

        BoxEntity item = BoxItem.createBoxItemFromJson(json);
        if (item != null){
            return item;
        }
        item = BoxCollaborator.createCollaboratorFromJson(json);
        if (item != null){
            return item;
        }
        return null;
    }

    /**
     * Helper method that will parse into a known child of BoxEntity.
     * @param json JsonObject representing a BoxEntity or one of its known children.
     * @return a BoxEntity or one of its known children.
     */
    public static BoxEntity createEntityFromJson(final JsonObject json){
        BoxEntity createdByEntity = new BoxEntity();
        createdByEntity.createFromJson(json);
        if (createdByEntity.getType() == null){
            return createdByEntity;
        }
        if (createdByEntity.getType().equals(BoxCollection.TYPE)) {
            BoxCollection collection = new BoxCollection();
            collection.createFromJson(json);
            return collection;
        } else if (createdByEntity.getType().equals(BoxComment.TYPE)) {
                BoxComment comment = new BoxComment();
                comment.createFromJson(json);
                return comment;
        } else if (createdByEntity.getType().equals(BoxCollaboration.TYPE)) {
                BoxCollaboration collaboration = new BoxCollaboration();
                collaboration.createFromJson(json);
                return collaboration;
        } else if (createdByEntity.getType().equals(BoxEnterprise.TYPE)) {
                BoxEnterprise enterprise = new BoxEnterprise();
                enterprise.createFromJson(json);
                return enterprise;
        } else if (createdByEntity.getType().equals(BoxFileVersion.TYPE)) {
                BoxFileVersion version = new BoxFileVersion();
                version.createFromJson(json);
                return version;
        } else if (createdByEntity.getType().equals(BoxEvent.TYPE)) {
                // because enterprise events are a superset of BoxEvent create this version if necessary.
                BoxEnterpriseEvent version = new BoxEnterpriseEvent();
                version.createFromJson(json);
                return version;
        }

        BoxEntity item = BoxItem.createBoxItemFromJson(json);
        if (item != null){
            return item;
        }
        item = BoxCollaborator.createCollaboratorFromJson(json);
        if (item != null){
            return item;
        }
        return null;
    }

}
