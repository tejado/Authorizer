package com.microsoft.authenticate;

import android.app.Activity;
import android.content.res.Configuration;
import android.util.Log;

/**
 * The ScreenSize is used to determine the DeviceType.
 * Small and Normal ScreenSizes are Phones.
 * Large and XLarge are Tablets.
 */
enum ScreenSize {
    /**
     * Small screen
     */
    SMALL {
        @Override
        public DeviceType getDeviceType() {
            return DeviceType.PHONE;
        }
    },

    /**
     * Normal screen
     */
    NORMAL {
        @Override
        public DeviceType getDeviceType() {
            return DeviceType.PHONE;
        }

    },

    /**
     * Large screen
     */
    LARGE {
        @Override
        public DeviceType getDeviceType() {
            return DeviceType.TABLET;
        }
    },

    /**
     * Very large screen
     */
    XLARGE {
        @Override
        public DeviceType getDeviceType() {
            return DeviceType.TABLET;
        }
    };

    /**
     * Gets the device type
     * @return The device type
     */
    public abstract DeviceType getDeviceType();

    /**
     * Configuration.SCREENLAYOUT_SIZE_XLARGE was not provided in API level 9.
     * However, its value of 4 does show up.
     */
    private static final int SCREENLAYOUT_SIZE_XLARGE = 4;

    /**
     * Determine the screen size for a given activity
     * @param activity The activity
     * @return The size of screen
     */
    public static ScreenSize determineScreenSize(final Activity activity)  {
        int screenLayout = activity.getResources().getConfiguration().screenLayout;
        int screenLayoutMasked = screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
        switch (screenLayoutMasked) {
            case Configuration.SCREENLAYOUT_SIZE_SMALL:
                return SMALL;
            case Configuration.SCREENLAYOUT_SIZE_NORMAL:
                return NORMAL;
            case Configuration.SCREENLAYOUT_SIZE_LARGE:
                return LARGE;
            case SCREENLAYOUT_SIZE_XLARGE:
                return XLARGE;
            default:
                // If we cannot determine the ScreenSize, we'll guess and say it's normal.
                Log.d(
                    ScreenSize.class.getSimpleName(),
                    "Unable to determine ScreenSize. A Normal ScreenSize will be returned.");
                return NORMAL;
        }
    }
}
