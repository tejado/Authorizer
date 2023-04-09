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
import android.content.Context;

import kotlin.NotImplementedError;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.NoSuchElementException;


@SuppressLint({"MissingPermission", "NewApi"})
public class OutputBluetoothKeyboard implements OutputInterface
{
    private static final String TAG = "OutputBluetoothKeyboard";

    private UsbHidKbd kbdKeyInterpreter;

    public OutputBluetoothKeyboard(Language lang, Context ctx)
    {
        setLanguage(lang);
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


    @Override
    public int sendText(String text)
    {
        throw new NotImplementedError();
    }

    @Override
    public int sendReturn()
    {
        throw new NotImplementedError();
    }

    @Override
    public int sendTabulator()
    {
        throw new NotImplementedError();
    }

    @Override
    public void destruct() throws Exception {}

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
}
