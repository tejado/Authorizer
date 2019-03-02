/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.authorizer;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

public class OutputUsbKeyboard implements OutputInterface
{

    protected String devicePath = "/dev/hidg0";
    protected FileOutputStream device;
    UsbHidKbd kbdKeyInterpreter;

    private static final String TAG = "OutputUsbKeyboard";

    public OutputUsbKeyboard(OutputInterface.Language lang) throws IOException
    {
        setLanguage(lang);

        openDevice();
    }

    public void destruct()  {
        closeDevice();
    }

    public boolean setLanguage(OutputInterface.Language lang) {

        String className = "net.tjado.authorizer.UsbHidKbd_" + lang;

        try {
            kbdKeyInterpreter = (UsbHidKbd) Class.forName(className).newInstance();
            Utilities.dbginfo(TAG, "Set language " + lang);
            return true;
        }
        catch (Exception e) {
            Utilities.dbginfo(TAG, "Language " + lang + " not found");
            kbdKeyInterpreter = new UsbHidKbd_en_US();
            return false;
        }

    }

    private void openDevice() throws IOException
    {
        device = new FileOutputStream(devicePath, true);
    }

    private void closeDevice() {
        try {
            if (device != null) {
                device.close();
            }
        } catch (Exception e) {}
    }

    private void clean() throws IOException
    {
        // overwriting the last keystroke, otherwise it will be repeated until the next writing
        // and it would not be possible to repeat the keystroke
        byte[] scancode_reset = kbdKeyInterpreter.getScancode(null);
        Utilities.dbginfo(TAG, "RST > " + Utilities.bytesToHex(scancode_reset));
        device.write(scancode_reset);
    }


    public int sendText(String output) throws IOException
    {

        byte[] scancode;
        int ret = 0;

        for (int i = 0; i < output.length(); i++) {
            String textCharString = String.valueOf(output.charAt(i) );

            try {
                scancode = kbdKeyInterpreter.getScancode(textCharString);
                Utilities.dbginfo(TAG, "'" + textCharString + "' > " + Utilities
                        .bytesToHex(scancode) );

                device.write(scancode);
                clean();
            }
            catch (NoSuchElementException e) {
                Utilities.dbginfo(TAG,  "'" + textCharString + "' mapping not found" );
                ret = 1;
            }
        }

        return ret;
    }

    public int sendSingleKey(String keyName) throws IOException
    {

        byte[] scancode;
        int ret = 0;

        try {
            scancode = kbdKeyInterpreter.getScancode(keyName);
            Utilities.dbginfo(TAG, "'" + keyName + "' > " + Utilities
                    .bytesToHex(scancode) );

            device.write(scancode);
            clean();
        }
        catch (NoSuchElementException e) {
            Utilities.dbginfo(TAG,  "'" + keyName + "' mapping not found" );
            ret = 1;
        }

        return ret;
    }

    public int sendReturn() throws IOException
    {
        return sendSingleKey("return");
    }

    public int sendTabulator() throws IOException
    {
        return sendSingleKey("tabulator");
    }


    public void sendScancode(byte[] output) throws FileNotFoundException,
                                                   IOException
    {

        if( output.length == 8) {
            Utilities.dbginfo(TAG, Utilities.bytesToHex(output) );
            device.write(output);

            clean();
        } else if (output.length == 1) {
            byte[] scancode = new byte[] {0x00, 0x00, output[0], 0x00, 0x00, 0x00, 0x00, 0x00};

            Utilities.dbginfo(TAG, Utilities.bytesToHex(scancode) );
            device.write(scancode);

            clean();
        }

    }

}
