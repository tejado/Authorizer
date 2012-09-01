/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.jefftharris.passwdsafe.R;
import com.jefftharris.passwdsafe.util.Pair;

import android.content.Context;
import android.text.TextUtils;

/**
 * The PasswdPolicy class represents a password policy for a file or record
 */
public class PasswdPolicy implements Comparable<PasswdPolicy>
{
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

    public static final String SYMBOLS_DEFAULT = "+-=_@#$%^&;:,.<>/~\\[](){}?!|";
    public static final String SYMBOLS_EASY = "+-=_@#$%^&<>/~\\?";
    public static final String SYMBOLS_PRONOUNCE = "@&(#!|$+";

    private static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
    private static final String UPPER_CHARS = LOWER_CHARS.toUpperCase();
    private static final String DIGITS = "0123456789";
    private static final String EASY_LOWER_CHARS = "abcdefghijkmnopqrstuvwxyz";
    private static final String EASY_UPPER_CHARS = "ABCDEFGHJKLMNPQRTUVWXY";
    private static final String EASY_DIGITS = "346789";

    /** The location of the policy */
    public enum Location
    {
        DEFAULT         (0),
        HEADER          (1),
        RECORD_NAME     (2),
        RECORD          (3);

        public final int itsSortOrder;

        private Location(int sortOrder)
        {
            itsSortOrder = sortOrder;
        }
    }

    /** Policy fields for a record */
    public static class RecordPolicyStrs
    {
        public final String itsPolicyName;
        public final String itsPolicyStr;
        public final String itsOwnSymbols;

        public RecordPolicyStrs(String policyName,
                                String policyStr,
                                String ownSymbols)
        {
            itsPolicyName = policyName;
            itsPolicyStr = policyStr;
            itsOwnSymbols = ownSymbols;
        }
    }

    // TODO: UTF-8 chars in policy name and special chars

    /** Type of policy.  String indexes must match policy_type strings. */
    public enum Type
    {
        NORMAL          (0),
        EASY_TO_READ    (1),
        PRONOUNCEABLE   (2),
        HEXADECIMAL     (3);

        private Type(int strIdx)
        {
            itsStrIdx = strIdx;
        }

        public final int itsStrIdx;

        /** Get the type from the string index */
        public static Type fromStrIdx(int idx)
        {
            for (Type t: values()) {
                if (idx == t.itsStrIdx) {
                    return t;
                }
            }
            return NORMAL;
        }
    }


    private final String itsName;
    private final Location itsLocation;
    private int itsFlags = FLAG_USE_LOWERCASE | FLAG_USE_UPPERCASE |
                           FLAG_USE_DIGITS | FLAG_USE_SYMBOLS;
    private int itsLength = 12;
    private int itsMinLowercase = 1;
    private int itsMinUppercase = 1;
    private int itsMinDigits = 1;
    private int itsMinSymbols = 1;
    private String itsSpecialSymbols = null;

    /**
     * Constructor
     */
    public PasswdPolicy(String name, Location loc)
    {
        itsName = name;
        itsLocation = loc;
    }

    /** Copy constructor with a different name */
    public PasswdPolicy(String name, PasswdPolicy copy)
    {
        itsName = name;
        itsLocation = copy.itsLocation;
        itsFlags = copy.itsFlags;
        itsLength = copy.itsLength;
        itsMinLowercase = copy.itsMinLowercase;
        itsMinUppercase = copy.itsMinUppercase;
        itsMinDigits = copy.itsMinDigits;
        itsMinSymbols = copy.itsMinSymbols;
        itsSpecialSymbols = copy.itsSpecialSymbols;
    }

    /** Create a default policy */
    public static PasswdPolicy createDefaultPolicy(Context ctx)
    {
        return new PasswdPolicy(ctx.getString(R.string.default_policy),
                                PasswdPolicy.Location.DEFAULT);
    }

    /** Get the policy name */
    public String getName()
    {
        return itsName;
    }

    /** Get the location of the policy */
    public Location getLocation()
    {
        return itsLocation;
    }

    /** Get the policy flags */
    public int getFlags()
    {
        return itsFlags;
    }

    /** Check for the presence of flags */
    public final boolean checkFlags(int flags)
    {
        return (itsFlags & flags) == flags;
    }

    /** Set the policy flags */
    public void setFlags(int flags)
    {
        itsFlags = flags & FLAGS_VALID;
    }

