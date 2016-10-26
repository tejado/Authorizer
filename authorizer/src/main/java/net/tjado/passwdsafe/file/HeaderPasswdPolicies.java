/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

import java.util.Collection;
import java.util.List;
import java.util.TreeMap;

/**
 * The HeaderPasswdPolicies class encapsulates the policies stored in the header
 * of a password file
 */
public class HeaderPasswdPolicies
{
    /** The HdrPolicy class encapsulates a policy in the file header */
    public static class HdrPolicy
    {
        private final PasswdPolicy itsPolicy;
        private int itsUseCount;

        /** Constructor */
        public HdrPolicy(PasswdPolicy policy)
        {
            itsPolicy = policy;
            itsUseCount = 0;
        }

        /** Get the policy */
        public PasswdPolicy getPolicy()
        {
            return itsPolicy;
        }

        /** Get the number of records referencing this policy */
        public int getUseCount()
        {
            return itsUseCount;
        }

        /** Increment the number of records referencing this policy */
        public void incUseCount()
        {
            ++itsUseCount;
        }
    }

    private final TreeMap<String, HdrPolicy> itsPolicies = new TreeMap<>();

    /** Default constructor */
    public HeaderPasswdPolicies()
    {
    }

    /** Constructor from the contents of a file */
    public HeaderPasswdPolicies(Collection<PasswdRecord> recs,
                                List<PasswdPolicy> policies)
    {
        if (policies != null) {
            for (PasswdPolicy policy: policies) {
                itsPolicies.put(policy.getName(), new HdrPolicy(policy));
            }
        }
        for (PasswdRecord rec: recs) {
            PasswdPolicy recPolicy = rec.getPasswdPolicy();
            if ((recPolicy != null) &&
                (recPolicy.getLocation() ==
                 PasswdPolicy.Location.RECORD_NAME)) {
                HeaderPasswdPolicies.HdrPolicy hdrPolicy =
                    itsPolicies.get(recPolicy.getName());
                if (hdrPolicy != null) {
                    hdrPolicy.incUseCount();
                }
            }
        }
    }

    /** Get the named password policy */
    public PasswdPolicy getPasswdPolicy(String name)
    {
        HeaderPasswdPolicies.HdrPolicy hdrPolicy = itsPolicies.get(name);
        if (hdrPolicy != null) {
            return hdrPolicy.getPolicy();
        }
        return null;
    }

    /** Get the named password policy's use count (-1 if not found) */
    public int getPolicyUseCount(String name)
    {
        HeaderPasswdPolicies.HdrPolicy hdrPolicy = itsPolicies.get(name);
        if (hdrPolicy != null) {
            return hdrPolicy.getUseCount();
        }
        return -1;

    }

    /** Does the header contain a policy with the given name */
    public boolean containsPolicy(String name)
    {
        return itsPolicies.containsKey(name);
    }

    /** Get the collection of header policies */
    public Collection<HeaderPasswdPolicies.HdrPolicy> getPolicies()
    {
        return itsPolicies.values();
    }
}
