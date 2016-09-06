/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;


public class PwsPasswdUnicodeField extends AbstractPwsPasswdField
{
    private static final String ENCODING = "UTF-8";

    /**
     * Constructor from binary value
     */
    public PwsPasswdUnicodeField(int type, byte[] value, PwsFile file)
    {
        super(type, value, file, ENCODING);
    }

    /**
     * Constructor from string
     */
    public PwsPasswdUnicodeField(int type, String value, PwsFile file)
    {
        super(type, value, file, ENCODING);
    }

    /**
     * Constructor from type
     */
    @SuppressWarnings("SameParameterValue")
    public PwsPasswdUnicodeField(PwsFieldType type)
    {
        super(type, ENCODING);
    }
}
