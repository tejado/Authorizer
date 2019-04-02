/*
 * Copyright (©) 2019 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.test.file;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import android.annotation.SuppressLint;

import net.tjado.passwdsafe.file.PasswdPolicy;

import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static junit.framework.Assert.assertTrue;
import static junit.framework.TestCase.assertNotNull;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.empty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;

/**
 * Tests for the PasswdPolicy class
 */
@SuppressWarnings("unused")
public class PasswdPolicyTest
{
    //private static final String TAG = "PasswdPolicyTest";

    /** Test an empty header policy */
    @Test
    public void testHdrEmpty()
    {
        List<PasswdPolicy> policies;
        policies = PasswdPolicy.parseHdrPolicies(null);
        assertNull(policies);
        policies = PasswdPolicy.parseHdrPolicies("");
        assertNull(policies);

        String str = PasswdPolicy.hdrPoliciesToString(null);
        //noinspection ConstantConditions
        assertNull(str);
    }

    /** Test zero header policies */
    @Test
    public void testHdrZero()
    {
        doTestBadHdrPolicy("0", "Policies length (1) too short: 2");

        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies("00");
        assertThat(policies, is(empty()));
        String str = PasswdPolicy.hdrPoliciesToString(policies);
        assertEquals("00", str);

        doTestBadHdrPolicy("000",
                           "Policies field does not end at the last policy");
    }

    /** Test one valid header policy */
    @Test
    public void testHdrOneValid()
    {
        String policiesStr = "0107Policy1fe00abc111aaa000fff03!@#";
        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies(policiesStr);
        assertNotNull(policies);
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

    /** Test one valid header policy with zero min lengths */
    @Test
    public void testHdrOneValidZeros()
    {
        String policiesStr = "0107Policy1fe00abc00000000000003!@#";
        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies(policiesStr);
        assertNotNull(policies);
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
        assertEquals(0x000, policy.getMinDigits());
        assertEquals(0x000, policy.getMinLowercase());
        assertEquals(0x000, policy.getMinSymbols());
        assertEquals(0x000, policy.getMinUppercase());
        assertEquals("!@#", policy.getSpecialSymbols());

        assertEquals(policiesStr, PasswdPolicy.hdrPoliciesToString(policies));
    }

    /** Test a default header policy */
    @Test
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
        assertEquals(PasswdPolicy.Type.NORMAL, policy.getType());
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
    @Test
    public void testHdrMultiValid()
    {
        String policiesStr = "060ceasy to readb40000a0010010010010008hex only08000140010010010010008policy 1f00000f0040020050030009pronounced200008001001001001000dspecial charsf00000d0030010040020a!@#$%^&*()05zerosf00000e00000000000000";
        List<PasswdPolicy> policies = PasswdPolicy.parseHdrPolicies(policiesStr);
        assertNotNull(policies);
        assertEquals(6, policies.size());

        PasswdPolicy policy;
        policy = policies.get(0);
        assertEquals("easy to read", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS |
                     PasswdPolicy.FLAG_USE_EASY_VISION, policy.getFlags());
        assertEquals(PasswdPolicy.Type.EASY_TO_READ, policy.getType());
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
        assertEquals(PasswdPolicy.Type.HEXADECIMAL, policy.getType());
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
        assertEquals(PasswdPolicy.Type.NORMAL, policy.getType());
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
        assertEquals(PasswdPolicy.Type.PRONOUNCEABLE, policy.getType());
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
        assertEquals(PasswdPolicy.Type.NORMAL, policy.getType());
        assertEquals(13, policy.getLength());
        assertEquals(1, policy.getMinLowercase());
        assertEquals(2, policy.getMinUppercase());
        assertEquals(3, policy.getMinDigits());
        assertEquals(4, policy.getMinSymbols());
        assertEquals("!@#$%^&*()", policy.getSpecialSymbols());

        policy = policies.get(5);
        assertEquals("zeros", policy.getName());
        assertEquals(PasswdPolicy.Location.HEADER, policy.getLocation());
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS, policy.getFlags());
        assertEquals(PasswdPolicy.Type.NORMAL, policy.getType());
        assertEquals(14, policy.getLength());
        assertEquals(0, policy.getMinLowercase());
        assertEquals(0, policy.getMinUppercase());
        assertEquals(0, policy.getMinDigits());
        assertEquals(0, policy.getMinSymbols());
        assertNull(policy.getSpecialSymbols());
    }

