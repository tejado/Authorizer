/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import net.tjado.passwdsafe.util.Pair;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import android.text.TextUtils;

/**
 * The PasswdPolicy class represents a password policy for a file or record
 */
public class PasswdPolicy implements Comparable<PasswdPolicy>, Parcelable
{
    public static final int FLAG_USE_LOWERCASE          = 0x8000;
    public static final int FLAG_USE_UPPERCASE          = 0x4000;
    public static final int FLAG_USE_DIGITS             = 0x2000;
    public static final int FLAG_USE_SYMBOLS            = 0x1000;
    public static final int FLAG_USE_HEX_DIGITS         = 0x0800;
    public static final int FLAG_USE_EASY_VISION        = 0x0400;
    public static final int FLAG_MAKE_PRONOUNCEABLE     = 0x0200;
    private static final int FLAGS_VALID                 = 0xffff;

    /** Maximum value for length fields */
    private static final int LENGTH_MAX = 4095;

    public static final String SYMBOLS_DEFAULT = "+-=_@#$%^&;:,.<>/~\\[](){}?!|";
    public static final String SYMBOLS_EASY = "+-=_@#$%^&<>/~\\?";
    public static final String SYMBOLS_PRONOUNCE = "@&(#!|$+";

    private static String PREFS_DEFAULT_SYMBOLS = null;

    public static final String LOWER_CHARS = "abcdefghijklmnopqrstuvwxyz";
    public static final String UPPER_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String DIGITS = "0123456789";
    public static final String HEX_DIGITS = DIGITS + "abcdef";
    public static final String EASY_LOWER_CHARS = "abcdefghijkmnopqrstuvwxyz";
    public static final String EASY_UPPER_CHARS = "ABCDEFGHJKLMNPQRTUVWXY";
    public static final String EASY_DIGITS = "346789";

    /**
     * Parcelable CREATOR instance
     */
    public static final Parcelable.Creator<PasswdPolicy> CREATOR =
            new Parcelable.Creator<PasswdPolicy>()
            {
                @Override
                public PasswdPolicy createFromParcel(Parcel source)
                {
                    return new PasswdPolicy(source);
                }

                @Override
                public PasswdPolicy[] newArray(int size)
                {
                    return new PasswdPolicy[size];
                }
            };

    /** The location of the policy */
    public enum Location
    {
        DEFAULT         (0),
        HEADER          (1),
        RECORD_NAME     (2),
        RECORD          (3);

        public final int itsSortOrder;

        Location(int sortOrder)
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

        Type(int strIdx)
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
    private static Random getRandom()
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

    // 'Leet' digits that can replace a character
    private static final char[] LEETS_DIGITS = {
        '4', '8', 0, 0, // a, b, c, d
        '3', 0, '6', 0, // e, f, g, h
        '1', 0, 0, '1', // i, j, k, l
        0, 0, '0', 0,   // m, n, o, p
        0, 0, '5', '7', // q, r, s, t
        0, 0, 0, 0,     // u, v, w, x
        0, '2'          // y, z
    };

    // 'Leet' symbols that can replace a character
    private static final char[] LEETS_SYMBOLS = {
        '@', '&', '(', 0,       // a, b, c, d
        0, 0, 0, '#',           // e, f, g, h
        '!', 0, 0, '|',         // i, j, k, l
        0, 0, 0, 0,             // m, n, o, p
        0, 0, '$', '+',         // q, r, s, t
        0, 0, 0, 0,             // u, v, w, x
        0, 0                    // y, z
    };

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

    /**
     * Constructor from a parcel
     */
    private PasswdPolicy(Parcel source)
    {
        itsName = source.readString();
        itsLocation = Location.valueOf(source.readString());
        itsFlags = source.readInt();
        itsLength = source.readInt();
        itsMinLowercase = source.readInt();
        itsMinUppercase = source.readInt();
        itsMinDigits = source.readInt();
        itsMinSymbols = source.readInt();
        itsSpecialSymbols = source.readString();
    }

    /** Create a default policy */
    public static PasswdPolicy createDefaultPolicy(Context ctx)
    {
        return new PasswdPolicy(ctx.getString(net.tjado.passwdsafe.R.string.default_policy),
                                PasswdPolicy.Location.DEFAULT);
    }

