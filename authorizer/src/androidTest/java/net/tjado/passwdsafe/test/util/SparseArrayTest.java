/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import androidx.collection.LongSparseArray;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;


/**
 * Verification tests for sparse arrays
 */
public class SparseArrayTest
{
    private LongSparseArray<Long> itsArray;

    @Before
    public void setUp()
    {
        itsArray = new LongSparseArray<>();
        itsArray.put(1, 1L);
        itsArray.put(3, 3L);
        itsArray.put(5, 5L);
        itsArray.put(7, 7L);
        itsArray.put(9, 9L);
    }

    @Test
    public void testIterRemoveAll()
    {
        for (int i = itsArray.size() - 1; i >= 0; --i) {
            itsArray.removeAt(i);
        }

        assertEquals(0, itsArray.size());
    }

    @Test
    public void testIterRemoveFirst()
    {
        for (int i = itsArray.size() - 1; i >= 0; --i) {
            if ((itsArray.keyAt(i) == 1) || (itsArray.keyAt(i) == 3)) {
                itsArray.removeAt(i);
            }
        }

        assertEquals(3, itsArray.size());
        assertEquals(5, itsArray.keyAt(0));
        assertEquals(7, itsArray.keyAt(1));
        assertEquals(9, itsArray.keyAt(2));
    }

    @Test
    public void testIterRemoveMid()
    {
        for (int i = itsArray.size() - 1; i >= 0; --i) {
            if ((itsArray.keyAt(i) == 3) || (itsArray.keyAt(i) == 7)) {
                itsArray.removeAt(i);
            }
        }

        assertEquals(3, itsArray.size());
        assertEquals(1, itsArray.keyAt(0));
        assertEquals(5, itsArray.keyAt(1));
        assertEquals(9, itsArray.keyAt(2));
    }

    @Test
    public void testIterRemoveLast()
    {
        for (int i = itsArray.size() - 1; i >= 0; --i) {
            if ((itsArray.keyAt(i) == 7) || (itsArray.keyAt(i) == 9)) {
                itsArray.removeAt(i);
            }
        }

        assertEquals(3, itsArray.size());
        assertEquals(1, itsArray.keyAt(0));
        assertEquals(3, itsArray.keyAt(1));
        assertEquals(5, itsArray.keyAt(2));
    }
}
