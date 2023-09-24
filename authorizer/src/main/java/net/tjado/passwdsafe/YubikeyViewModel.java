/*
 * Copyright (©) 2023 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.pm.PackageManager;
import android.nfc.NfcAdapter;
import android.os.Handler;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import net.tjado.passwdsafe.lib.ManagedRef;
import net.tjado.passwdsafe.lib.PasswdSafeUtil;
import net.tjado.passwdsafe.util.YubiState;
import com.yubico.yubikit.android.YubiKitManager;
import com.yubico.yubikit.android.transport.nfc.NfcConfiguration;
import com.yubico.yubikit.android.transport.nfc.NfcNotAvailable;
import com.yubico.yubikit.android.transport.usb.UsbConfiguration;
import com.yubico.yubikit.android.transport.usb.UsbYubiKeyDevice;
import com.yubico.yubikit.core.YubiKeyDevice;

/**
 * View model for a YubiKey
 */
public class YubikeyViewModel extends AndroidViewModel
{
    public static final int KEY_TIMEOUT = 30 * 1000;

    // Also decide proper log level in logback.xml
    public static final boolean TEST = false;//PasswdSafeUtil.DEBUG;

    private static final long NFC_STOP_DELAY = 5 * 1000;

    private static final String TAG = "YubikeyViewModel";

    /**
     * NFC state
     */
    private enum NfcState
    {
        UNAVAILABLE,
        DISABLED,
        ENABLED
    }

    private final YubiKitManager itsYubiMgr;
    private NfcState itsNfcState;
    private final boolean itsHasUsb;
    private final Handler itsUiHandler = new Handler(Looper.getMainLooper());
    private ManagedRef<Activity> itsStopAct;
    private final MutableLiveData<YubiKeyDevice> itsYubiDevice =
            new MutableLiveData<>();

    /**
     * Constructor
     */
    public YubikeyViewModel(@NonNull Application app)
    {
        super(app);
        itsYubiMgr = new YubiKitManager(app);
        itsNfcState = getNfcState(app);

        var pkgmgr = app.getPackageManager();
        itsHasUsb = pkgmgr.hasSystemFeature(PackageManager.FEATURE_USB_HOST);
        if (itsHasUsb) {
            itsYubiMgr.startUsbDiscovery(new UsbConfiguration(), device -> {
                PasswdSafeUtil.dbginfo(TAG, "USB discovery, device: %s",
                        toString(device));
                if (!device.hasPermission()) {
                    return;
                }

                device.setOnClosed(() -> {
                    PasswdSafeUtil.dbginfo(TAG, "USB device removed");
                    itsYubiDevice.postValue(null);
                });

                itsYubiDevice.postValue(device);
            });
        }
    }

    /**
     * Get the state of support for the YubiKey
     */
    public YubiState getState(Context ctx)
    {
        itsNfcState = getNfcState(ctx);

        if (TEST) {
            return YubiState.ENABLED;
        }

        switch (itsNfcState) {
            case UNAVAILABLE: {
                return itsHasUsb ? YubiState.USB_ENABLED_NFC_UNAVAILABLE :
                        YubiState.UNAVAILABLE;
            }
            case DISABLED: {
                return itsHasUsb ? YubiState.USB_ENABLED_NFC_DISABLED :
                        YubiState.USB_DISABLED_NFC_DISABLED;
            }
            case ENABLED: {
                return itsHasUsb ? YubiState.ENABLED :
                        YubiState.USB_DISABLED_NFC_ENABLED;
            }
        }
        return YubiState.UNAVAILABLE;
    }

    /**
     * Get the live YubiKey device
     */
    @NonNull
    public LiveData<YubiKeyDevice> getDeviceData()
    {
        return itsYubiDevice;
    }

    /**
     * Is the current YubiKey device a USB device
     */
    public boolean isUsbYubikeyDevice()
    {
        return isUsbYubikey(itsYubiDevice.getValue());
    }

    /**
     * Start using NFC to discover a YubiKey
     */
    public void startNfc(@NonNull Activity act)
    {
        if (isNfcEnabled()) {
            try {
                itsYubiMgr.startNfcDiscovery(
                        new NfcConfiguration().timeout(KEY_TIMEOUT), act,
                        device -> {
                            PasswdSafeUtil.dbginfo(TAG,
                                    "NFC discover, device: %s",
                                    toString(device));

                            itsUiHandler.post(() -> {
                                itsYubiDevice.setValue(device);
                                itsYubiDevice.postValue(null);
                            });
                        });
            } catch (NfcNotAvailable e) {
                PasswdSafeUtil.dbginfo(TAG, e, "NFC discovery failed");
            }
        }
    }

    /**
     * Stop using NFC to discover a YubiKey
     */
    public void stopNfc(@NonNull Activity act)
    {
        if (isNfcEnabled()) {
            // Delay stopping NFC for a few seconds to allow the user time to
            // move the key away from the device.  Otherwise, the key may
            // activate a second time and load the default activity.
            if (itsStopAct != null) {
                itsStopAct.clear();
            }
            itsStopAct = new ManagedRef<>(act);

            itsUiHandler.postDelayed(() -> {
                PasswdSafeUtil.dbginfo(TAG, "stopNfc stopping");
                if (itsStopAct != null) {
                    var stopAct = itsStopAct.get();
                    if (stopAct != null) {
                        itsYubiMgr.stopNfcDiscovery(stopAct);
                        itsStopAct.clear();
                    }
                }
            }, NFC_STOP_DELAY);
        }
    }

    /**
     * Is a YubiKey device a USB YubiKey device
     */
    public static boolean isUsbYubikey(YubiKeyDevice device)
    {
        return (device instanceof UsbYubiKeyDevice);
    }

    /**
     * Get a string identifier for a YubiKey device
     */
    public static String toString(YubiKeyDevice device)
    {
        if (device == null) {
            return "(null)";
        } else if (device instanceof UsbYubiKeyDevice) {
            // The default USB device toString is too verbose
            var usbdevice = (UsbYubiKeyDevice)device;
            var dev = usbdevice.getUsbDevice();
            var pid = usbdevice.getPid();
            return "UsbYubiKeyDevice{" + "usbDevice=[UsbDevice[name=" +
                    dev.getDeviceName() + ", vendor=" + dev.getVendorId() +
                    ", product=" + dev.getProductId() + "], usbPid=" + pid + '}';
        }
        return device.toString();
    }

    @Override
    protected void onCleared()
    {
        PasswdSafeUtil.dbginfo(TAG, "onCleared");
        itsYubiDevice.setValue(null);
        if (itsHasUsb) {
            itsYubiMgr.stopUsbDiscovery();
        }
        if (itsStopAct != null) {
            itsStopAct.clear();
        }
    }

    /**
     * Is NFC enabled
     */
    private boolean isNfcEnabled()
    {
        switch (itsNfcState) {
            case ENABLED: {
                return true;
            }
            case UNAVAILABLE:
            case DISABLED: {
                break;
            }
        }
        return false;
    }

    /**
     * Get the NFC state
     */
    private static NfcState getNfcState(Context ctx)
    {
        NfcAdapter adapter = NfcAdapter.getDefaultAdapter(ctx);
        if (adapter != null) {
            return adapter.isEnabled() ? NfcState.ENABLED : NfcState.DISABLED;
        } else {
            return NfcState.UNAVAILABLE;
        }
    }
}
