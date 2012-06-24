/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.util.ArrayList;
import java.util.List;

import android.text.TextUtils;

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
    // TODO HEX_DIGITS exclusivity
    // TODO: defaults?

    /**
     * Constructor
     */
    public PasswdPolicy(String name)
    {
        itsName = name;
    }

    /** Get the policy name */
    public String getName()
    {
        return itsName;
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
        itsLength = minmaxLength(length);
    }

    /** Get the minimum number of lowercase characters */
    public int getMinLowercase()
    {
        return itsMinLowercase;
    }

    /** Set the minimum number of lowercase characters */
    public void setMinLowercase(int length)
    {
        itsMinLowercase = minmaxLength(length);
    }

    /** Get the minimum number of uppercase characters */
    public int getMinUppercase()
    {
        return itsMinUppercase;
    }

    /** Set the minimum number of uppercase characters */
    public void setMinUppercase(int length)
    {
        itsMinUppercase = minmaxLength(length);
    }

    /** Get the minimum number of digit characters */
    public int getMinDigits()
    {
        return itsMinDigits;
    }

    /** Set the minimum number of digit characters */
    public void setMinDigits(int length)
    {
        itsMinDigits = minmaxLength(length);
    }

    /** Get the minimum number of symbol characters */
    public int getMinSymbols()
    {
        return itsMinSymbols;
    }

    /** Set the minimum number of symbol characters */
    public void setMinSymbols(int length)
    {
        itsMinSymbols = minmaxLength(length);
    }

    /** Get the special symbols */
    public String getSpecialSymbols()
    {
        return itsSpecialSymbols;
    }

    /** Set the special symbols */
    public void setSpecialSymbols(String symbols)
    {
        itsSpecialSymbols = symbols;
    }

    /** Parse policies from the header named policies field */
    public static List<PasswdPolicy> parseHdrPolicies(String policyStr)
        throws IllegalArgumentException, NumberFormatException
    {
        List<PasswdPolicy> policies = null;
        if (TextUtils.isEmpty(policyStr)) {
            return policies;
        }

        int policyLen = policyStr.length();
        if (policyLen < 2) {
            throw new IllegalArgumentException(
                "Policies length (" + policyLen + ") too short: 2");
        }

        int numPolicies = Integer.parseInt(policyStr.substring(0, 2), 16);
        policies = new ArrayList<PasswdPolicy>(numPolicies);
        int policyStart = 2;
        int fieldStart = policyStart;
        for (int i = 0; i < numPolicies; ++i, policyStart = fieldStart) {
            int nameLen = getPolicyStrInt(policyStr, i, fieldStart, 2,
                                          "name length");
            fieldStart += 2;

            String name = getPolicyStrField(policyStr, i, fieldStart, nameLen,
                                            "name");
            fieldStart += nameLen;
            PasswdPolicy policy = new PasswdPolicy(name);
            policies.add(policy);

            int flags = getPolicyStrInt(policyStr, i, fieldStart, 4, "flags");
            fieldStart += 4;
            policy.setFlags(flags);

            int pwLen = getPolicyStrInt(policyStr, i, fieldStart, 3,
                                        "password length");
            fieldStart += 3;
            policy.setLength(pwLen);

            int minLower = getPolicyStrInt(policyStr, i, fieldStart, 3,
                                           "min lowercase chars");
            fieldStart += 3;
            policy.setMinLowercase(minLower);

            int minUpper = getPolicyStrInt(policyStr, i, fieldStart, 3,
                                           "min uppercase chars");
            fieldStart += 3;
            policy.setMinUppercase(minUpper);

            int minDigits = getPolicyStrInt(policyStr, i, fieldStart, 3,
                                            "min digit chars");
            fieldStart += 3;
            policy.setMinDigits(minDigits);

            int minSymbols = getPolicyStrInt(policyStr, i, fieldStart, 3,
                                             "min symbol chars");
            fieldStart += 3;
            policy.setMinSymbols(minSymbols);

            int numSpecials = getPolicyStrInt(policyStr, i, fieldStart, 2,
                                              "special symbols length");
            fieldStart += 2;
            String specialSyms = getPolicyStrField(policyStr, i, fieldStart,
                                                   numSpecials,
                                                   "special symbols");
            fieldStart += numSpecials;
            policy.setSpecialSymbols(specialSyms);
        }

        if (policyStart != policyLen) {
            throw new IllegalArgumentException(
                "Policies field does not end at the last policy");
        }

        return policies;
    }

    /** Get an integer from a hexidecimal policy field */
    private static int getPolicyStrInt(String policyStr,
                                       int policyNum,
                                       int fieldStart,
                                       int fieldLen,
                                       String fieldName)
        throws IllegalArgumentException
    {
        return Integer.parseInt(getPolicyStrField(policyStr, policyNum,
                                                  fieldStart, fieldLen,
                                                  fieldName), 16);
    }

    /** Get a field from a policy string */
    private static String getPolicyStrField(String policyStr,
                                            int policyNum,
                                            int fieldStart,
                                            int fieldLen,
                                            String fieldName)
        throws IllegalArgumentException
    {
        if (policyStr.length() < fieldStart + fieldLen) {
            throw new IllegalArgumentException(
                "Policy " + policyNum + " too short for " +
                    fieldName + ": " + fieldLen);
        }
        return policyStr.substring(fieldStart, fieldStart + fieldLen);
    }

    /** Constrain a length from 0 to LENGTH_MAX */
    private static final int minmaxLength(int length)
    {
        return Math.min(Math.max(length, 0), LENGTH_MAX);
    }
}
