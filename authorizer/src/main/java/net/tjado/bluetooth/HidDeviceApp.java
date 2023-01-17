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
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothHidDevice;
import android.bluetooth.BluetoothProfile;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.BinderThread;
import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.annotation.WorkerThread;

/** Helper class that holds all data about the HID Device's SDP record and wraps data sending. */
@SuppressLint({"MissingPermission", "NewApi"})
public class HidDeviceApp
        implements MouseReport.MouseDataSender,
                KeyboardReport.KeyboardDataSender,
                BatteryReport.BatteryDataSender {

    private static final String TAG = "HidDeviceApp";

    /** Used to call back when a device connection state has changed. */
    public interface DeviceStateListener {
        /**
         * Callback that receives the new device connection state.
         *
         * @param device Device that was connected or disconnected.
         * @param state New connection state, see {@link BluetoothProfile#EXTRA_STATE}.
         */
        @MainThread
        void onConnectionStateChanged(BluetoothDevice device, int state);

        /** Callback that receives the app unregister event. */
        @MainThread
        void onAppStatusChanged(boolean registered);
    }

    private final MouseReport mouseReport = new MouseReport();
    private final KeyboardReport keyboardReport = new KeyboardReport();
    private final BatteryReport batteryReport = new BatteryReport();
    private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    @Nullable
    private BluetoothDevice device;
    @Nullable private DeviceStateListener deviceStateListener;
    private boolean lastReportZero;

    /** Callback to receive the HID Device's SDP record state. */
    private final BluetoothHidDevice.Callback callback =
            new BluetoothHidDevice.Callback() {
                @Override
                @BinderThread
                public void onAppStatusChanged(BluetoothDevice pluggedDevice, boolean registered) {
                    super.onAppStatusChanged(pluggedDevice, registered);

                    Log.i(TAG, "onAppStatusChanged: " + registered);
                    HidDeviceApp.this.registered = registered;
                    HidDeviceApp.this.onAppStatusChanged(registered);
                }

                @Override
                @BinderThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    super.onConnectionStateChanged(device, state);
                    HidDeviceApp.this.onConnectionStateChanged(device, state);
                }

                @Override
                @BinderThread
                public void onGetReport(
                        BluetoothDevice device, byte type, byte id, int bufferSize) {
                    super.onGetReport(device, type, id, bufferSize);
                    if (inputHost != null) {
                        if (type != BluetoothHidDevice.REPORT_TYPE_INPUT) {
                            inputHost.reportError(
                                    device, BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                        } else if (!replyReport(device, type, id)) {
                            inputHost.reportError(
                                    device, BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
                        }
                    }
                }

                @Override
                @BinderThread
                public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) {
                    super.onSetReport(device, type, id, data);
                    if (inputHost != null) {
                        inputHost.reportError(device, BluetoothHidDevice.ERROR_RSP_SUCCESS);
                    }
                }
            };

    @Nullable private BluetoothHidDevice inputHost;
    private boolean registered;

    /**
     * Register the HID Device's SDP record.
     *
     * @param inputHost Interface for managing the paired HID Host devices and sending the data.
     */
    @MainThread
    void registerApp(BluetoothProfile inputHost) {
        this.inputHost = ((BluetoothHidDevice) inputHost);
        this.inputHost.registerApp(
                Constants.SDP_RECORD, null, Constants.QOS_OUT, Runnable::run, callback);
    }

    /** Unregister the HID Device's SDP record. */
    @MainThread
    void unregisterApp() {
        if (inputHost != null && registered) {
            inputHost.unregisterApp();
        }
        inputHost = null;
    }

    /**
     * Start listening for device connection state changes.
     *
     * @param listener Callback that will receive the new device connection state.
     */
    @MainThread
    void registerDeviceListener(DeviceStateListener listener) {
        deviceStateListener = (listener);
    }

    /** Stop listening for device connection state changes. */
    @MainThread
    void unregisterDeviceListener() {
        deviceStateListener = null;
    }

    /**
     * Notify that we have a new HID Host to send the data to.
     *
     * @param device New device or {@code null} if we should stop sending any data.
     */
    @MainThread
    public void setDevice(@Nullable BluetoothDevice device) {
        this.device = device;
    }

    @Override
    @WorkerThread
    public void sendMouse(boolean left, boolean right, boolean middle, int dX, int dY, int dWheel) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        byte[] report = mouseReport.setValue(left, right, middle, dX, dY, dWheel);
        if (inputHost != null && device != null) {
            // Check if there is any data to send at all. Save power by skipping zero reports.
            boolean isZeroReport = true;
            for (int i = 0; i < report.length && isZeroReport; i++) {
                if (report[i] != 0) {
                    isZeroReport = false;
                }
            }

            // Allow sending one zero report to release the buttons, but no more than that.
            if (!isZeroReport || !lastReportZero) {
                inputHost.sendReport(device, Constants.ID_MOUSE, report);
            }
            lastReportZero = isZeroReport;
        }
    }

    @Override
    @WorkerThread
    public void sendKeyboard(
            int modifier, int key1, int key2, int key3, int key4, int key5, int key6) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        byte[] report = keyboardReport.setValue(modifier, key1, key2, key3, key4, key5, key6);
        if (inputHost != null && device != null) {
            inputHost.sendReport(device, Constants.ID_KEYBOARD, report);
        }
    }

    @Override
    @WorkerThread
    public void sendScancode(byte[] scancode) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        byte[] report = keyboardReport.setValue(scancode);
        if (inputHost != null && device != null) {
            inputHost.sendReport(device, Constants.ID_KEYBOARD, report);
        }
    }

    @Override
    @MainThread
    public void sendBatteryLevel(float level) {
        // Store the current values in case the host will try to read them with a GET_REPORT call.
        byte[] report = batteryReport.setValue(level);
        if (inputHost != null && device != null) {
            inputHost.sendReport(device, Constants.ID_BATTERY, report);
        }
    }

    @BinderThread
    private void onConnectionStateChanged(BluetoothDevice device, int state) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onConnectionStateChanged(device, state);
            }
        });
    }

    @BinderThread
    private void onAppStatusChanged(boolean registered) {
        mainThreadHandler.post(() -> {
            if (deviceStateListener != null) {
                deviceStateListener.onAppStatusChanged(registered);
            }
        });
    }

    @BinderThread
    private boolean replyReport(BluetoothDevice device, byte type, byte id) {
        @Nullable byte[] report = getReport(id);
        if (report == null) {
            return false;
        }

        if (inputHost != null) {
            inputHost.replyReport(device, type, id, report);
        }
        return true;
    }

    @BinderThread
    @Nullable
    private byte[] getReport(byte id) {
        switch (id) {
            case Constants.ID_KEYBOARD:
                return keyboardReport.getReport();

            case Constants.ID_MOUSE:
                return mouseReport.getReport();

            case Constants.ID_BATTERY:
                return batteryReport.getReport();

            default: // fall out
        }

        Log.e(TAG, "Invalid report ID requested: " + id);
        return null;
    }
}