    /** Get the type of policy */
    public Type getType()
    {
        if (checkFlags(FLAG_USE_EASY_VISION)) {
            return Type.EASY_TO_READ;
        } else if (checkFlags(FLAG_MAKE_PRONOUNCEABLE)) {
            return Type.PRONOUNCEABLE;
        } else if (checkFlags(FLAG_USE_HEX_DIGITS)) {
            return Type.HEXADECIMAL;
        }
        return Type.NORMAL;
    }

    /** Get the type of policy as a string */
    public static String getTypeStr(Type type, Context ctx)
    {
        return ctx.getResources().getStringArray(
            R.array.policy_type)[type.itsStrIdx];
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

    /** Generate a password */
    public String generate()
        throws NoSuchAlgorithmException
    {
        Type type = getType();
        ArrayList<String> chars = new ArrayList<String>();
        switch (type) {
        case NORMAL:
        case EASY_TO_READ: {
            boolean isEasy = (type == PasswdPolicy.Type.EASY_TO_READ);
            if (checkFlags(PasswdPolicy.FLAG_USE_LOWERCASE)) {
                chars.add(isEasy ? EASY_LOWER_CHARS : LOWER_CHARS);
            }
            if (checkFlags(PasswdPolicy.FLAG_USE_UPPERCASE)) {
                chars.add(isEasy ? EASY_UPPER_CHARS : UPPER_CHARS);
            }
            if (checkFlags(PasswdPolicy.FLAG_USE_DIGITS)) {
                chars.add(isEasy ? EASY_DIGITS: DIGITS);
            }
            if (checkFlags(PasswdPolicy.FLAG_USE_SYMBOLS)) {
                chars.add(isEasy ? PasswdPolicy.SYMBOLS_EASY :
                            PasswdPolicy.SYMBOLS_DEFAULT);
            }
            break;
        }
        case PRONOUNCEABLE: {
            // TODO: support pronounceable
            break;
        }
        case HEXADECIMAL: {
            chars.add(DIGITS + "abcdef");
            break;
        }
        }

        if (chars.isEmpty()) {
            return null;
        }

        String charsStr =
            TextUtils.concat(chars.toArray(new CharSequence[0])).toString();
        int numChars = charsStr.length();
        StringBuilder passwd = new StringBuilder();

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        random.nextBytes(new byte[itsLength]);

        ArrayList<String> verifyChars = new ArrayList<String>();
        do {
            verifyChars.clear();
            verifyChars.addAll(chars);
            passwd.delete(0, passwd.length());
            for (int i = 0; i < itsLength; ++i) {
                int charPos = random.nextInt(numChars);
                char c = charsStr.charAt(charPos);
                passwd.append(c);

                if (!verifyChars.isEmpty()) {
                    Iterator<String> iter = verifyChars.iterator();
                    while (iter.hasNext()) {
                        String verifyStr = iter.next();
                        if (verifyStr.indexOf(c) != -1) {
                            iter.remove();
                        }
                    }
                }
            }
        } while (!verifyChars.isEmpty() &&
            (itsLength > (chars.size() - verifyChars.size())));

        return passwd.toString();
    }

    /** Convert the object to a string */
    @Override
    public String toString()
    {
        return itsName;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(PasswdPolicy policy)
    {
        if (itsLocation != policy.itsLocation) {
            return (itsLocation.itsSortOrder < policy.itsLocation.itsSortOrder)
                ? -1 : 1;
        }
        return itsName.compareTo(policy.itsName);
    }

    /** Convert the object to a string formatted as a header policy */
    public String toHdrPolicyString()
    {
        StringBuilder str = new StringBuilder();
        str.append(String.format("%02x", itsName.length()));
        str.append(itsName);
        str.append(flagsAndLengthsToString());
        if (itsSpecialSymbols == null) {
            str.append("00");
        } else {
            str.append(String.format("%02x", itsSpecialSymbols.length()));
            str.append(itsSpecialSymbols);
        }
        return str.toString();
    }

    /** Parse a header policy from a string */
    public static Pair<PasswdPolicy, Integer> parseHdrPolicy(String policyStr,
                                                             int pos,
                                                             int policyNum,
                                                             Location loc)
        throws IllegalArgumentException, NumberFormatException
    {
        int fieldStart = pos;
        int nameLen = getPolicyStrInt(policyStr, policyNum, fieldStart, 2,
                                      "name length");
        fieldStart += 2;

        String name = getPolicyStrField(policyStr, policyNum, fieldStart,
                                        nameLen, "name");
        fieldStart += nameLen;
        PasswdPolicy policy = new PasswdPolicy(name, loc);

        fieldStart = parsePolicyFlagsAndLengths(policy, policyStr,
                                                policyNum, fieldStart);

        int numSpecials = getPolicyStrInt(policyStr, policyNum, fieldStart, 2,
                                          "special symbols length");
        fieldStart += 2;
        String specialSyms = null;
        if (numSpecials > 0) {
            specialSyms = getPolicyStrField(policyStr, policyNum, fieldStart,
                                            numSpecials, "special symbols");
            fieldStart += numSpecials;
        }
        policy.setSpecialSymbols(specialSyms);

        return new Pair<PasswdPolicy, Integer>(policy, fieldStart);
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
            Pair<PasswdPolicy, Integer> rc = parseHdrPolicy(policyStr,
                                                            fieldStart, i,
                                                            Location.HEADER);
            policies.add(rc.first);
            fieldStart = rc.second;
        }

        if (policyStart != policyLen) {
            throw new IllegalArgumentException(
                "Policies field does not end at the last policy");
        }

        return policies;
    }

    /** Convert the header policies to a string */
    public static String hdrPoliciesToString(List<PasswdPolicy> policies)
    {
        if (policies == null) {
            return null;
        }

        StringBuilder str = new StringBuilder();
        str.append(String.format("%02x", policies.size()));
        for (PasswdPolicy policy: policies) {
            str.append(policy.toHdrPolicyString());
        }
        return str.toString();
    }

    /** Parse a record's policy from its fields */
    public static PasswdPolicy parseRecordPolicy(String policyName,
                                                 String policyStr,
                                                 String ownSymbols)
    {
        PasswdPolicy policy = null;
        if (policyName != null) {
            policy = new PasswdPolicy(policyName, Location.RECORD_NAME);
        } else if (policyStr != null) {
            policy = new PasswdPolicy(null, Location.RECORD);
            int endPos = parsePolicyFlagsAndLengths(policy, policyStr, 0, 0);
            if (endPos != policyStr.length()) {
                throw new IllegalArgumentException(
                    "Password policy too long: " + policyStr);
            }
            policy.setSpecialSymbols(ownSymbols);
        }
        return policy;
    }

    /** Convert a record policy to its string fields */
    public static RecordPolicyStrs recordPolicyToString(PasswdPolicy policy)
    {
        if (policy == null) {
            return null;
        }
        switch (policy.getLocation()) {
        case DEFAULT:
        case HEADER: {
            return null;
        }
        case RECORD_NAME: {
            return new RecordPolicyStrs(policy.getName(), null, null);
        }
        case RECORD: {
            return new RecordPolicyStrs(null,
                                        policy.flagsAndLengthsToString(),
                                        policy.getSpecialSymbols());
        }
        }
        return null;
    }

    /** Parse the flags and lengths of a policy from a string */
    private static int parsePolicyFlagsAndLengths(PasswdPolicy policy,
                                                  String policyStr,
                                                  int policyNum,
                                                  int fieldStart)

    {
        int flags = getPolicyStrInt(policyStr, policyNum, fieldStart, 4,
                                    "flags");
        fieldStart += 4;
        policy.setFlags(flags);

        int pwLen = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                    "password length");
        fieldStart += 3;
        policy.setLength(pwLen);

        int minDigits = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                        "min digit chars");
        fieldStart += 3;
        policy.setMinDigits(minDigits);

        int minLower = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                       "min lowercase chars");
        fieldStart += 3;
        policy.setMinLowercase(minLower);

        int minSymbols = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                         "min symbol chars");
        fieldStart += 3;
        policy.setMinSymbols(minSymbols);

        int minUpper = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                       "min uppercase chars");
        fieldStart += 3;
        policy.setMinUppercase(minUpper);

        return fieldStart;
    }

    /** Convert the policy's flags and lengths to a string */
    private String flagsAndLengthsToString()
    {
        return String.format("%04x%03x%03x%03x%03x%03x",
                             itsFlags, itsLength, itsMinDigits, itsMinLowercase,
                             itsMinSymbols, itsMinUppercase);
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
