/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.test.file;

import java.util.Collections;
import java.util.List;

import com.jefftharris.passwdsafe.file.PasswdPolicy;

import android.test.AndroidTestCase;
import android.test.MoreAsserts;

/**
 * Tests for the PasswdPolicy class
 */
public class PasswdPolicyTest extends AndroidTestCase
{
    /** Constructor */
    public PasswdPolicyTest()
    {
        super();
    }

    /** Test an empty header policy */
    public void testHdrEmpty()
    {
        List<PasswdPolicy> policies;
        policies = PasswdPolicy.parseHdrPolicies(null);
        assertNull(policies);
        policies = PasswdPolicy.parseHdrPolicies("");
        assertNull(policies);

        String str = PasswdPolicy.hdrPoliciesToString(null);
        assertNull(str);
    }

    /** Test zero header policies */
    public void testHdrZero()
    {
        doTestBadHdrPolicy("0", "Policies length (1) too short: 2");

        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies("00");
        MoreAsserts.assertEmpty(policies);
        String str = PasswdPolicy.hdrPoliciesToString(policies);
        assertEquals("00", str);

        doTestBadHdrPolicy("000",
                           "Policies field does not end at the last policy");
    }

    /** Test one valid header policy */
    public void testHdrOneValid()
    {
        String policiesStr = "0107Policy1fe00abc111aaa000fff03!@#";
        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies(policiesStr);
        assertEquals(1, policies.size());
        PasswdPolicy policy = policies.get(0);
        assertEquals("Policy1", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS |
                     PasswdPolicy.FLAG_USE_HEX_DIGITS |
                     PasswdPolicy.FLAG_USE_EASY_VISION |
                     PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE, policy.getFlags());
        assertEquals(0xabc, policy.getLength());
        assertEquals(0x111, policy.getMinDigits());
        assertEquals(0xaaa, policy.getMinLowercase());
        assertEquals(0x000, policy.getMinSymbols());
        assertEquals(0xfff, policy.getMinUppercase());
        assertEquals("!@#", policy.getSpecialSymbols());

        assertEquals(policiesStr, PasswdPolicy.hdrPoliciesToString(policies));
    }

    /** Test a default header policy */
    public void testHdrDefault()
    {
        PasswdPolicy policy = new PasswdPolicy("policy1",
                                               PasswdPolicy.Location.HEADER);
        assertEquals("policy1", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS, policy.getFlags());
        assertEquals(12, policy.getLength());
        assertEquals(1, policy.getMinDigits());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(1, policy.getMinSymbols());
        assertEquals(1, policy.getMinUppercase());
        assertNull(policy.getSpecialSymbols());

        assertEquals("0107policy1f00000c00100100100100",
                     PasswdPolicy.hdrPoliciesToString(
                         Collections.singletonList(policy)));
    }

