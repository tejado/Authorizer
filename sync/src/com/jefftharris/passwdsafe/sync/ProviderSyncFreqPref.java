/*
 * Copyright (©) 2013 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync;

/**
 *  Preference enum for a provider's sync frequency
 */
public enum ProviderSyncFreqPref
{
    // Values in display order
    FREQ_NONE   (0,             0),
    FREQ_15_MIN (15 * 60,       1),
    FREQ_30_MIN (30 * 60,       2),
    FREQ_1_HOUR (1 * 60 * 60,   3),
    FREQ_1_DAY  (24 * 60 * 60,  4);

    private final int itsFreq;
    private final int itsDisplayIdx;

    public static final ProviderSyncFreqPref DEFAULT = FREQ_15_MIN;

    /** Constructor */
    private ProviderSyncFreqPref(int freq, int displayIdx)
    {
        itsFreq = freq;
        itsDisplayIdx = displayIdx;
    }

    /** Get the frequency in seconds */
    public final int getFreq()
    {
        return itsFreq;
    }

    /** Get the display index */
    public final int getDisplayIdx()
    {
        return itsDisplayIdx;
    }

    /** Get the enum value from a sync frequency */
    public static ProviderSyncFreqPref freqValueOf(int freq)
    {
        for (ProviderSyncFreqPref pref: ProviderSyncFreqPref.values()) {
            if (pref.itsFreq == freq) {
                return pref;
            }
        }
        return DEFAULT;
    }

    /** Get the enum value from the display index */
    public static ProviderSyncFreqPref displayValueOf(int idx)
    {
        for (ProviderSyncFreqPref pref: ProviderSyncFreqPref.values()) {
            if (pref.itsDisplayIdx == idx) {
                return pref;
            }
        }
        return DEFAULT;
    }
}
