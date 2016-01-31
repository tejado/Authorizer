package com.box.androidsdk.content.requests;


import com.box.androidsdk.content.models.BoxSession;
import com.box.androidsdk.content.models.BoxUser;

/**
 * Abstract class representing a request to update a user.
 *
 * @param <E>   type of BoxUser to be returned in the response.
 * @param <R>   type of BoxRequest being created.
 */
abstract class BoxRequestUserUpdate<E extends BoxUser, R extends BoxRequest<E,R>> extends BoxRequestItem<E, R> {
    public BoxRequestUserUpdate(Class<E> clazz, String id, String requestUrl, BoxSession session) {
        super(clazz, id, requestUrl, session);
    }

    /**
     * Returns the name currently set for the user in the request.
     *
     * @return  name for the user, or null if not set.
     */
    public String getName() {
        return (String) mBodyMap.get(BoxUser.FIELD_NAME);
    }

    /**
     * Sets the new name for the user in the request.
     *
     * @param name  new name for the user.
     * @return  request with the updated name.
     */
    public R setName(String name) {
        mBodyMap.put(BoxUser.FIELD_NAME, name);
        return (R) this;
    }

    /**
     * Returns the role currently set for the user in the request.
     *
     * @return  {@link com.box.androidsdk.content.models.BoxUser.Role} for the user, or null if not set.
     */
    public BoxUser.Role getRole() {
        return (BoxUser.Role) mBodyMap.get(BoxUser.FIELD_ROLE);
    }

    /**
     * Sets the new role for the user.
     *
     * @param role  new role for the user.
     * @return  request with the updated role.
     */
    public R setRole(BoxUser.Role role) {
        mBodyMap.put(BoxUser.FIELD_ROLE, role);
        return (R) this;
    }

    /**
     * Returns whether Box Sync is enabled for the user or not.
     *
     * @return  whether or not Box Sync is enabled for the user, or null if not set.
     */
    public boolean getIsSyncEnabled() {
        return (Boolean) mBodyMap.get(BoxUser.FIELD_IS_SYNC_ENABLED);
    }

    /**
     * Sets whether or not Box Sync is enabled for the user.
     *
     * @param isSyncEnabled whether or not Box Sync is enabled for the user.
     * @return  request with the updated value for whether Sync is enabled or not.
     */
    public R setIsSyncEnabled(boolean isSyncEnabled) {
        mBodyMap.put(BoxUser.FIELD_IS_SYNC_ENABLED, isSyncEnabled);
        return (R) this;
    }

    /**
     * Returns the job title currently set for the user.
     *
     * @return  job title of the user.
     */
    public String getJobTitle() {
        return (String) mBodyMap.get(BoxUser.FIELD_JOB_TITLE);
    }

    /**
     * Sets the job title for the user in the request.
     *
     * @param jobTitle  new job title for the user.
     * @return  request with the updated job title.
     */
    public R setJobTitle(String jobTitle) {
        mBodyMap.put(BoxUser.FIELD_JOB_TITLE, jobTitle);
        return (R) this;
    }

    /**
     * Returns the phone number for the user currently set in the request.
     *
     * @return  phone number for the user, or null if not set.
     */
    public String getPhone() {
        return (String) mBodyMap.get(BoxUser.FIELD_PHONE);
    }

    /**
     * Sets the phone number of the user in the request.
     *
     * @param phone new phone number for the user.
     * @return  request with the updated phone number.
     */
    public R setPhone(String phone) {
        mBodyMap.put(BoxUser.FIELD_PHONE, phone);
        return (R) this;
    }

    /**
     * Returns the address currently set for the user in the request.
     *
     * @return  address for the user, or null if not set.
     */
    public String getAddress() {
        return (String) mBodyMap.get(BoxUser.FIELD_ADDRESS);
    }

    /**
     * Sets the address of the user in the request.
     *
     * @param address new address for the user.
     * @return  request with the updated address.
     */
    public R setAddress(String address) {
        mBodyMap.put(BoxUser.FIELD_ADDRESS, address);
        return (R) this;
    }

    /**
     * Returns the amount of available space currently set for the user in the request.
     *
     * @return  space for the user in bytes, or null if not set.
     */
    public double getSpaceAmount() {
        return (Double) mBodyMap.get(BoxUser.FIELD_SPACE_AMOUNT);
    }

