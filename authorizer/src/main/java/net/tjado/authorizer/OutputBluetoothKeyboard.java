/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.authorizer;


import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.os.SystemClock;
import android.util.Log;

import net.tjado.bluetooth.HidDataSender;
import net.tjado.bluetooth.HidDeviceProfile;

import androidx.annotation.MainThread;
import kotlin.NotImplementedError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Set;



@SuppressLint({"MissingPermission", "NewApi"})
public class OutputBluetoothKeyboard implements OutputInterface
{
    private static final String TAG = "OutputBluetoothKeyboard";

    private UsbHidKbd kbdKeyInterpreter;
    private Context context;

    private BluetoothAdapter btAdapter;
    HidDataSender hidDataSender;
    HidDeviceProfile hidDeviceProfile;
    BluetoothDevice connectedDevice = null;
    AuthorizerProfileListener profileListener;

    private String btOutputString;
    private byte[] btOutputBytes;


    public OutputBluetoothKeyboard(Language lang, Context ctx)
    {
        setLanguage(lang);
        context = ctx;

        BluetoothManager bluetoothManager = (BluetoothManager) context.getSystemService(Context.BLUETOOTH_SERVICE);
        btAdapter = bluetoothManager.getAdapter();
    }

    public void destruct()
    {
        deinitializeBluetoothHidDevice();
    }

    public boolean setLanguage(Language lang)
    {
        String className = "net.tjado.authorizer.UsbHidKbd_" + lang;
        try {
            kbdKeyInterpreter = (UsbHidKbd)Class.forName(className).newInstance();
            Utilities.dbginfo(TAG, "Set language " + lang);
            return true;
        } catch (Exception e) {
            Utilities.dbginfo(TAG, "Language " + lang + " not found");
            kbdKeyInterpreter = new UsbHidKbd_en_US();
            return false;
        }
    }

    public boolean checkBluetoothStatus()
    {
        if (btAdapter != null) {
            return btAdapter.isEnabled();
        }

        return false;
    }

    public void initializeBluetoothHidDevice()
    {
        Utilities.dbginfo(TAG, "initializeBluetoothHidDevice");

        profileListener = new AuthorizerProfileListener();
        hidDataSender = HidDataSender.getInstance();
        hidDeviceProfile = hidDataSender.register(context, profileListener);
    }

    public void deinitializeBluetoothHidDevice()
    {
        Utilities.dbginfo(TAG, "deinitializeBluetoothHidDevice");

        if(hidDataSender != null) {
            hidDataSender.unregister(context, profileListener);
        }

        profileListener = null;
    }


    @SuppressLint("MissingPermission")
    public Set<BluetoothDevice> getBondedDevices()
    {
        if (btAdapter != null) {
            return btAdapter.getBondedDevices();
        }

        return new HashSet<>();
    }


    public void connectDeviceAndSend(BluetoothDevice device, byte[] output)
    {
        Utilities.dbginfo(TAG, "connectDeviceAndSend: " + device);

        connectedDevice = device;

        if ((output.length % 8) != 0) {
            Utilities.dbginfo(TAG, "connectDeviceAndSend: MOD ERROR: " + output.length);
            return;
        }
        btOutputBytes = output;

        if(hidDataSender != null && hidDataSender.isConnected()) {
            send();
            return;
        }

        if(hidDataSender != null) {
            deinitializeBluetoothHidDevice();
            SystemClock.sleep(100);
        }

        initializeBluetoothHidDevice();
    }

    private void clean() throws IOException
    {
        // overwriting the last keystroke, otherwise it will be repeated until the next writing
        // and it would not be possible to repeat the keystroke
        byte[] scancode_reset = kbdKeyInterpreter.getScancode(null);
        Utilities.dbginfo(TAG, "RST > " + Utilities.bytesToHex(scancode_reset));
        hidDataSender.sendScancode(scancode_reset);
    }

    public byte[] convertTextToScancode(String text) {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (int i = 0; i < text.length(); i++) {
            String textCharString = String.valueOf(text.charAt(i) );

            try {
                byte[] scancode = kbdKeyInterpreter.getScancode(textCharString);
                Utilities.dbginfo(TAG, "convertTextToScancode: '" + textCharString + "' > " + Utilities
                        .bytesToHex(scancode) );

                outputStream.write( scancode );
            }
            catch (NoSuchElementException e) {
                Utilities.dbginfo(TAG,  "'" + textCharString + "' mapping not found" );
            } catch (IOException e) {
                e.printStackTrace();
                Utilities.dbginfo(TAG,  "convertText:" + e.getLocalizedMessage() );
            }
        }

        return outputStream.toByteArray();
    }

    private void send() {
        Log.w(TAG, "send");

        Utilities.dbginfo(TAG, "send");
        try
        {
            if (btOutputBytes != null) {

                int blockSize = 8;
                int blockCount = btOutputBytes.length / blockSize;

                int start = 0;
                for (int i = 0; i < blockCount; i++) {
                    byte[] scancode = Arrays.copyOfRange(btOutputBytes, start, start + blockSize);
                    Utilities.dbginfo(TAG, "send: " + Utilities.bytesToHex(scancode) );

                    hidDataSender.sendScancode(scancode);
                    clean();
                    start += blockSize;
                }

                btOutputBytes = null;
            }
        } catch (IOException e) {
            Utilities.dbginfo(TAG, "send error: " + e.getLocalizedMessage());
            e.printStackTrace();
        }
    }

    @Override
    public int sendText(String text) throws Exception
    {
        throw new NotImplementedError();
    }

    @Override
    public int sendReturn() throws Exception
    {
        throw new NotImplementedError();
    }

    @Override
    public int sendTabulator() throws Exception
    {
        throw new NotImplementedError();
    }

    public byte[] getSingleKey(String keyName) {
        try {
            return kbdKeyInterpreter.getScancode(keyName);
        }
        catch (NoSuchElementException e) {
            Utilities.dbginfo(TAG,  "'" + keyName + "' mapping not found" );
        }

        return new byte[0];
    }

    public byte[] getReturn() {
        return getSingleKey("return");
    }

    public byte[] getTabulator() {
        return getSingleKey("tabulator");
    }


    private final class AuthorizerProfileListener implements HidDataSender.ProfileListener {

        private static final String TAG = "AuthorizerProfileLsnr";

        @Override
        @MainThread
        public void onServiceStateChanged(BluetoothProfile proxy) {}

        @Override
        @MainThread
        public void onConnectionStateChanged(BluetoothDevice device, int state) {
            Log.w(TAG, "onConnectionStateChanged - " + device + ": " + state);

            if (state == BluetoothProfile.STATE_CONNECTED) {
                Log.i(TAG, "onConnectionStateChanged: CONNECTED");

                SystemClock.sleep(100);
                send();
                SystemClock.sleep(500);
                deinitializeBluetoothHidDevice();
            }
        }

        @Override
        @MainThread
        public void onAppStatusChanged(boolean registered) {
            Log.i(TAG, "onAppStatusChanged: " + registered);

            if (registered) {
                SystemClock.sleep(100);
                hidDataSender.requestConnect(connectedDevice);
            }
        }
    }
}