    /** Test multiple valid header policies */
    public void testHdrMultiValid()
    {
        String policiesStr = "050ceasy to readb40000a0010010010010008hex only08000140010010010010008policy 1f00000f0040020050030009pronounced200008001001001001000dspecial charsf00000d0030010040020a!@#$%^&*()";
        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies(policiesStr);
        assertEquals(5, policies.size());

        PasswdPolicy policy;
        policy = policies.get(0);
        assertEquals("easy to read", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS |
                     PasswdPolicy.FLAG_USE_EASY_VISION, policy.getFlags());
        assertEquals(10, policy.getLength());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(1, policy.getMinUppercase());
        assertEquals(1, policy.getMinDigits());
        assertEquals(1, policy.getMinSymbols());
        assertNull(policy.getSpecialSymbols());

        policy = policies.get(1);
        assertEquals("hex only", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_HEX_DIGITS, policy.getFlags());
        assertEquals(20, policy.getLength());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(1, policy.getMinUppercase());
        assertEquals(1, policy.getMinDigits());
        assertEquals(1, policy.getMinSymbols());
        assertNull(policy.getSpecialSymbols());

        policy = policies.get(2);
        assertEquals("policy 1", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS, policy.getFlags());
        assertEquals(15, policy.getLength());
        assertEquals(2, policy.getMinLowercase());
        assertEquals(3, policy.getMinUppercase());
        assertEquals(4, policy.getMinDigits());
        assertEquals(5, policy.getMinSymbols());
        assertNull(policy.getSpecialSymbols());

        policy = policies.get(3);
        assertEquals("pronounce", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_SYMBOLS |
                     PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE, policy.getFlags());
        assertEquals(8, policy.getLength());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(1, policy.getMinUppercase());
        assertEquals(1, policy.getMinDigits());
        assertEquals(1, policy.getMinSymbols());
        assertNull(policy.getSpecialSymbols());

        policy = policies.get(4);
        assertEquals("special chars", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS, policy.getFlags());
        assertEquals(13, policy.getLength());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(2, policy.getMinUppercase());
        assertEquals(3, policy.getMinDigits());
        assertEquals(4, policy.getMinSymbols());
        assertEquals("!@#$%^&*()", policy.getSpecialSymbols());
    }

    /** Test max valid header policies */
    public void testHdrMaxValid()
    {
        StringBuilder policiesStr = new StringBuilder("ff");
        for (int i = 0; i < 255; ++i) {
            policiesStr.append(
                String.format("09Policy%03dfe00%03x%03x%03x%03x%03x03!@#",
                              i, i + 1, i + 2, i + 3, i + 4, i + 5));
        }
        List<PasswdPolicy> policies =
            PasswdPolicy.parseHdrPolicies(policiesStr.toString());
        assertEquals(255, policies.size());
        for (int i = 0; i < 255; ++i) {
            PasswdPolicy policy = policies.get(i);
            assertEquals(String.format("Policy%03d", i), policy.getName());
            assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
            assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                         PasswdPolicy.FLAG_USE_UPPERCASE |
                         PasswdPolicy.FLAG_USE_DIGITS |
                         PasswdPolicy.FLAG_USE_SYMBOLS |
                         PasswdPolicy.FLAG_USE_HEX_DIGITS |
                         PasswdPolicy.FLAG_USE_EASY_VISION |
                         PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE, policy.getFlags());
            assertEquals(i + 1, policy.getLength());
            assertEquals(i + 2, policy.getMinDigits());
            assertEquals(i + 3, policy.getMinLowercase());
            assertEquals(i + 4, policy.getMinSymbols());
            assertEquals(i + 5, policy.getMinUppercase());
            assertEquals("!@#", policy.getSpecialSymbols());
        }