    /** Create a default policy with custom flags and length */
    public static PasswdPolicy createDefaultPolicy(Context ctx,
                                                   int flags, int length)
    {
        return new PasswdPolicy(ctx.getString(net.tjado.passwdsafe.R.string.default_policy),
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
                net.tjado.passwdsafe.R.array.policy_type)[type.itsStrIdx];
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

            String syms;
            if (itsSpecialSymbols == null) {
                if (TextUtils.isEmpty(PREFS_DEFAULT_SYMBOLS)) {
                    syms = SYMBOLS_DEFAULT;
                } else {
                    syms = PREFS_DEFAULT_SYMBOLS;
                }
            } else {
                syms = itsSpecialSymbols;
            }
            addRandomChars(FLAG_USE_SYMBOLS, itsMinSymbols,
                           syms, passwd, allchars);
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
            // Pronounceable generated differently
            return generatePronounceable();
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
    public int compareTo(@NonNull PasswdPolicy policy)
    {
        if (itsLocation != policy.itsLocation) {
            return (itsLocation.itsSortOrder < policy.itsLocation.itsSortOrder)
                ? -1 : 1;
        }
        return itsName.compareTo(policy.itsName);
    }

    /** Are the policies equal for the fields used by a record policy */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public boolean recordPolicyEquals(PasswdPolicy policy)
    {
        return ((itsFlags == policy.itsFlags) &&
                (itsLength == policy.itsLength) &&
                (itsMinLowercase == policy.itsMinLowercase) &&
                (itsMinUppercase == policy.itsMinUppercase) &&
                (itsMinDigits == policy.itsMinDigits) &&
                (itsMinSymbols == policy.itsMinSymbols) &&
                TextUtils.equals(itsSpecialSymbols, policy.itsSpecialSymbols));
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

    @Override
    public int describeContents()
    {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags)
    {
        dest.writeString(itsName);
        dest.writeString(itsLocation.name());
        dest.writeInt(itsFlags);
        dest.writeInt(itsLength);
        dest.writeInt(itsMinLowercase);
        dest.writeInt(itsMinUppercase);
        dest.writeInt(itsMinDigits);
        dest.writeInt(itsMinSymbols);
        dest.writeString(itsSpecialSymbols);
    }

    /** Parse a header policy from a string */
    public static Pair<PasswdPolicy, Integer> parseHdrPolicy(String policyStr,
                                                             int pos,
                                                             int policyNum,
                                                             Location loc)
        throws IllegalArgumentException
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
        return new Pair<>(policy, fieldStart);
    }

    /** Parse policies from the header named policies field */
    public static List<PasswdPolicy> parseHdrPolicies(String policyStr)
        throws IllegalArgumentException
    {
        if (TextUtils.isEmpty(policyStr)) {
            return null;
        }

        int policyLen = policyStr.length();
        if (policyLen < 2) {
            throw new IllegalArgumentException(
                "Policies length (" + policyLen + ") too short: 2");
        }

        int numPolicies = Integer.parseInt(policyStr.substring(0, 2), 16);
        List<PasswdPolicy> policies = new ArrayList<>(numPolicies);
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
            if (endPos > policyStr.length()) {
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

    /** Set the default symbols from user preferences */
    public static void setPrefsDefaultSymbols(String symbols)
    {
        PREFS_DEFAULT_SYMBOLS = symbols;
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
    private static int minmaxLength(int length)
    {
        return Math.min(Math.max(length, 0), LENGTH_MAX);
    }

    /** Generate a pronounceable password */
    private String generatePronounceable()
    {
        // Pronounceable passwords generation code copied from
        // CPasswordCharPool::MakePronounceable from Password Safe project

        /**
         * Following based on gpw.C from
         * http://www.multicians.org/thvv/tvvtools.html
         * Thanks to Tom Van Vleck, Morrie Gasser, and Dan Edwards.
         */
        int c1, c2, c3;  /* array indices */
        int sumfreq;      /* total frequencies[c1][c2][*] */
        int ranno;        /* random number in [0,sumfreq] */
        int sum;          /* running total of frequencies */
        int nchar;        /* number of chars in password so far */

        char[] password = new char[itsLength];

        /* Pick a random starting point. */
        /* (This cheats a little; the statistics for three-letter
           combinations beginning a word are different from the stats
           for the general population.  For example, this code happily
           generates "mmitify" even though no word in my dictionary
           begins with mmi. So what.) */
        sumfreq = Trigram.SIGMA;  // sigma calculated by loadtris
        ranno = itsRandom.nextInt(sumfreq+1); // Weight by sum of frequencies
        sum = 0;
        nchar = 0;
        for (c1 = 0; c1 < 26; c1++) {
            for (c2 = 0; c2 < 26; c2++) {
                for (c3 = 0; c3 < 26; c3++) {
                    sum += Trigram.TRIS[c1][c2][c3];
                    if (sum > ranno) { // Pick first value
                        if (itsLength > 0) {
                            password[0] = LOWER_CHARS.charAt(c1);
                            ++nchar;
                            if (itsLength > 1) {
                                password[1] = LOWER_CHARS.charAt(c2);
                                ++nchar;
                                if (itsLength > 2) {
                                    password[2] = LOWER_CHARS.charAt(c3);
                                    ++nchar;
                                }
                            }
                        }
                        c1 = c2 = c3 = 26; // Break all loops.
                    } // if sum
                } // for c3
            } // for c2
        } // for c1

        /* Do a random walk. */
        while (nchar < itsLength) {
            c1 = password[nchar-2] - 'a'; // Take the last 2 chars
            c2 = password[nchar-1] - 'a'; // .. and find the next one.
            sumfreq = 0;
            for (c3 = 0; c3 < 26; c3++) {
                sumfreq += Trigram.TRIS[c1][c2][c3];
            }
            /* Note that sum < duos[c1][c2] because
             duos counts all digraphs, not just those
             in a trigraph. We want sum. */
            /* Choose a continuation. */
            if (sumfreq == 0) { // If there is no possible extension..
                ranno = 0;
            } else {
                 // Weight by sum of frequencies
                ranno = itsRandom.nextInt(sumfreq+1);
            }
            sum = 0;
            for (c3 = 0; c3 < 26; c3++) {
                sum += Trigram.TRIS[c1][c2][c3];
                if (sum > ranno) {
                    password[nchar++] = LOWER_CHARS.charAt(c3);
                    break;
                }
            } // for c3
        } // while nchar

        /*
         * password now has an all-lowercase pronounceable password
         * We now want to modify it per policy:
         * If use digits and/or use symbols, replace some chars with
         * corresponding 'leet' values
         * Also enforce use upper case & use lower case policies
         */

        boolean useSymbols = checkFlags(FLAG_USE_SYMBOLS);
        boolean useDigits = checkFlags(FLAG_USE_DIGITS);
        if (useSymbols || useDigits) {
            // fill a vector with indices of substitution candidates
            ArrayList<Integer> sc = new ArrayList<>(password.length);
            for (int i = 0; i < password.length; ++i) {
                int idx = password[i] - 'a';
                if ((useDigits && (LEETS_DIGITS[idx] != 0)) ||
                    (useSymbols && (LEETS_SYMBOLS[idx] != 0))) {
                    sc.add(i);
                }
            }

            int sclen = sc.size();
            if (sclen > 0) {
                // choose how many to replace (not too many, but at least one)
                int rn = 1;
                if (sclen > 1) {
                    rn += itsRandom.nextInt(sclen - 1)/2;
                }
                // replace some of them
                Collections.shuffle(sc, itsRandom);
                for (int i = 0; i < rn; ++i) {
                    int pwIdx = sc.get(i);
                    int leetIdx = password[pwIdx] - 'a';
                    char digsub = useDigits ? LEETS_DIGITS[leetIdx] : 0;
                    char symsub = useSymbols ? LEETS_SYMBOLS[leetIdx] : 0;

                    // if both substitutions possible, select one randomly
                    if ((digsub != 0) && (symsub != 0) &&
                        itsRandom.nextBoolean()) {
                        digsub = 0;
                    }
                    password[pwIdx] = (digsub != 0) ? digsub : symsub;
                }
            }
        }

        // case
        boolean useLower = checkFlags(FLAG_USE_LOWERCASE);
        boolean useUpper = checkFlags(FLAG_USE_UPPERCASE);
        if (!useLower && useUpper) {
            for (int i = 0; i < itsLength; i++) {
                if (Character.isLowerCase(password[i])) {
                    password[i] = Character.toUpperCase(password[i]);
                }
            }
        } else if (useLower && useUpper) { // mixed case
            for (int i = 0; i < itsLength; ++i) {
                if (Character.isLetter(password[i]) &&
                    itsRandom.nextBoolean()) {
                    password[i] = Character.toUpperCase(password[i]);
                }
            }
        }

        return new String(password);
    }

    /** Randomly add a number of characters to the password and add the
     *  character set to the returned all list if the flags match */
    private void addRandomChars(int flag, int numChars, String chars,
                                StringBuilder passwd, StringBuilder allchars) {
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
