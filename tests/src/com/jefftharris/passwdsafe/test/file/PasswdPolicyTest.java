/**
 *
 */
package com.jefftharris.passwdsafe.test.file;

import java.util.List;

import com.jefftharris.passwdsafe.file.PasswdPolicy;

import android.test.AndroidTestCase;

/**
 * @author jharris
 *
 */
public class PasswdPolicyTest extends AndroidTestCase
{
    public PasswdPolicyTest()
    {
        super();
    }

    public void testHdrEmpty()
    {
        /*
        List<PasswdPolicy> policies;
        policies = PasswdPolicy.parseHdrPolicies(null);
        assertNull(policies);
        policies = PasswdPolicy.parseHdrPolicies("");
        assertNull(policies);
        */
    }

    public void testHdrZero()
    {
        /*
        try {
            PasswdPolicy.parseHdrPolicies("0");
            fail();
        } catch (Throwable t) {
            assertTrue(t instanceof IllegalArgumentException);
            assertEquals("Field length (1) too short: 2", t.getMessage());
        }
        */
    }
}
