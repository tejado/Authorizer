/*
 * Copyright (©) 2009-2010 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

public class PwsPasswdField extends AbstractPwsPasswdField
{
    private static final String ENCODING = "ISO-8859-1";

    /**
     * Constructor from value
     */
    public PwsPasswdField(int type, String value, PwsFile file)
    {
        super(type, value, file, ENCODING);
    }

    /**
     * Constructor from type
     */
    public PwsPasswdField(PwsFieldType type)
    {
        super(type, ENCODING);
    }
}
