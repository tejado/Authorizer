package net.tjado.bluetooth;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothDevice;
import android.util.Log;

import androidx.core.util.Preconditions;

import net.tjado.passwdsafe.lib.Utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@SuppressLint({"RestrictedApi", "MissingPermission"})
public final class BluetoothDeviceWrapper {
    private static final String TAG = "BluetoothDeviceWrapper";

    final private BluetoothDevice device;

    final private String devName;
    final private String devAddress;
    public String devString;
    private String devType;
    private boolean isDefault = false;


    public BluetoothDeviceWrapper(BluetoothDevice device) {
        this(device, BluetoothDeviceListing.HID_UNKNOWN_HOST);
    }

    public BluetoothDeviceWrapper(BluetoothDevice device, String type) {
        Preconditions.checkNotNull(device);

        this.device = device;
        devType = type;
        devName = device.getName();
        devAddress = device.getAddress();
        String devMajorDeviceClass = Integer.toString(device.getBluetoothClass().getMajorDeviceClass());
        String devDeviceClass = Integer.toString(device.getBluetoothClass().getDeviceClass());
        devString = devName + devAddress + devMajorDeviceClass + devDeviceClass;
    }

    public void setDefault(boolean isDefault) {
        this.isDefault = isDefault;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setType(String type) {
        devType = type;
    }

    public String getType() {
        return devType;
    }

    public BluetoothDevice getDevice() {
        return device;
    }

    public String getName() {
        return devName;
    }

    public String getAddress() {
        return devAddress;
    }

    @Override
    public boolean equals(Object obj) {

        if (obj == null || obj.getClass() != this.getClass()) {
            return false;
        }

        final BluetoothDeviceWrapper other = (BluetoothDeviceWrapper) obj;
        if ((this.devName == null) ? (other.getName() != null) : !this.devName.equals(other.getName())) {
            return false;
        }

        return this.devAddress.equals(other.getAddress());
    }

    public String getHash() {
        String devHash;
        try {
            MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
            messageDigest.update(devString.getBytes());
            devHash = Utils.bytesToHexString(messageDigest.digest());
        } catch (NoSuchAlgorithmException e) {
            Log.d(TAG, "Failed to use SHA-256 for hashing - using literal representation");
            devHash = devString;
        }

        return devHash;
    }
}