    /** Test max valid header policies */
    @Test
    @SuppressLint("DefaultLocale")
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
        assertNotNull(policies);
        assertEquals(255, policies.size());
        for (int i = 0; i < 255; ++i) {
            PasswdPolicy policy = policies.get(i);
            assertEquals(String.format(Locale.US, "Policy%03d", i),
                         policy.getName());
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
    @Test
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
    @Test
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
    @Test
    public void testRecNone()
    {
        PasswdPolicy policy = PasswdPolicy.parseRecordPolicy(null, null, null);
        assertNull(policy);

        PasswdPolicy.RecordPolicyStrs strs =
                PasswdPolicy.recordPolicyToString(null);
        //noinspection ConstantConditions
        assertNull(strs);
    }

    /** Test a record with a policy name */
    @Test
    public void testRecPolicyName()
    {
        doTestRecordPolicyName("policy1", null, null);
        doTestRecordPolicyName("policy2", "foo", null);
        doTestRecordPolicyName("policy3", null, "bar");
        doTestRecordPolicyName("policy4", "foo", "bar");
    }

    /** Test a record with its own policy */
    @Test
    public void testRecPolicy()
    {
        // easy to read
        doTestRecordPolicy(null, "b40000a001001001001", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS |
                           PasswdPolicy.FLAG_USE_EASY_VISION,
                           10, PasswdPolicy.Type.EASY_TO_READ, 1, 1, 1, 1);

        // hex only
        doTestRecordPolicy(null, "0800014001001001001", null,
                           PasswdPolicy.FLAG_USE_HEX_DIGITS,
                           20, PasswdPolicy.Type.HEXADECIMAL, 1, 1, 1, 1);

        // policy 1
        doTestRecordPolicy(null, "f00000f004002005003", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS,
                           15, PasswdPolicy.Type.NORMAL, 2, 3, 4, 5);

        // pronounce
        doTestRecordPolicy(null, "d200008001001001001", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_SYMBOLS |
                           PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE,
                           8, PasswdPolicy.Type.PRONOUNCEABLE, 1, 1, 1, 1);

        // special chars
        doTestRecordPolicy(null, "f00000d003001004002", "!@#$%^&*()",
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS,
                           13, PasswdPolicy.Type.NORMAL, 1, 2, 3, 4);

        // zero lengths
        doTestRecordPolicy(null, "f00000e000000000000", null,
                           PasswdPolicy.FLAG_USE_LOWERCASE |
                           PasswdPolicy.FLAG_USE_UPPERCASE |
                           PasswdPolicy.FLAG_USE_DIGITS |
                           PasswdPolicy.FLAG_USE_SYMBOLS,
                           14, PasswdPolicy.Type.NORMAL, 0, 0, 0, 0);
    }

    /** Test an invalid record policy */
    @Test
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
    }

    /** Test default password generation */
    @Test
    public void testPasswdGenDefault()
    {
        PasswdPolicy policy =
                new PasswdPolicy("", PasswdPolicy.Location.DEFAULT);
        verifyGenPasswd(policy);
    }


    /** Test normal type password generation */
    @Test
    public void testPasswdGenNormal()
    {
        doTestPasswdGen(PasswdPolicy.Type.NORMAL);
    }

    /** Test easy-to-read type password generation */
    @Test
    public void testPasswdGenEasy()
    {
        doTestPasswdGen(PasswdPolicy.Type.EASY_TO_READ);
    }

    /** Test easy-to-read type password generation */
    @Test
    public void testPasswdGenHex()
    {
        for (int len = 0; len < 100; ++len) {
            PasswdPolicy policy =
                    new PasswdPolicy("", PasswdPolicy.Location.DEFAULT,
                                     PasswdPolicy.FLAG_USE_HEX_DIGITS,
                                     len, 0, 0, 0, 0, null);
            assertEquals(PasswdPolicy.Type.HEXADECIMAL, policy.getType());
            verifyGenPasswd(policy);
        }
    }

    /** Test pronounceable type password generation */
    @Test
    public void testPasswdGenPronounceable()
    {
        doTestPasswdGen(PasswdPolicy.Type.PRONOUNCEABLE);
    }

