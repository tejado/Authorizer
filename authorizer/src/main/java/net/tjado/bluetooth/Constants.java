/*
 * Copyright 2018 Google LLC All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.tjado.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothHidDeviceAppQosSettings;
import android.bluetooth.BluetoothHidDeviceAppSdpSettings;

/** Handy constants for the HID Report Descriptor and SDP configuration. */
@SuppressLint("NewApi")
public class Constants {
    public static final int MODE_KEYBOARD = 1;
    public static final int MODE_FIDO = 2;
    static final byte ID_KEYBOARD = 1;

    private static final byte[] HID_REPORT_DESC_KEYBOARD = {
            // Keyboard
            (byte) 0x05, (byte) 0x01, // Usage page (Generic Desktop)
            (byte) 0x09, (byte) 0x06, // Usage (Keyboard)
            (byte) 0xA1, (byte) 0x01, // Collection (Application)
            (byte) 0x85, ID_KEYBOARD, //    Report ID
            (byte) 0x05, (byte) 0x07, //       Usage page (Key Codes)
            (byte) 0x19, (byte) 0xE0, //       Usage minimum (224)
            (byte) 0x29, (byte) 0xE7, //       Usage maximum (231)
            (byte) 0x15, (byte) 0x00, //       Logical minimum (0)
            (byte) 0x25, (byte) 0x01, //       Logical maximum (1)
            (byte) 0x75, (byte) 0x01, //       Report size (1)
            (byte) 0x95, (byte) 0x08, //       Report count (8)
            (byte) 0x81, (byte) 0x02, //       Input (Data, Variable, Absolute) ; Modifier byte
            (byte) 0x75, (byte) 0x08, //       Report size (8)
            (byte) 0x95, (byte) 0x01, //       Report count (1)
            (byte) 0x81, (byte) 0x01, //       Input (Constant)                 ; Reserved byte
            (byte) 0x75, (byte) 0x08, //       Report size (8)
            (byte) 0x95, (byte) 0x06, //       Report count (6)
            (byte) 0x15, (byte) 0x00, //       Logical Minimum (0)
            (byte) 0x25, (byte) 0x65, //       Logical Maximum (101)
            (byte) 0x05, (byte) 0x07, //       Usage page (Key Codes)
            (byte) 0x19, (byte) 0x00, //       Usage Minimum (0)
            (byte) 0x29, (byte) 0x65, //       Usage Maximum (101)
            (byte) 0x81, (byte) 0x00, //       Input (Data, Array)              ; Key array (6 keys)
            (byte) 0xC0               // End Collection
    };

    // FIDO Client libraries are mostly not supporting a Report ID in the HID Report Descriptor
    // due to that, it is not possible to merge Keyboard and FIDO descriptors into one.
    private static final byte[] HID_REPORT_DESC_FIDO = {
        // FIDO
        (byte)0x06, (byte)0xD0, (byte)0xF1,             // Usage Page (FIDO_USAGE_PAGE, 2 bytes)
        (byte)0x09, (byte)0x01,                             // Usage (FIDO_USAGE_U2FHID)
        (byte)0xA1, (byte)0x01,                             // Collection (Application)
        (byte)0x09, (byte)0x20,                             // Usage (FIDO_USAGE_DATA_IN)
        (byte)0x15, (byte)0x00,                             // Logical Minimum (0)
        (byte)0x26, (byte)0xFF, (byte)0x00,                 // Logical Maximum (255, 2 bytes)
        (byte)0x75, (byte)0x08,                             // Report Size (8)
        (byte)0x95, (byte)net.tjado.webauthn.fido.hid.Constants.HID_REPORT_SIZE,      // Report Count (variable)
        (byte)0x81, (byte)0x02,                             // Input (Data, Absolute, Variable)
        (byte)0x09, (byte)0x21,                             // Usage (FIDO_USAGE_DATA_OUT)
        (byte)0x15, (byte)0x00,                             // Logical Minimum (0)
        (byte)0x26, (byte)0xFF, (byte)0x00,                 // Logical Maximum (255, 2 bytes)
        (byte)0x75, (byte)0x08,                             // Report Size (8)
        (byte)0x95, (byte)net.tjado.webauthn.fido.hid.Constants.HID_REPORT_SIZE,                  // Report Count (variable)
        (byte)0x91, (byte)0x02,                             // Output (Data, Absolute, Variable)
        (byte)0xC0                                      // End Collection
    };


