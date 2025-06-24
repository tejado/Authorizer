/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.util;

/**
 * The CountedBool class is initially false.  It maintains its value until the
 * count of updates of the opposite value exceeds the updates of the current
 * value.
 */
public final class CountedBool
{
    private int itsCount = 0;

    /**
     * Change of state from an update
     */
    public enum StateChange
    {
        TRUE,
        FALSE,
        SAME
    }

    /**
     * Update the state of the boolean
     * @return Whether the boolean changed state
     */
    public StateChange update(boolean bool)
    {
        if (bool && (++itsCount == 1)) {
            return StateChange.TRUE;
        } else if (!bool && (--itsCount == 0)) {
            return StateChange.FALSE;
        }
        return StateChange.SAME;
    }

    /**
     * Get the current value
     */
    public boolean get()
    {
        return (itsCount > 0);
    }
}