    /** Check a bad header policy */
    private static void doTestBadHdrPolicy(String policyStr, String exMsg)
    {
        try {
            PasswdPolicy.parseHdrPolicies(policyStr);
            assertTrue(false);
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
            assertTrue(false);
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
        assertEquals(PasswdPolicy.Type.NORMAL, policy.getType());
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
    private static void doTestRecordPolicy(
            @SuppressWarnings("SameParameterValue") String policyName,
            String policyStr, String ownSymbols, int flags, int length,
            PasswdPolicy.Type type, int minLower, int minUpper,
            int minDigits, int minSymbols)
    {
        doTestRecordPolicy(policyName, policyStr, ownSymbols, flags, length,
                           type, minLower, minUpper, minDigits, minSymbols, 0);
    }


    private static void doTestRecordPolicy(String policyName, String policyStr,
                                           String ownSymbols,
                                           int flags, int length,
                                           PasswdPolicy.Type type,
                                           int minLower, int minUpper,
                                           int minDigits, int minSymbols,
                                           int extras)
    {
        PasswdPolicy policy = PasswdPolicy.parseRecordPolicy(policyName,
                                                             policyStr,
                                                             ownSymbols);
        assertNotNull(policy);
        assertNull(policy.getName());
        assertEquals(PasswdPolicy.Location.RECORD, policy.getLocation());
        assertEquals(flags, policy.getFlags());
        assertEquals(type, policy.getType());
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
        assertTrue(policyStr.startsWith(strs.itsPolicyStr));
        assertEquals(ownSymbols, strs.itsOwnSymbols);

        if (extras < 5) {
            policyStr += '\0';
            doTestRecordPolicy(policyName, policyStr, ownSymbols, flags, length,
                               type, minLower, minUpper, minDigits, minSymbols,
                               extras + 1);
        }
    }

    /** Test normal, easy-to-read, or pronounceable type password generation */
    private static void doTestPasswdGen(PasswdPolicy.Type type)
    {
        PasswdPolicy policy;

        for (int i = 1; i < 16; ++i) {
            int flags = 0;
            switch (type) {
            case NORMAL: {
                break;
            }
            case EASY_TO_READ: {
                flags = PasswdPolicy.FLAG_USE_EASY_VISION;
                break;
            }
            case PRONOUNCEABLE: {
                flags = PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE;
                break;
            }
            case HEXADECIMAL: {
                assertTrue(false);
                break;
            }
            }
            int minLen = 0;
            if ((i & 0x01) != 0) {
                flags |= PasswdPolicy.FLAG_USE_LOWERCASE;
                ++minLen;
            }
            if ((i & 0x02) != 0) {
                flags |= PasswdPolicy.FLAG_USE_UPPERCASE;
                ++minLen;
            }
            if ((i & 0x04) != 0) {
                flags |= PasswdPolicy.FLAG_USE_DIGITS;
                ++minLen;
            }
            if ((i & 0x08) != 0) {
                flags |= PasswdPolicy.FLAG_USE_SYMBOLS;
                ++minLen;
            }
            minLen = 0;

            switch (type) {
            case NORMAL:
            case EASY_TO_READ: {
                final int MAX_LEN = 16;
                final int LEN_STEP = MAX_LEN / 4;
                for (int lowerIdx = 0; lowerIdx <= MAX_LEN;
                     lowerIdx += LEN_STEP) {
                    for (int upperIdx = 0;
                         upperIdx <= MAX_LEN - lowerIdx; upperIdx += LEN_STEP) {
                        for (int digitIdx = 0;
                             digitIdx <= MAX_LEN - lowerIdx - upperIdx;
                             digitIdx += LEN_STEP) {
                            for (int symbolIdx = 0;
                                 symbolIdx <= MAX_LEN - lowerIdx - upperIdx - digitIdx;
                                 symbolIdx += LEN_STEP) {
                                /*
                            PasswdSafeUtil.dbginfo(TAG, "Iter %x %d %d %d %d",
                                                  flags, lowerIdx, upperIdx,
                                                  digitIdx, symbolIdx);
                                 */
                                policy = new PasswdPolicy(
                                        "", PasswdPolicy.Location.DEFAULT,
                                        flags, MAX_LEN + minLen, lowerIdx,
                                        upperIdx, digitIdx, symbolIdx,
                                        null);
                                assertEquals(type, policy.getType());
                                verifyGenPasswd(policy);
                            }
                        }
                    }
                }
                break;
            }
            case PRONOUNCEABLE: {
                for (int len: new int[] {0, 1, 2, 3, 5, 10, 20}) {
                    //PasswdSafeApp.dbginfo("TAG", "Iter %x %d", flags, len);
                    policy = new PasswdPolicy(
                            "", PasswdPolicy.Location.DEFAULT,
                            flags, len, 1, 1, 1, 1, null);
                    assertEquals(type, policy.getType());
                    verifyGenPasswd(policy);
                }
                break;
            }
            case HEXADECIMAL: {
                assertTrue(false);
                break;
            }
            }
        }
    }

    /** Verify a generated password */
    @SuppressWarnings("ConstantConditions")
    private static void verifyGenPasswd(PasswdPolicy policy)
    {
        boolean useLower = policy.checkFlags(PasswdPolicy.FLAG_USE_LOWERCASE);
        boolean useUpper = policy.checkFlags(PasswdPolicy.FLAG_USE_UPPERCASE);
        boolean useDigits = policy.checkFlags(PasswdPolicy.FLAG_USE_DIGITS);
        boolean useSymbols = policy.checkFlags(PasswdPolicy.FLAG_USE_SYMBOLS);
        String symbols = policy.getSpecialSymbols();
        for (int testIdx = 0; testIdx < 20; ++testIdx) {
            String passwd = policy.generate();
            assertEquals(policy.getLength(), passwd.length());

            int numLower = 0;
            int numUpper = 0;
            int numDigits = 0;
            int numSymbols = 0;
            switch (policy.getType()) {
            case NORMAL: {
                if (symbols == null) {
                    symbols = PasswdPolicy.SYMBOLS_DEFAULT;
                }
                for (int idx = 0; idx < passwd.length(); ++idx) {
                    char c = passwd.charAt(idx);
                    if (PasswdPolicy.LOWER_CHARS.indexOf(c) >= 0) {
                        ++numLower;
                    } else if (PasswdPolicy.UPPER_CHARS.indexOf(c) >= 0) {
                        ++numUpper;
                    } else if (PasswdPolicy.DIGITS.indexOf(c) >= 0) {
                        ++numDigits;
                    } else if (symbols.indexOf(c) >= 0) {
                        ++numSymbols;
                    } else {
                        assertTrue(false);
                    }
                }
                verifyPolicyNumChars(useLower, numLower,
                                     policy.getMinLowercase());
                verifyPolicyNumChars(useUpper, numUpper,
                                     policy.getMinUppercase());
                verifyPolicyNumChars(useDigits, numDigits,
                                     policy.getMinDigits());
                verifyPolicyNumChars(useSymbols, numSymbols,
                                     policy.getMinSymbols());
                break;
            }
            case EASY_TO_READ: {
                if (symbols == null) {
                    symbols = PasswdPolicy.SYMBOLS_EASY;
                }
                for (int idx = 0; idx < passwd.length(); ++idx) {
                    char c = passwd.charAt(idx);
                    if (PasswdPolicy.EASY_LOWER_CHARS.indexOf(c) >= 0) {
                        ++numLower;
                    } else if (PasswdPolicy.EASY_UPPER_CHARS.indexOf(c) >= 0) {
                        ++numUpper;
                    } else if (PasswdPolicy.EASY_DIGITS.indexOf(c) >= 0) {
                        ++numDigits;
                    } else if (symbols.indexOf(c) >= 0) {
                        ++numSymbols;
                    } else {
                        assertTrue(false);
                    }
                }
                verifyPolicyNumChars(useLower, numLower,
                                     policy.getMinLowercase());
                verifyPolicyNumChars(useUpper, numUpper,
                                     policy.getMinUppercase());
                verifyPolicyNumChars(useDigits, numDigits,
                                     policy.getMinDigits());
                verifyPolicyNumChars(useSymbols, numSymbols,
                                     policy.getMinSymbols());
                break;
            }
            case PRONOUNCEABLE: {
                String pronDigits = "483610572";
                for (int idx = 0; idx < passwd.length(); ++idx) {
                    char c = passwd.charAt(idx);
                    if (PasswdPolicy.LOWER_CHARS.indexOf(c) >= 0) {
                        ++numLower;
                    } else if (PasswdPolicy.UPPER_CHARS.indexOf(c) >= 0) {
                        ++numUpper;
                    } else if (pronDigits.indexOf(c) >= 0) {
                        ++numDigits;
                    } else if (PasswdPolicy.SYMBOLS_PRONOUNCE.indexOf(c) >= 0) {
                        ++numSymbols;
                    } else {
                        assertTrue(false);
                    }
                }

                /*if (!useLower) {
                    assertEquals(0, numLower);
                }*/
                if (!useUpper) {
                    assertEquals(0, numUpper);
                }
                if (!useDigits) {
                    assertEquals(0, numDigits);
                }
                if (!useSymbols) {
                    assertEquals(0, numSymbols);
                }
                break;
            }
            case HEXADECIMAL: {
                for (int idx = 0; idx < passwd.length(); ++idx) {
                    char c = passwd.charAt(idx);
                    assertTrue(PasswdPolicy.HEX_DIGITS.indexOf(c) >= 0);
                }
                break;
            }
            }
        }
    }

    /** Verify the number of characters used for a policy */
    private static void verifyPolicyNumChars(boolean charsUsed,
                                             int numChars,
                                             int policyMin)
    {
        if (charsUsed) {
            assertTrue(numChars >= policyMin);
        } else {
            assertEquals(0, numChars);
        }
    }

}
