/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

/**
 * The PasswdPolicy class represents a password policy for a file or record
 */
public class PasswdPolicy
{
    private final String itsName;
    private int itsFlags;
    private int itsLength;
    private int itsMinLowercase;
    private int itsMinUppercase;
    private int itsMinDigits;
    private int itsMinSymbols;
    private String itsSpecialSymbols = null;

    public static final int FLAG_USE_LOWERCASE          = 0x8000;
    public static final int FLAG_USE_UPPERCASE          = 0x4000;
    public static final int FLAG_USE_DIGITS             = 0x2000;
    public static final int FLAG_USE_SYMBOLS            = 0x1000;
    public static final int FLAG_USE_HEX_DIGITS         = 0x0800;
    public static final int FLAG_USE_EASY_VISION        = 0x0400;
    public static final int FLAG_MAKE_PRONOUNCEABLE     = 0x0200;
    public static final int FLAGS_VALID                 = 0xffff;

    /** Maximum value for length fields */
    public static final int LENGTH_MAX = 4095;

    // TODO: Support pronounceable passwords
    // TODO: defaults?

    /**
     * Constructor
     */
    public PasswdPolicy(String name)
    {
        itsName = name;
    }

    /** Get the policy flags */
    public int getFlags()
    {
        return itsFlags;
    }

    /** Set the policy flags */
    public void setFlags(int flags)
    {
        itsFlags = flags & FLAGS_VALID;
    }

    /** Get the password length */
    public int getLength()
    {
        return itsLength;
    }

    /** Set the password length */
    public void setLength(int length)
    {
        itsLength = Math.min(Math.max(length, 0), LENGTH_MAX);
    }
}
