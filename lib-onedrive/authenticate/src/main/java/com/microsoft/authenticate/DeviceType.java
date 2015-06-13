package com.microsoft.authenticate;

import static com.microsoft.authenticate.OAuth.DisplayType;

/**
 * The type of the device is used to determine the display query parameter.
 * Phones have a display parameter of android_phone.
 * Tablets have a display parameter of android_tablet.
 */
enum DeviceType {

    /**
     * The device is considered a phone
     */
    PHONE {
        @Override
        public DisplayType getDisplayParameter() {
            return DisplayType.ANDROID_PHONE;
        }
    },

    /**
     * The device is considered a tablet
     */
    TABLET {
        @Override
        public DisplayType getDisplayParameter() {
            return DisplayType.ANDROID_TABLET;
        }
    };

    /**
     * The display parameters for this device type
     * @return The display type
     */
    public abstract DisplayType getDisplayParameter();
}