        assertEquals(policiesStr.toString(),
                     PasswdPolicy.hdrPoliciesToString(policies));
    }

    /** Test an invalid header policy */
    public void testHdrOneInvalid()
    {
        doTestBadHdrPolicy("01",
                           "Policy 0 too short for name length: 2");
        doTestBadHdrPolicy("01abPolicy",
                           "Policy 0 too short for name: 171");
        doTestBadHdrPolicy("0107Policy1",
                           "Policy 0 too short for flags: 4");
        doTestBadHdrPolicy("0107Policy1f",
                           "Policy 0 too short for flags: 4");
        doTestBadHdrPolicy("0107Policy1fe0",
                           "Policy 0 too short for flags: 4");
        doTestBadHdrPolicy("0107Policy1fe00",
                           "Policy 0 too short for password length: 3");
        doTestBadHdrPolicy("0107Policy1fe00a",
                           "Policy 0 too short for password length: 3");
        doTestBadHdrPolicy("0107Policy1fe00ab",
                           "Policy 0 too short for password length: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc1",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc11",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111a",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aa",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa0",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa00",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000f",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000ff",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff",
                           "Policy 0 too short for special symbols length: 2");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff0",
                           "Policy 0 too short for special symbols length: 2");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fffab",
                           "Policy 0 too short for special symbols: 171");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff03",
                           "Policy 0 too short for special symbols: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff03!",
                           "Policy 0 too short for special symbols: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff03!@",
                           "Policy 0 too short for special symbols: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff03!@#0",
                           "Policies field does not end at the last policy");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03!@#",
                           "Policies field does not end at the last policy");
    }

    /** Test multiple invalid header policies */
    public void testHdrMultiInvalid()
    {
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#",
                           "Policy 1 too short for name length: 2");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#abPolicy",
                           "Policy 1 too short for name: 171");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1",
                           "Policy 1 too short for flags: 4");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1f",
                           "Policy 1 too short for flags: 4");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe0",
                           "Policy 1 too short for flags: 4");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00",
                           "Policy 1 too short for password length: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00a",
                           "Policy 1 too short for password length: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00ab",
                           "Policy 1 too short for password length: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc",
                           "Policy 1 too short for min digit chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc1",
                           "Policy 1 too short for min digit chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc11",
                           "Policy 1 too short for min digit chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111",
                           "Policy 1 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111a",
                           "Policy 1 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aa",
                           "Policy 1 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa",
                           "Policy 1 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa0",
                           "Policy 1 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa00",
                           "Policy 1 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000",
                           "Policy 1 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000f",
                           "Policy 1 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000ff",
                           "Policy 1 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff",
                           "Policy 1 too short for special symbols length: 2");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff0",
                           "Policy 1 too short for special symbols length: 2");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fffab",
                           "Policy 1 too short for special symbols: 171");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03",
                           "Policy 1 too short for special symbols: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03!",
                           "Policy 1 too short for special symbols: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03!@",
                           "Policy 1 too short for special symbols: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03!@#0",
                           "Policies field does not end at the last policy");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000fff03!@#",
                           "Policies field does not end at the last policy");
    }

    /** Test a record without a policy */
    public void testRecNone()
    {
        PasswdPolicy policy = PasswdPolicy.parseRecordPolicy(null, null, null);
        assertNull(policy);

        PasswdPolicy.RecordPolicyStrs strs =
            PasswdPolicy.recordPolicyToString(null);
        assertNull(strs);
    }

    /** Test a record with a policy name */
    public void testRecPolicyName()
    {
        doTestRecordPolicyName("policy1", null, null);
        doTestRecordPolicyName("policy2", "foo", null);
        doTestRecordPolicyName("policy3", null, "bar");
        doTestRecordPolicyName("policy4", "foo", "bar");
    }

    /** Test a record with its own policy */
    public void testRecPolicy()
    {
        // easy to read
        doTestRecordPolicy(null, "b40000a001001001001", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS |
                           PasswdPolicy.FLAG_USE_EASY_VISION,
                           10, 1, 1, 1, 1);

        // hex only
        doTestRecordPolicy(null, "0800014001001001001", null,
                           PasswdPolicy.FLAG_USE_HEX_DIGITS,
                           20, 1, 1, 1, 1);

        // policy 1
        doTestRecordPolicy(null, "f00000f004002005003", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS,
                           15, 2, 3, 4, 5);

        // pronounce
        doTestRecordPolicy(null, "d200008001001001001", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_SYMBOLS |
                           PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE,
                           8, 1, 1, 1, 1);

        // special chars
        doTestRecordPolicy(null, "f00000d003001004002", "!@#$%^&*()",
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS,
                           13, 1, 2, 3, 4);
    }

    /** Test an invalid record policy */
    public void testRecPolicyInvalid()
    {
        doTestBadRecPolicy("",
                           "Policy 0 too short for flags: 4");
        doTestBadRecPolicy("f",
                           "Policy 0 too short for flags: 4");
        doTestBadRecPolicy("fe0",
                           "Policy 0 too short for flags: 4");
        doTestBadRecPolicy("fe00",
                           "Policy 0 too short for password length: 3");
        doTestBadRecPolicy("fe00a",
                           "Policy 0 too short for password length: 3");
        doTestBadRecPolicy("fe00ab",
                           "Policy 0 too short for password length: 3");
        doTestBadRecPolicy("fe00abc",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadRecPolicy("fe00abc1",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadRecPolicy("fe00abc11",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadRecPolicy("fe00abc111",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadRecPolicy("fe00abc111a",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadRecPolicy("fe00abc111aa",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadRecPolicy("fe00abc111aaa",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadRecPolicy("fe00abc111aaa0",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadRecPolicy("fe00abc111aaa00",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadRecPolicy("fe00abc111aaa000",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadRecPolicy("fe00abc111aaa000f",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadRecPolicy("fe00abc111aaa000ff",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadRecPolicy("fe00abc111aaa000fff0",
                           "Password policy too long: fe00abc111aaa000fff0");
    }

    /** Check a bad header policy */
    private static void doTestBadHdrPolicy(String policyStr, String exMsg)
    {
        try {
            PasswdPolicy.parseHdrPolicies(policyStr);
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof IllegalArgumentException);
            assertEquals(exMsg, t.getMessage());
        }
    }

    /** Check a bad record policy */
    private static void doTestBadRecPolicy(String policyStr, String exMsg)
    {
        try {
            PasswdPolicy.parseRecordPolicy(null, policyStr, null);
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof IllegalArgumentException);
            assertEquals(exMsg, t.getMessage());
        }
    }

    /** Check a record with a named password policy */
    private static void doTestRecordPolicyName(String policyName,
                                               String policyStr,
                                               String ownSymbols)
    {
        PasswdPolicy policy = PasswdPolicy.parseRecordPolicy(policyName,
                                                             policyStr,
                                                             ownSymbols);
        assertNotNull(policy);
        assertEquals(policyName, policy.getName());
        assertEquals(PasswdPolicy.Location.RECORD_NAME, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS, policy.getFlags());
        assertEquals(12, policy.getLength());
        assertEquals(1, policy.getMinDigits());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(1, policy.getMinSymbols());
        assertEquals(1, policy.getMinUppercase());
        assertNull(policy.getSpecialSymbols());

        PasswdPolicy.RecordPolicyStrs strs =
            PasswdPolicy.recordPolicyToString(policy);
        assertNotNull(strs);
        assertEquals(policyName, strs.itsPolicyName);
        assertEquals(null, strs.itsPolicyStr);
        assertEquals(null, strs.itsOwnSymbols);
    }

    /** Check a record with its own password policy */
    private static void doTestRecordPolicy(String policyName, String policyStr,
                                           String ownSymbols,
                                           int flags, int length,
                                           int minLower, int minUpper,
                                           int minDigits, int minSymbols)
    {
        PasswdPolicy policy = PasswdPolicy.parseRecordPolicy(policyName,
                                                             policyStr,
                                                             ownSymbols);
        assertNotNull(policy);
        assertNull(policy.getName());
        assertEquals(PasswdPolicy.Location.RECORD, policy.getLocation());
        assertEquals(flags, policy.getFlags());
        assertEquals(length, policy.getLength());
        assertEquals(minDigits, policy.getMinDigits());
        assertEquals(minLower, policy.getMinLowercase());
        assertEquals(minSymbols, policy.getMinSymbols());
        assertEquals(minUpper, policy.getMinUppercase());
        assertEquals(ownSymbols, policy.getSpecialSymbols());

        PasswdPolicy.RecordPolicyStrs strs =
            PasswdPolicy.recordPolicyToString(policy);
        assertNotNull(strs);
        assertEquals(policyName, strs.itsPolicyName);
        assertEquals(policyStr, strs.itsPolicyStr);
        assertEquals(ownSymbols, strs.itsOwnSymbols);
    }
}
