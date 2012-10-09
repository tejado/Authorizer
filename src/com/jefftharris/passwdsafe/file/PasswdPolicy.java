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
import java.util.List;
import java.util.Random;

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

    public static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER_CHARS = LOWER_CHARS.toUpperCase();
    public static final String DIGITS = "0123456789";
    public static final String HEX_DIGITS = DIGITS + "abcdef";
    public static final String EASY_LOWER_CHARS = "abcdefghijkmnopqrstuvwxyz";
    public static final String EASY_UPPER_CHARS = "ABCDEFGHJKLMNPQRTUVWXY";
    public static final String EASY_DIGITS = "346789";

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
    private final int itsFlags;
    private final int itsLength;
    private final int itsMinLowercase;
    private final int itsMinUppercase;
    private final int itsMinDigits;
    private final int itsMinSymbols;
    private final String itsSpecialSymbols;

    private static final Random itsRandom = getRandom();
    private static final Random getRandom()
    {
        Random random;
        try {
            random = SecureRandom.getInstance("SHA1PRNG");
        } catch (NoSuchAlgorithmException e) {
            random = new SecureRandom();
        }
        random.nextBytes(new byte[32]);
        return random;
    }

    /**
     * Constructor
     */
    public PasswdPolicy(String name, Location loc)
    {
        this(name, loc,
             FLAG_USE_LOWERCASE | FLAG_USE_UPPERCASE |
             FLAG_USE_DIGITS | FLAG_USE_SYMBOLS,
             12, 1, 1, 1, 1, null);
    }

    /** Constructor with fields */
    public PasswdPolicy(String name, Location loc, int flags, int length,
                        int minLower, int minUpper, int minDigits, int minSyms,
                        String specialSymbols)
    {
        itsName = name;
        itsLocation = loc;
        itsFlags = flags & FLAGS_VALID;
        itsLength = minmaxLength(length);
        itsMinLowercase = minmaxLength(minLower);
        itsMinUppercase = minmaxLength(minUpper);
        itsMinDigits = minmaxLength(minDigits);
        itsMinSymbols = minmaxLength(minSyms);
        itsSpecialSymbols = specialSymbols;
    }

    /** Copy constructor with a different name */
    public PasswdPolicy(String name, PasswdPolicy copy)
    {
        this(name, copy.itsLocation, copy.itsFlags, copy.itsLength,
             copy.itsMinLowercase, copy.itsMinUppercase,
             copy.itsMinDigits, copy.itsMinSymbols, copy.itsSpecialSymbols);
    }

    /** Create a default policy */
    public static PasswdPolicy createDefaultPolicy(Context ctx)
    {
        return new PasswdPolicy(ctx.getString(R.string.default_policy),
                                PasswdPolicy.Location.DEFAULT);
    }

    /** Create a default policy with custom flags and length */
    public static PasswdPolicy createDefaultPolicy(Context ctx,
                                                   int flags, int length)
    {
        return new PasswdPolicy(ctx.getString(R.string.default_policy),
                                PasswdPolicy.Location.DEFAULT,
                                flags, length, 1, 1, 1, 1, null);
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

    /** Get the minimum number of lowercase characters */
    public int getMinLowercase()
    {
        return itsMinLowercase;
    }

    /** Get the minimum number of uppercase characters */
    public int getMinUppercase()
    {
        return itsMinUppercase;
    }

    /** Get the minimum number of digit characters */
    public int getMinDigits()
    {
        return itsMinDigits;
    }

    /** Get the minimum number of symbol characters */
    public int getMinSymbols()
    {
        return itsMinSymbols;
    }

    /** Get the special symbols */
    public String getSpecialSymbols()
    {
        return itsSpecialSymbols;
    }

    /** Generate a password */
    public String generate()
    {
        // Fill the password with the minimum number of required
        // characters
        StringBuilder passwd = new StringBuilder();
        StringBuilder allchars = new StringBuilder();
        Type type = getType();
        switch (type) {
        case NORMAL: {
            addRandomChars(FLAG_USE_LOWERCASE, itsMinLowercase,
                           LOWER_CHARS, passwd, allchars);
            addRandomChars(FLAG_USE_UPPERCASE, itsMinUppercase,
                           UPPER_CHARS, passwd, allchars);
            addRandomChars(FLAG_USE_DIGITS, itsMinDigits,
                           DIGITS, passwd, allchars);
            addRandomChars(FLAG_USE_SYMBOLS, itsMinSymbols,
                           (itsSpecialSymbols == null) ?
                               SYMBOLS_DEFAULT : itsSpecialSymbols,
                           passwd, allchars);
            break;
        }
        case EASY_TO_READ: {
            addRandomChars(FLAG_USE_LOWERCASE, itsMinLowercase,
                           EASY_LOWER_CHARS, passwd, allchars);
            addRandomChars(FLAG_USE_UPPERCASE, itsMinUppercase,
                           EASY_UPPER_CHARS, passwd, allchars);
            addRandomChars(FLAG_USE_DIGITS, itsMinDigits,
                           EASY_DIGITS, passwd, allchars);
            addRandomChars(FLAG_USE_SYMBOLS, itsMinSymbols,
                           (itsSpecialSymbols == null) ?
                               SYMBOLS_EASY: itsSpecialSymbols,
                           passwd, allchars);
            break;
        }
        case HEXADECIMAL: {
            allchars.append(HEX_DIGITS);
            break;
        }
        case PRONOUNCEABLE: {
            // TODO: support pronounceable
            break;
        }
        }

        // Fill the rest with all of the usable characters
        int allLen = allchars.length();
        for (int i = passwd.length(); i < itsLength; ++i) {
            int rand = itsRandom.nextInt(allLen);
            passwd.append(allchars.charAt(rand));
        }

        // Shuffle the characters
        for (int i = passwd.length(); i > 1; --i) {
            int rand = itsRandom.nextInt(i);
            char c = passwd.charAt(i - 1);
            passwd.setCharAt(i - 1, passwd.charAt(rand));
            passwd.setCharAt(rand, c);
        }

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

    /** Are the policies equal for the fields used by a record policy */
    public boolean recordPolicyEquals(PasswdPolicy policy)
    {
        if ((itsFlags != policy.itsFlags) ||
            (itsLength != policy.itsLength) ||
            (itsMinLowercase != policy.itsMinLowercase) ||
            (itsMinUppercase != policy.itsMinUppercase) ||
            (itsMinDigits != policy.itsMinDigits) ||
            (itsMinSymbols != policy.itsMinSymbols) ||
            !TextUtils.equals(itsSpecialSymbols, policy.itsSpecialSymbols)) {
            return false;
        }
        return true;
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

        ParsedFields fields = parsePolicyFlagsAndLengths(policyStr,
                                                         policyNum, fieldStart);
        fieldStart = fields.itsFieldsEnd;

        int numSpecials = getPolicyStrInt(policyStr, policyNum, fieldStart, 2,
                                          "special symbols length");
        fieldStart += 2;
        String specialSyms = null;
        if (numSpecials > 0) {
            specialSyms = getPolicyStrField(policyStr, policyNum, fieldStart,
                                            numSpecials, "special symbols");
            fieldStart += numSpecials;
        }

        PasswdPolicy policy =
            new PasswdPolicy(name, loc, fields.itsFlags, fields.itsLength,
                             fields.itsMinLowercase, fields.itsMinUppercase,
                             fields.itsMinDigits, fields.itsMinSymbols,
                             specialSyms);
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
            ParsedFields fields = parsePolicyFlagsAndLengths(policyStr, 0, 0);
            int endPos = fields.itsFieldsEnd;
            if (endPos != policyStr.length()) {
                throw new IllegalArgumentException(
                    "Password policy too long: " + policyStr);
            }
            policy =
                new PasswdPolicy(null, Location.RECORD,
                                 fields.itsFlags, fields.itsLength,
                                 fields.itsMinLowercase, fields.itsMinUppercase,
                                 fields.itsMinDigits, fields.itsMinSymbols,
                                 ownSymbols);
        }
        return policy;
    }

    /** Convert a policy to the string fields for a record */
    public static RecordPolicyStrs recordPolicyToString(PasswdPolicy policy)
    {
        if (policy == null) {
            return null;
        }
        switch (policy.getLocation()) {
        case DEFAULT: {
            return null;
        }
        case HEADER:
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

    /** Fields parsed from a policy flags and length string */
    private static class ParsedFields
    {
        public final int itsFlags;
        public final int itsLength;
        public final int itsMinLowercase;
        public final int itsMinUppercase;
        public final int itsMinDigits;
        public final int itsMinSymbols;
        public final int itsFieldsEnd;

        public ParsedFields(int flags, int pwLen, int minLower, int minUpper,
                            int minDigits, int minSymbols, int fieldsEnd)
        {
            itsFlags = flags;
            itsLength = pwLen;
            itsMinLowercase = minLower;
            itsMinUppercase = minUpper;
            itsMinDigits = minDigits;
            itsMinSymbols = minSymbols;
            itsFieldsEnd = fieldsEnd;
        }
    }

    /** Parse the flags and lengths of a policy from a string */
    private static ParsedFields parsePolicyFlagsAndLengths(String policyStr,
                                                           int policyNum,
                                                           int fieldStart)

    {
        int flags = getPolicyStrInt(policyStr, policyNum, fieldStart, 4,
                                    "flags");
        fieldStart += 4;

        int pwLen = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                    "password length");
        fieldStart += 3;

        int minDigits = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                        "min digit chars");
        fieldStart += 3;

        int minLower = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                       "min lowercase chars");
        fieldStart += 3;

        int minSymbols = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                         "min symbol chars");
        fieldStart += 3;

        int minUpper = getPolicyStrInt(policyStr, policyNum, fieldStart, 3,
                                       "min uppercase chars");
        fieldStart += 3;

        return new ParsedFields(flags, pwLen, minLower, minUpper,
                                minDigits, minSymbols, fieldStart);
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

    /** Randomly add a number of characters to the password and add the
     *  character set to the returned all list if the flags match */
    private final void addRandomChars(int flag,
                                      int numChars,
                                      String chars,
                                      StringBuilder passwd,
                                      StringBuilder allchars)
    {
        if (checkFlags(flag)) {
            int charsLen = chars.length();
            for (int i = 0; i < numChars; ++i) {
                int rand = itsRandom.nextInt(charsLen);
                passwd.append(chars.charAt(rand));
            }
            allchars.append(chars);
        }
    }
}
