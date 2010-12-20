package com.jefftharris.passwdsafe;

public enum FontSizePref
{
    NORMAL ("Normal"),
    SMALL ("Small");

    private String itsDisplayName;

    private FontSizePref(String displayName)
    {
        itsDisplayName = displayName;
    }

    public final String getDisplayName()
    {
        return itsDisplayName;
    }
}