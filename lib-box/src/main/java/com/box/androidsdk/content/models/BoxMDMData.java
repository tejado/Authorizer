package com.box.androidsdk.content.models;

import java.util.Map;

/**
 * An object containing Mobile Device Management data that may be required to login with certain enterprise and api key combinations.
 */
public class BoxMDMData extends BoxJsonObject {

    public static final String BOX_MDM_DATA = "box_mdm_data";
    public static final String BUNDLE_ID = "bundle_id";
    public static final String MANAGEMENT_ID = "management_id";
    public static final String PUBLIC_ID = "public_id";
    public static final String BILLING_ID = "billing_id";
    public static final String EMAIL_ID = "email_id";


    public BoxMDMData() {
        super();
    }

    public BoxMDMData(Map<String, Object> map) {
        super(map);
    }

    /**
     * Helper method to get values from this object if keys are known. Alternatively this object can be
     * converted into a map using getPropertiesAsHashMap for a similar result.
     * @param key a string that maps to an object in this class.
     * @return an object indexed by the given key, or null if there is no such object.
     */
    public Object getValue(final String key){
        return mProperties.get(key);
    }

    public void setValue(final String key, final String value){
        mProperties.put(key, value);
    }

    public void setBundleId(final String bundleId){
        setValue(BUNDLE_ID, bundleId);
    }

    public void setPublicId(final String publicId){
        setValue(PUBLIC_ID, publicId);
    }

    public void setManagementId(final String managementId){
        setValue(MANAGEMENT_ID, managementId);
    }

    public void setEmailId(final String emailId){
        setValue(EMAIL_ID, emailId);
    }

    public void setBillingId(final String billingId){
        setValue(BILLING_ID, billingId);
    }

    public String getBundleId(){
        return (String)getValue(PUBLIC_ID);
    }

    public String getPublicId(){
        return (String)getValue(PUBLIC_ID);
    }

    public String getManagementId(){
        return (String)getValue(MANAGEMENT_ID);
    }

    public String getEmailId(){
        return (String)getValue(EMAIL_ID);
    }

    public String getBillingIdId(){
        return (String)getValue(BILLING_ID);
    }


}
