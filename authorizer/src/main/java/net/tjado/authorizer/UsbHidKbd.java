/**
 * Authorizer
 *
 *  Copyright 2016 by Tjado Mäcke <tjado@maecke.de>
 *  Licensed under GNU General Public License 3.0.
 *
 * @license GPL-3.0 <https://opensource.org/licenses/GPL-3.0>
 */

package net.tjado.authorizer;

import java.util.HashMap;
import java.util.Map;
import java.util.NoSuchElementException;

public abstract class UsbHidKbd {

    // ToDo: replace byte with ByteArray... eveywhere
    protected Map<String, byte[]> kbdVal= new HashMap<String, byte[]>();

    public byte[] getScancode(String key) {
        byte[] value = (byte[]) kbdVal.get(key);

        if ( value == null ) {
            throw new NoSuchElementException("Scancode for '" + key + "' not found (" + this.kbdVal.size() + ")");
        }

        return value;
    }

}
