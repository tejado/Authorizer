/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import androidx.annotation.CheckResult;
import androidx.annotation.NonNull;

import java.io.Closeable;
import java.io.IOException;

/**
 * The Owner class encapsulates an object to ensure it is closed after all
 * users are finished with it
 */
public final class Owner<T extends Closeable>
{
    private T itsItem;
    private int itsRefCount = 1;

    /**
     * Owner as a method parameter
     */
    public final class Param
    {
        private final Owner<T> itsOwnedItem;

        /**
         * Constructor
         */
        public Param(@NonNull Owner<T> item)
        {
            itsOwnedItem = item;
        }

        /**
         * Use the owner in the parameter.  Close must be called on the
         * returned instance
         */
        public @NonNull @CheckResult(suggest="#close()")
        Owner<T> use()
        {
            ++itsOwnedItem.itsRefCount;
            return itsOwnedItem;
        }
    }

    /**
     * Constructor to take ownership of an object
     */
    public Owner(@NonNull T item)
    {
        itsItem = item;
    }

    /**
     * Get the owned object
     */
    public @NonNull T get()
    {
        return itsItem;
    }

    /**
     * Pass the owner as a method parameter
     */
    public @NonNull Param pass()
    {
        return new Param(this);
    }

    /**
     * Close the owner and its owned object if the last user
     */
    public void close()
    {
        if (itsItem != null) {
            if (--itsRefCount <= 0) {
                try {
                    itsItem.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                itsItem = null;
            }
        }
    }

    /**
     * Finalize the object to check for missed calls to close
     */
    @Override
    protected void finalize() throws Throwable
    {
        super.finalize();
        if ((itsItem != null) && (itsRefCount > 0)) {
            Exception e = new Exception("NOT FINALIZED");
            e.printStackTrace();
        }
    }
}
