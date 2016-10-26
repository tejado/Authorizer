/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import net.tjado.passwdsafe.util.CountedBool;

import org.junit.Before;
import org.junit.Test;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;

/**
 * Unit tests for CountedBool
 */
public class CountedBoolTest
{
    private CountedBool itsBool;

    @Before
    public void createBool()
    {
        itsBool = new CountedBool();
    }

    @Test
    public void testDefault()
    {
        assertFalse(itsBool.get());
    }

    @Test
    public void testUpdateTrueBasic()
    {
        assertEquals(CountedBool.StateChange.TRUE, itsBool.update(true));
        assertTrue(itsBool.get());
        assertEquals(CountedBool.StateChange.FALSE, itsBool.update(false));
        assertFalse(itsBool.get());
    }

    @Test
    public void testUpdateTrueMulti()
    {
        assertEquals(CountedBool.StateChange.TRUE, itsBool.update(true));
        assertTrue(itsBool.get());
        for (int i = 0; i < 10; ++i) {
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(true));
            assertTrue(itsBool.get());
        }
        for (int i = 0; i < 10; ++i) {
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(false));
            assertTrue(itsBool.get());
        }
        assertEquals(CountedBool.StateChange.FALSE, itsBool.update(false));
        assertFalse(itsBool.get());
    }

    @Test
    public void testUpdateTrueMultiMixed()
    {
        assertEquals(CountedBool.StateChange.TRUE, itsBool.update(true));
        assertTrue(itsBool.get());
        for (int i = 0; i < 10; ++i) {
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(true));
            assertTrue(itsBool.get());
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(false));
            assertTrue(itsBool.get());
        }
        assertEquals(CountedBool.StateChange.FALSE, itsBool.update(false));
        assertFalse(itsBool.get());
    }

    @Test
    public void testUpdateFalseBasic()
    {
        assertFalse(itsBool.get());
        assertEquals(CountedBool.StateChange.SAME, itsBool.update(false));
        assertFalse(itsBool.get());
        assertEquals(CountedBool.StateChange.SAME, itsBool.update(true));
        assertFalse(itsBool.get());
        assertEquals(CountedBool.StateChange.TRUE, itsBool.update(true));
        assertTrue(itsBool.get());
    }

    @Test
    public void testUpdateFalseMulti()
    {
        assertFalse(itsBool.get());
        for (int i = 0; i < 10; ++i) {
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(false));
            assertFalse(itsBool.get());
        }
        for (int i = 0; i < 10; ++i) {
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(true));
            assertFalse(itsBool.get());
        }
        assertEquals(CountedBool.StateChange.TRUE, itsBool.update(true));
        assertTrue(itsBool.get());
    }

    @Test
    public void testUpdateFalseMultiMixed()
    {
        assertFalse(itsBool.get());
        for (int i = 0; i < 10; ++i) {
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(false));
            assertFalse(itsBool.get());
            assertEquals(CountedBool.StateChange.SAME, itsBool.update(true));
            assertFalse(itsBool.get());
        }
        assertEquals(CountedBool.StateChange.TRUE, itsBool.update(true));
        assertTrue(itsBool.get());
    }
}