    // Keyboard SDP/QoS
    private static final String SDP_NAME_KEYBOARD = "Authorizer Keyboard";
    private static final String SDP_DESCRIPTION_KEYBOARD = "Bluetooth Password Manager";
    private static final String SDP_PROVIDER_KEYBOARD = "github.com/tejado/Authorizer";
    private static final int QOS_TOKEN_RATE_KEYBOARD = 800;
    private static final int QOS_TOKEN_BUCKET_SIZE_KEYBOARD = 9;
    private static final int QOS_PEAK_BANDWIDTH_KEYBOARD= 0;
    private static final int QOS_LATENCY_KEYBOARD = 11250;

    static final BluetoothHidDeviceAppSdpSettings SDP_RECORD_KEYBOARD =
            new BluetoothHidDeviceAppSdpSettings(
                    Constants.SDP_NAME_KEYBOARD,
                    Constants.SDP_DESCRIPTION_KEYBOARD,
                    Constants.SDP_PROVIDER_KEYBOARD,
                    BluetoothHidDevice.SUBCLASS1_KEYBOARD,
                    Constants.HID_REPORT_DESC_KEYBOARD);

    static final BluetoothHidDeviceAppQosSettings QOS_OUT_KEYBOARD =
            new BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    Constants.QOS_TOKEN_RATE_KEYBOARD,
                    Constants.QOS_TOKEN_BUCKET_SIZE_KEYBOARD,
                    Constants.QOS_PEAK_BANDWIDTH_KEYBOARD,
                    Constants.QOS_LATENCY_KEYBOARD,
                    BluetoothHidDeviceAppQosSettings.MAX);

    // FIDO SDP/QoS
    private static final String SDP_NAME_FIDO = "Authorizer Security Key";
    private static final String SDP_DESCRIPTION_FIDO = "FIDO2/U2F Android OS Security Key";
    private static final String SDP_PROVIDER_FIDO = "github.com/tejado/Authorizer";
    private static final int QOS_TOKEN_RATE_FIDO = 1000;
    private static final int QOS_TOKEN_BUCKET_SIZE_FIDO = net.tjado.webauthn.fido.hid.Constants.HID_REPORT_SIZE + 1;
    private static final int QOS_PEAK_BANDWIDTH_FIDO = 2000;
    private static final int QOS_LATENCY_FIDO = 5000;

    static final BluetoothHidDeviceAppSdpSettings SDP_RECORD_FIDO =
            new BluetoothHidDeviceAppSdpSettings(
                    Constants.SDP_NAME_FIDO,
                    Constants.SDP_DESCRIPTION_FIDO,
                    Constants.SDP_PROVIDER_FIDO,
                    BluetoothHidDevice.SUBCLASS2_UNCATEGORIZED,
                    Constants.HID_REPORT_DESC_FIDO);

    static final BluetoothHidDeviceAppQosSettings QOS_OUT_FIDO =
            new BluetoothHidDeviceAppQosSettings(
                    BluetoothHidDeviceAppQosSettings.SERVICE_BEST_EFFORT,
                    Constants.QOS_TOKEN_RATE_FIDO,
                    Constants.QOS_TOKEN_BUCKET_SIZE_FIDO,
                    Constants.QOS_PEAK_BANDWIDTH_FIDO,
                    Constants.QOS_LATENCY_FIDO,
                    BluetoothHidDeviceAppQosSettings.MAX);
}
