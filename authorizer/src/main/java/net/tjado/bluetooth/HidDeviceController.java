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
import android.content.Context;
import android.os.SystemClock;
import android.util.ArraySet;
import android.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;

import net.tjado.authorizer.Utilities;


/** Central point for enabling the HID SDP record and sending all data. */
@SuppressLint("MissingPermission")
public class HidDeviceController
        implements KeyboardReport.KeyboardDataSender {

    private static final String TAG = "HidDeviceController";

    /** Compound interface that listens to both device and service state changes. */
    public interface ProfileListener
            extends HidDeviceApp.DeviceStateListener, HidDeviceProfile.ServiceStateListener {}

    static final class InstanceHolder {
        static final HidDeviceController INSTANCE = createInstance();

        private static HidDeviceController createInstance() {
            return new HidDeviceController(new HidDeviceApp(), new HidDeviceProfile());
        }
    }

    private final HidDeviceApp hidDeviceApp;
    private final HidDeviceProfile hidDeviceProfile;

    private final Object lock = new Object();

    @SuppressLint("NewApi")
    private final Set<ProfileListener> listeners = new ArraySet<>();

    @Nullable
    private BluetoothDevice connectedDevice;

    @Nullable
    private BluetoothDevice waitingForDevice;

    private boolean isAppRegistered;

    private boolean registerAppReturnStatus;

    private int currentMode = Constants.MODE_FIDO;

    /**
     * @param hidDeviceApp HID Device App interface.
     * @param hidDeviceProfile Interface to manage paired HID Host devices.
     */
    private HidDeviceController(HidDeviceApp hidDeviceApp, HidDeviceProfile hidDeviceProfile) {
        this.hidDeviceApp = hidDeviceApp;
        this.hidDeviceProfile = hidDeviceProfile;
    }


    public boolean isHidKeyboardMode() {
        return currentMode == Constants.MODE_KEYBOARD;
    }

    public boolean isHidFidoMode() {
        return currentMode == Constants.MODE_FIDO;
    }

    /**
     * Retrieve the singleton instance of the class.
     *
     * @return Singleton instance.
     */
    public static HidDeviceController getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public HidDeviceProfile registerKeyboard(Context context, ProfileListener listener) {
        Log.i(TAG, "registerKeyboard");
        return register(context, listener, Constants.MODE_KEYBOARD);
    }

    public HidDeviceProfile registerFido(Context context, ProfileListener listener) {
        Log.i(TAG, "registerFido");
        return register(context, listener, Constants.MODE_FIDO);
    }

    /**
     * Ensure that the HID Device SDP record is registered and start listening for the profile proxy
     * and HID Host connection state changes.
     *
     * @param context Context that is required to listen for battery charge.
     * @param listener Callback that will receive the profile events.
     * @return Interface for managing the paired HID Host devices.
     */
    @MainThread
    public HidDeviceProfile register(Context context, ProfileListener listener, int mode) {
        synchronized (lock) {
            Log.i(TAG, "register");
            if (!listeners.add(listener)) {
                Log.w(TAG, "user already registered");
                return hidDeviceProfile;
            }
            if (listeners.size() > 1) {
                Log.w(TAG, "too many registered");
                return hidDeviceProfile;
            }

            currentMode = mode;

            context = (context).getApplicationContext();
            hidDeviceProfile.registerServiceListener(context, profileListener);
            hidDeviceApp.registerDeviceListener(profileListener);
        }
        return hidDeviceProfile;
    }

    /**
     * Stop listening for the profile events. When the last listener is unregistered, the SD record
     * for HID Device will also be unregistered.
     *
     * @param listener Callback to unregisterDeviceListener.
     */
    @MainThread
    public void unregister(ProfileListener listener) {
        Log.i(TAG, "unregister");

        synchronized (lock) {
            if (!listeners.remove(listener)) {
                // This user was removed before
                Log.w(TAG, "user already removed");
                return;
            }
            if (!listeners.isEmpty()) {
                // Some users are still left
                Log.w(TAG, "listeners are still registered");
                return;
            }

            hidDeviceApp.unregisterDeviceListener();

            for (BluetoothDevice device : hidDeviceProfile.getConnectedDevices()) {
                hidDeviceProfile.disconnect(device);
            }

            hidDeviceApp.setDevice(null);
            hidDeviceApp.unregisterApp();

            hidDeviceProfile.unregisterServiceListener();

            connectedDevice = null;
            waitingForDevice = null;
            isAppRegistered = false;
        }
    }

    /**
     * Check if there is any active connection present.
     *
     * @return {@code true} if HID Host is connected, {@code false} otherwise.
     */
    public boolean isConnected() {
        return (connectedDevice != null);
    }

    public BluetoothDevice getConnectedDevice() {
        return connectedDevice;
    }

    /**
     * Initiate connection sequence for the specified HID Host. If another device is already
     * connected, it will be disconnected first. If the parameter is {@code null}, then the service
     * will only disconnect from the current device.
     *
     * @param device New HID Host to connect to or {@code null} to disconnect.
     */
    @MainThread
    public boolean requestConnect(BluetoothDevice device) {
        String deviceName = device != null ? device.getName() : "null";
        Log.i(TAG, "requestConnect: " + deviceName);

        synchronized (lock) {
            waitingForDevice = device;
            if (!isAppRegistered) {
                // Request will be fulfilled as soon the as app becomes registered.
                Log.e(TAG, "App Is Not Registered");
                return false;
            }

            connectedDevice = null;
            updateDeviceList();

            if (device != null && device.equals(connectedDevice)) {
                for (ProfileListener listener : listeners) {
                    listener.onConnectionStateChanged(device, BluetoothProfile.STATE_CONNECTED);
                }
            }
        }

        return true;
    }

    public void disconnect() {
        if (isConnected()) {
            requestConnect(null);
        }
    }

    public int getMode() {
        return currentMode;
    }

    public boolean getRegisterAppStatus() {
        return registerAppReturnStatus;
    }


    public void sendKeyboard(
            int modifier, int key1, int key2, int key3, int key4, int key5, int key6) {
        synchronized (lock) {
            if (connectedDevice != null) {
                hidDeviceApp.sendKeyboard(modifier, key1, key2, key3, key4, key5, key6);
            }
        }
    }

    public void sendToKeyboardHost(byte[] keyboardOutput) {
        Log.d(TAG, "sendToKeyboardHost");

        synchronized (lock) {
            Utilities.dbginfo(TAG, "send");
            try
            {
                if (keyboardOutput != null) {

                    int blockSize = 8;
                    int blockCount = keyboardOutput.length / blockSize;

                    int start = 0;
                    for (int i = 0; i < blockCount; i++) {
                        byte[] scancode = Arrays.copyOfRange(keyboardOutput, start, start + blockSize);
                        Utilities.dbginfo(TAG, "send: " + Utilities.bytesToHex(scancode) );

                        sendScancodeInternal(scancode);
                        clean();
                        start += blockSize;
                    }
                }
            } catch (IOException e) {
                Utilities.dbginfo(TAG, "send error: " + e.getLocalizedMessage());
                e.printStackTrace();
            }
        }
    }

    private void clean() throws IOException
    {
        // overwriting the last keystroke, otherwise it will be repeated until the next writing
        // and it would not be possible to repeat the keystroke
        byte[] scancode = new byte[] {0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};
        Utilities.dbginfo(TAG, "RST > " + Utilities.bytesToHex(scancode));
        sendScancode(scancode);
    }

    public void sendScancode(byte[] scancode) {
        synchronized (lock) {
            if (connectedDevice != null) {
                sendScancodeInternal(scancode);
            }
        }
    }

    private void sendScancodeInternal(byte[] scancode) {
        hidDeviceApp.sendScancode(scancode);
    }

    private final ProfileListener profileListener =
            new ProfileListener() {
                @Override
                @MainThread
                public void onServiceStateChanged(BluetoothProfile proxy) {
                    Log.i(TAG, "Internal profileListener - onServiceStateChanged");
                    synchronized (lock) {
                        if (proxy == null) {
                            if (isAppRegistered) {
                                // Service has disconnected before we could unregister the app.
                                // Notify listeners, update the UI and internal state.
                                onAppStatusChanged(false);
                            }
                        } else {
                            registerAppReturnStatus = hidDeviceApp.registerApp(proxy, currentMode);
                            Log.i(TAG, "Internal profileListener - registerApp return value: " + registerAppReturnStatus);
                            if(isAppRegistered && registerAppReturnStatus == false) {
                                Log.i(TAG, "Internal profileListener - isAppRegistered state: " + isAppRegistered);
                            }
                        }

                        updateDeviceList();
                        for (ProfileListener listener : listeners) {
                            Log.d(TAG, "Internal profileListener - onServiceStateChanged - exec listener");
                            listener.onServiceStateChanged(proxy);
                        }
                    }
                }

                @Override
                @MainThread
                public void onConnectionStateChanged(BluetoothDevice device, int state) {
                    Log.i(TAG, "Internal profileListener - onConnectionStateChanged: " + state);
                    synchronized (lock) {
                        if (state == BluetoothProfile.STATE_CONNECTED) {
                            // A new connection was established. If we weren't expecting that, it
                            // must be an incoming one. In that case, we shouldn't try to disconnect
                            // from it.
                            waitingForDevice = device;
                        } else if (state == BluetoothProfile.STATE_DISCONNECTED) {
                            // If we are disconnected from a device we are waiting to connect to, we
                            // ran into a timeout and should no longer try to connect.
                            if (device == waitingForDevice) {
                                waitingForDevice = null;
                            }
                        }

                        updateDeviceList();
                        for (ProfileListener listener : listeners) {
                            listener.onConnectionStateChanged(device, state);
                        }
                    }
                }

                @Override
                @MainThread
                public void onAppStatusChanged(boolean registered) {
                    Log.i(TAG, "Internal profileListener - onAppStatusChanged: " + registered);
                    synchronized (lock) {
                        if (isAppRegistered == registered) {
                            // We are4 already in the correct state.
                            return;
                        }
                        isAppRegistered = registered;

                        for (ProfileListener listener : listeners) {
                            listener.onAppStatusChanged(registered);
                        }

                        if (registered && waitingForDevice != null) {
                            // Fulfill the postponed request to connect.
                            Log.d(TAG, "Fulfill the postponed request to connect: " + waitingForDevice.getName());
                            requestConnect(waitingForDevice);
                        }
                    }
                }

                @Override
                @MainThread
                public void onInterruptData(BluetoothDevice device,
                                            int reportId, byte[] data,
                                            BluetoothHidDevice inputHost) {
                    Log.i(TAG, "Internal profileListener - onInterruptData");
                    synchronized (lock) {
                        if (data == null) {
                            // No data to process - nothing to do
                            return;
                        }

                        for (ProfileListener listener : listeners) {
                            if (listener != null) {
                                listener.onInterruptData(device, reportId, data, inputHost);
                            }
                        }
                    }
                }
            };

    @MainThread
    private void updateDeviceList() {
        synchronized (lock) {
            BluetoothDevice connected = null;

            // If we are connected to some device, but want to connect to another (or disconnect
            // completely), then we should disconnect all other devices first.
            for (BluetoothDevice device : hidDeviceProfile.getConnectedDevices()) {
                if (device.equals(waitingForDevice) || device.equals(connectedDevice)) {
                    connected = device;
                } else {
                    Log.i(TAG, "updateDeviceList: disconnecting device: " + device.getName());
                    hidDeviceProfile.disconnect(device);
                    SystemClock.sleep(50);
                }
            }

            // If there is nothing going on, and we want to connect, then do it.
            List<BluetoothDevice> connectionStateDevices = hidDeviceProfile.getDevicesMatchingConnectionStates(
                        new int[] {
                                BluetoothProfile.STATE_CONNECTED,
                                BluetoothProfile.STATE_CONNECTING,
                                BluetoothProfile.STATE_DISCONNECTING
                        });
            if (connectionStateDevices.isEmpty() && waitingForDevice != null) {
                hidDeviceProfile.connect(waitingForDevice);
            } else if(waitingForDevice == null) {
                Log.i(TAG, "updateDeviceList: waitingForDevice is null");
            } else {
                Log.i(TAG, "updateDeviceList: getDevicesMatchingConnectionStates is not empty: " );
                for (BluetoothDevice device : connectionStateDevices) {
                    Log.i(TAG, "updateDeviceList: still connected device: " + device.getName());
                }
            }

            if (connectedDevice == null && connected != null) {
                connectedDevice = connected;
                waitingForDevice = null;
            } else if (connectedDevice != null && connected == null) {
                connectedDevice = null;
            }
            hidDeviceApp.setDevice(connectedDevice);
        }
    }

}