    /**
     * Sets the amount of space available for the user in the request.
     *
     * @param spaceAmount   new amount of space available for the user in bytes.
     * @return  request with the updated available space.
     */
    public R setSpaceAmount(double spaceAmount) {
        mBodyMap.put(BoxUser.FIELD_SPACE_AMOUNT, spaceAmount);
        return (R) this;
    }

    /**
     * Returns whether or not the user can see other enterprise users in her contact list currently set in the request.
     *
     * @return  whether or not the user can see other enterprise users in her contact list, or null if not set.
     */
    public boolean getCanSeeManagedUsers() {
        return (Boolean) mBodyMap.get(BoxUser.FIELD_CAN_SEE_MANAGED_USERS);
    }

    /**
     * Sets whether or not the user can see other enterprise users in her contact list in the request.
     *
     * @param canSeeManagedUsers    whether or not the user can see other enterprise users in her contact list.
     * @return  request with the updated value for whether or not the user can see other enterprise users in her contact list.
     */
    public R setCanSeeManagedUsers(boolean canSeeManagedUsers) {
        mBodyMap.put(BoxUser.FIELD_CAN_SEE_MANAGED_USERS, canSeeManagedUsers);
        return (R) this;
    }

    /**
     * Returns the status currently set for the user in the request.
     *
     * @return  {@link com.box.androidsdk.content.models.BoxUser.Status} currently set for the user, or null if not set.
     */
    public BoxUser.Status getStatus() {
        return (BoxUser.Status) mBodyMap.get(BoxUser.FIELD_STATUS);
    }

    /**
     * Sets the status for the user in the request.
     *
     * @param status    new {@link com.box.androidsdk.content.models.BoxUser.Status} for the user.
     * @return  request with the updated status for the user.
     */
    public R setStatus(BoxUser.Status status) {
        mBodyMap.put(BoxUser.FIELD_STATUS, status);
        return (R) this;
    }

    /**
     * Returns the timezone currently set for the user in the request.
     *
     * @return  timezone currently set for the user, or null if not set.
     */
    public String getTimezone() {
        return (String) mBodyMap.get(BoxUser.FIELD_TIMEZONE);
    }

    /**
     * Sets the timezone for the user in the request.
     *
     * @param timezone  new timezone for the user.
     * @return  request with the updated timezone.
     */
    public R setTimezone(String timezone) {
        mBodyMap.put(BoxUser.FIELD_TIMEZONE, timezone);
        return (R) this;
    }

    /**
     * Returns the value currently set in the request for whether to exempt the user from Enterprise device limits.
     *
     * @return  Boolean representing whether to exempt the user from Enterprise device limits, or null if not set.
     */
    public boolean getIsExemptFromDeviceLimits() {
        return (Boolean) mBodyMap.get(BoxUser.FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS);
    }

    /**
     * Sets whether to exempt the user from Enterprise device limits in the request.
     *
     * @param isExemptFromDeviceLimits  new value for whether the user is exempt from Enterprise device limits.
     * @return  request with the updated value for whether the user is exempt from Enterprise device limits.
     */
    public R setIsExemptFromDeviceLimits(boolean isExemptFromDeviceLimits) {
        mBodyMap.put(BoxUser.FIELD_IS_EXEMPT_FROM_DEVICE_LIMITS, isExemptFromDeviceLimits);
        return (R) this;
    }

    /**
     * Returns the value currently set in the request for whether to exempt the user from two-factor authentication.
     *
     * @return  Boolean representing whether to exempt the user from two-factor authentication, or null if not set.
     */
    public boolean getIsExemptFromLoginVerification() {
        return (Boolean) mBodyMap.get(BoxUser.FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION);
    }

    /**
     * Sets whether the user is exempt from two-factor authentication in the request.
     *
     * @param isExemptFromLoginVerification new value for whether the user is exempt from two-factor authentication.
     * @return  request with the updated value for whether the user is exempt from two-factor authentication.
     */
    public R setIsExemptFromLoginVerification(boolean isExemptFromLoginVerification) {
        mBodyMap.put(BoxUser.FIELD_IS_EXEMPT_FROM_LOGIN_VERIFICATION, isExemptFromLoginVerification);
        return (R) this;
    }
}
