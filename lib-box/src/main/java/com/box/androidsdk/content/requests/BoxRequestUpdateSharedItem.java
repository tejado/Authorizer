package com.box.androidsdk.content.requests;

import com.box.androidsdk.content.models.BoxItem;
import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxSharedLink;
import com.box.androidsdk.content.utils.BoxDateFormat;

import java.text.ParseException;
import java.util.Date;
import java.util.LinkedHashMap;

/**
 * Class representing a request to update a shared item.
 *
 * @param <E>   type of BoxItem to be returned in the response.
 * @param <R>   type of BoxRequest being created.
 */
public abstract class BoxRequestUpdateSharedItem<E extends BoxItem, R extends BoxRequest<E,R>> extends BoxRequestItemUpdate<E, R> {
    public BoxRequestUpdateSharedItem(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
        mRequestMethod = Methods.PUT;
    }

    protected BoxRequestUpdateSharedItem(BoxRequestItemUpdate r) {
        super(r);
    }

    /**
     * Gets the shared link access currently set for the item in the request.
     *
     * @return  shared link access for the item, or null if not set.
     */
    public BoxSharedLink.Access getAccess() {
        return mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK) ?
                ((BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK)).getAccess() :
                null;
    }

    /**
     * Sets the shared link access for the item in the request.
     *
     * @param access    new shared link access for the item.
     * @return  request with the updated shared link access.
     */
    public R setAccess(BoxSharedLink.Access access) {
        LinkedHashMap<String,Object> map = new LinkedHashMap<String,Object>();
        if (mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK)) {
            BoxSharedLink sl = (BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK);
            map = new LinkedHashMap(sl.getPropertiesAsHashMap());
        }
        map.put(BoxSharedLink.FIELD_ACCESS, access);
        BoxSharedLink sharedLink = new BoxSharedLink(map);
        mBodyMap.put(BoxItem.FIELD_SHARED_LINK, sharedLink);
        return (R) this;
    }

    /**
     * Returns the date the link will be disabled at currently set in the request.
     *
     * @return  date the shared link will be disabled at, or null if not set.
     */
    public Date getUnsharedAt() {
        return mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK) ?
                ((BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK)).getUnsharedDate() :
                null;
    }

    /**
     * Sets the date that this shared link will be deactivated. If this is set to null it will remove the
     * unshared at date that is set on this item.
     * Note: the date will be rounded to the day as the API does not support hours, minutes, or seconds
     *
     * @param unsharedAt the date that this shared link will be deactivated.
     */
    public R setUnsharedAt(Date unsharedAt) throws ParseException {
        LinkedHashMap<String,Object> map = new LinkedHashMap<String, Object>();
        if (mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK)) {
            BoxSharedLink sl = (BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK);
            map = new LinkedHashMap(sl.getPropertiesAsHashMap());
        }
        if (unsharedAt == null){
            map.put(BoxSharedLink.FIELD_UNSHARED_AT, null);
        } else {
            map.put(BoxSharedLink.FIELD_UNSHARED_AT, BoxDateFormat.convertToDay(unsharedAt));
        }
        BoxSharedLink sharedLink = new BoxSharedLink(map);
        mBodyMap.put(BoxItem.FIELD_SHARED_LINK, sharedLink);
        return (R) this;
    }

    /**
     *
     * Removes an unshared at date that is set on this item.
     */
    public R setRemoveUnsharedAtDate() throws ParseException {
        return setUnsharedAt(null);
    }

    /**
     * Returns the shared link password currently set in the request.
     *
     * @return  the password for the item's shared link, or null if not set.
     */
    public String getPassword(){
        return mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK) ?
                ((BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK)).getPassword() :
                null;
    }

    /**
     * Sets the shared link password in the request.
     *
     * @param password  new password for the shared link, or null to remove the password.
     * @return  request with the updated shared link password.
     */
    public R setPassword(final String password){
        LinkedHashMap<String,Object> map = new LinkedHashMap<String, Object>();
        if (mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK)) {
            BoxSharedLink sl = (BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK);
            map = new LinkedHashMap(sl.getPropertiesAsHashMap());
        }
        map.put(BoxSharedLink.FIELD_PASSWORD, password);
        BoxSharedLink sharedLink = new BoxSharedLink(map);
        mBodyMap.put(BoxItem.FIELD_SHARED_LINK, sharedLink);
        return (R) this;
    }

    /**
     * Returns the value for whether the shared link allows downloads currently set in the request.
     *
     * @return  Boolean for whether the shared link allows downloads, or null if not set.
     */
    protected Boolean getCanDownload() {
        return mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK) ?
                ((BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK)).getPermissions().getCanDownload() :
                null;
    }

    /**
     * Sets whether the shared link allows downloads in the request.
     *
     * @param canDownload   new value for whether the shared link allows downloads.
     * @return  request with the updated value for whether the shared link allows downloads.
     */
    protected R setCanDownload(boolean canDownload) {
        LinkedHashMap<String,Object> sharedLinkMap = new LinkedHashMap<String, Object>();
        if (mBodyMap.containsKey(BoxItem.FIELD_SHARED_LINK)) {
            BoxSharedLink sl = (BoxSharedLink) mBodyMap.get(BoxItem.FIELD_SHARED_LINK);
            sharedLinkMap = new LinkedHashMap(sl.getPropertiesAsHashMap());
        }
        LinkedHashMap<String,Object> permissionsMap = new LinkedHashMap<String, Object>();
        if (sharedLinkMap.containsKey(BoxSharedLink.FIELD_PERMISSIONS)) {
            BoxSharedLink.Permissions permissions = (BoxSharedLink.Permissions) sharedLinkMap.get(BoxSharedLink.FIELD_PERMISSIONS);
            permissionsMap = new LinkedHashMap(permissions.getPropertiesAsHashMap());
        }
        permissionsMap.put(BoxSharedLink.Permissions.FIELD_CAN_DOWNLOAD, canDownload);
        BoxSharedLink.Permissions permissions = new BoxSharedLink.Permissions(permissionsMap);
        sharedLinkMap.put(BoxSharedLink.FIELD_PERMISSIONS, permissions);
        BoxSharedLink sharedLink = new BoxSharedLink(sharedLinkMap);
        mBodyMap.put(BoxItem.FIELD_SHARED_LINK, sharedLink);
        return (R) this;
    }

    public BoxRequestUpdateSharedItem updateSharedLink() {
        return this;
    }
}
