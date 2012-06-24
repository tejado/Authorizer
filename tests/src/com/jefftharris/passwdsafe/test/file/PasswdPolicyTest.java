/*
 * Copyright (©) 2012 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.test.file;

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
        assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                     PasswdPolicy.FLAG_USE_UPPERCASE |
                     PasswdPolicy.FLAG_USE_DIGITS |
                     PasswdPolicy.FLAG_USE_SYMBOLS |
                     PasswdPolicy.FLAG_USE_HEX_DIGITS |
                     PasswdPolicy.FLAG_USE_EASY_VISION |
                     PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE, policy.getFlags());
        assertEquals(0xabc, policy.getLength());
        assertEquals(0x111, policy.getMinLowercase());
        assertEquals(0xaaa, policy.getMinUppercase());
        assertEquals(0x000, policy.getMinDigits());
        assertEquals(0xfff, policy.getMinSymbols());
        assertEquals("!@#", policy.getSpecialSymbols());

        assertEquals(policiesStr, PasswdPolicy.hdrPoliciesToString(policies));
    }

    /** Test multiple valid header policies */
    public void testHdrMultiValid()
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
            assertEquals(PasswdPolicy.FLAG_USE_LOWERCASE |
                         PasswdPolicy.FLAG_USE_UPPERCASE |
                         PasswdPolicy.FLAG_USE_DIGITS |
                         PasswdPolicy.FLAG_USE_SYMBOLS |
                         PasswdPolicy.FLAG_USE_HEX_DIGITS |
                         PasswdPolicy.FLAG_USE_EASY_VISION |
                         PasswdPolicy.FLAG_MAKE_PRONOUNCEABLE, policy.getFlags());
            assertEquals(i + 1, policy.getLength());
            assertEquals(i + 2, policy.getMinLowercase());
            assertEquals(i + 3, policy.getMinUppercase());
            assertEquals(i + 4, policy.getMinDigits());
            assertEquals(i + 5, policy.getMinSymbols());
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
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc1",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc11",
                           "Policy 0 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111a",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aa",
                           "Policy 0 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa0",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa00",
                           "Policy 0 too short for min digit chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000f",
                           "Policy 0 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0107Policy1fe00abc111aaa000ff",
                           "Policy 0 too short for min symbol chars: 3");
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
                           "Policy 1 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc1",
                           "Policy 1 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc11",
                           "Policy 1 too short for min lowercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111",
                           "Policy 1 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111a",
                           "Policy 1 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aa",
                           "Policy 1 too short for min uppercase chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa",
                           "Policy 1 too short for min digit chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa0",
                           "Policy 1 too short for min digit chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa00",
                           "Policy 1 too short for min digit chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000",
                           "Policy 1 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000f",
                           "Policy 1 too short for min symbol chars: 3");
        doTestBadHdrPolicy("0207Policy1fe00abc111aaa000fff03!@#07Policy1fe00abc111aaa000ff",
                           "Policy 1 too short for min symbol chars: 3");
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
}
