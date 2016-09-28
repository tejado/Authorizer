/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.authorizer;


import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;

/**
 * Created by tm on 21.04.16.
 */
public class OutputKeyboard implements OutputInterface
{

    // Logger
    private static Log log = Log.getInstance();

    protected String devicePath = "/dev/hidg0";
    protected FileOutputStream device;
    UsbHidKbd kbdKeyInterpreter;

    public OutputKeyboard(OutputInterface.Language lang) throws IOException
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
            log.debug("Set language " + lang);
            return true;
        }
        catch (Exception e) {
            log.debug("Language " + lang + " not found");
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
        log.debug("RST > " + ToolBox.bytesToHex(scancode_reset));
        device.write(scancode_reset);
    }


    public void sendText(String output) throws IOException
    {

        byte[] scancode;

        for (int i = 0; i < output.length(); i++) {
            String textCharString = String.valueOf(output.charAt(i) );

            try {
                scancode = kbdKeyInterpreter.getScancode(textCharString);
                log.debug( "'" + textCharString + "' > " + ToolBox.bytesToHex(scancode) );

                device.write(scancode);
                clean();
            }
            catch (NoSuchElementException e) {
                log.debug( "'" + textCharString + "' mapping not found" );
            }
        }
    }

    public void sendScancode(byte[] output) throws FileNotFoundException,
                                                   IOException
    {

        if( output.length == 8) {
            log.debug( ToolBox.bytesToHex(output) );
            device.write(output);

            clean();
        } else if (output.length == 1) {
            byte[] scancode = new byte[] {0x00, 0x00, output[0], 0x00, 0x00, 0x00, 0x00, 0x00};

            log.debug( ToolBox.bytesToHex(scancode) );
            device.write(scancode);

            clean();
        }

    }

}
