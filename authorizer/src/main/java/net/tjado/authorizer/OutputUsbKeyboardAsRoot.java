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
import java.io.IOException;
import java.io.ByteArrayOutputStream;
import java.util.NoSuchElementException;

public class OutputUsbKeyboardAsRoot implements OutputInterface
{

    private String devicePath = "/dev/hidg0";
    private UsbHidKbd kbdKeyInterpreter;

    private static final String TAG = "OutputUsbKeyboardAsRoot";

    public OutputUsbKeyboardAsRoot(Language lang)
    {
        if( !ExecuteAsRootUtil.canRunRootCommands() ) {
            throw new SecurityException("Root access rejected!");
        }

        setLanguage(lang);
    }

    public void destruct()  {}

    public boolean setLanguage(Language lang) {

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

    public int sendText(String output) throws IOException
    {
        int ret = 0;
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();

        for (int i = 0; i < output.length(); i++) {
            String textCharString = String.valueOf(output.charAt(i) );

            try {
                byte[] scancode;
                scancode = kbdKeyInterpreter.getScancode(textCharString);
                Utilities.dbginfo(TAG, "'" + textCharString + "' > " + Utilities
                        .bytesToHex(scancode) );

                outputStream.write( scancode );
                // overwriting the last keystroke, otherwise it will be repeated until the next writing
                // and it would not be possible to repeat the keystroke
                outputStream.write( kbdKeyInterpreter.getScancode(null) );

            }
            catch (NoSuchElementException e) {
                Utilities.dbginfo(TAG,  "'" + textCharString + "' mapping not found" );
                ret = 1;
            }
        }

        writeToUsbDevice(outputStream.toByteArray());

        return ret;
    }

    private boolean writeToUsbDevice(byte[] scancodesBytes)
    {
        String scancodeHex = Utilities.bytesToHex(scancodesBytes);
        return writeToUsbDevice(scancodeHex);
    }

    private boolean writeToUsbDevice(String scancodesHex)
    {
        //String command = String.format("echo %s | xxd -r -p > %s\n", scancodesHex, devicePath);

        // Using printf instead of echo with xxd - this should provide a better compability
        scancodesHex = scancodesHex.replaceAll("(.{2})", "\\\\x$1");;
        String command = String.format("printf '%s' > %s\n", scancodesHex, devicePath);

        Utilities.dbginfo(TAG,  "Handing over to ExecuteAsRootUtil -> " + command );
        boolean cr = ExecuteAsRootUtil.execute( command );

        if (cr) {
            Utilities.dbginfo(TAG,  "Command execution successful");
        } else {
            Utilities.dbginfo(TAG,  "Command execution failed");
        }

        return cr;
    }

    public int sendSingleKey(String keyName) throws IOException
    {
        byte[] scancode;
        int ret = 0;

        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            scancode = kbdKeyInterpreter.getScancode(keyName);
            Utilities.dbginfo(TAG, "'" + keyName + "' > " + Utilities
                    .bytesToHex(scancode) );

            outputStream.write( scancode );
            // overwriting the last keystroke, otherwise it will be repeated until the next writing
            // and it would not be possible to repeat the keystroke
            outputStream.write( kbdKeyInterpreter.getScancode(null) );

            writeToUsbDevice(outputStream.toByteArray());
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
        return sendSingleKey("tab");
    }


    public void sendScancode(byte[] output) throws FileNotFoundException,
                                                   IOException
    {
        if( output.length == 8) {
            Utilities.dbginfo(TAG, Utilities.bytesToHex(output) );
            writeToUsbDevice(output);
        } else if (output.length == 1) {
            byte[] scancode = new byte[] {0x00, 0x00, output[0], 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00};

            Utilities.dbginfo(TAG, Utilities.bytesToHex(scancode) );
            writeToUsbDevice(scancode);
        }

    }

}
