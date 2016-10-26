/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.util;

import org.junit.Before;
import org.junit.Test;
import org.pwsafe.lib.file.Owner;

import java.io.Closeable;
import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertSame;

/**
 * Unit tests for Owner
 */
public class OwnerTest
{
    private Item itsOwnedItem;
    private Owner<Item> itsItem;

    public static class Item implements Closeable
    {
        public int itsCloseCount = 0;

        @Override
        public void close() throws IOException
        {
            if (itsCloseCount != 0) {
                throw new IOException("Multiple closes");
            }
            ++itsCloseCount;
        }
    }

    @Before
    public void createItem()
    {
        itsOwnedItem = new Item();
        itsItem = new Owner<>(itsOwnedItem);
        assertNotClosed();
    }

    @Test
    public void testCtor()
    {
        try {
            assertNotClosed();
        } finally {
            closeItem();
        }
    }

    @Test
    public void testMultiClose()
    {
        closeItem();
        itsItem.close();
        assertNull(itsItem.get());
    }

    @Test
    public void testNonUsedParam()
    {
        nonUsedMethod(itsItem.pass());
        assertNotClosed();
        closeItem();
    }

    @Test
    public void testUsedParam()
    {
        usedMethod(itsItem.pass());
        assertNotClosed();
        closeItem();
    }

    private void assertNotClosed()
    {
        assertNotNull(itsItem.get());
        assertSame(itsOwnedItem, itsItem.get());
        assertEquals(0, itsOwnedItem.itsCloseCount);
    }

    private void closeItem()
    {
        assertNotClosed();
        itsItem.close();
        assertNull(itsItem.get());
        assertEquals(1, itsOwnedItem.itsCloseCount);
    }

    private void nonUsedMethod(
            @SuppressWarnings("UnusedParameters") Owner<Item>.Param param)
    {
        assertNotClosed();
    }

    private void usedMethod(Owner<Item>.Param param)
    {
        Owner<Item> item = param.use();
        assertSame(item, itsItem);
        assertNotClosed();
        item.close();
        assertNotClosed();
    }
}
