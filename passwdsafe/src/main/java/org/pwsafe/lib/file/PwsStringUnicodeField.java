/*
 * $Id: PwsStringField.java 294 2004-02-22 15:29:03Z preecek $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.file;

import android.support.annotation.NonNull;

import java.io.UnsupportedEncodingException;

/**
 * @author Kevin Preece
 */
@SuppressWarnings("SameParameterValue")
public class PwsStringUnicodeField extends PwsField
{
	private static final long serialVersionUID = -4530429748953931053L;

	/**
	 * Constructor
	 *
	 * @param type  the field's type.
	 * @param value the field's value.
	 * @throws UnsupportedEncodingException
	 */
	public PwsStringUnicodeField( int type, byte[] value ) throws UnsupportedEncodingException
	{

		super( type, new String(value, "UTF-8" ));
	}

	/**
	 * Constructor
	 *
	 * @param type  the field's type.
	 * @param value the field's value.
	 */
	public PwsStringUnicodeField( int type, String value ) {
		super( type, value);
	}

	/**
	 * Constructor
	 *
	 * @param type  the field's type.
	 * @param value the field's value.
	 */
	public PwsStringUnicodeField( int type, StringBuilder value ) {
		// TODOlib: allow StringBuilder or CharSequence as value further up.
		super( type, value != null ? value.toString() : null);
	}

	/**
	 * Constructor
	 *
	 * @param type  the field's type.
	 * @param value the field's value.
	 */
	public PwsStringUnicodeField( PwsFieldType type, String value ) {
		super( type, value);
	}

	/**
	 * Returns the field's value as a byte array.
	 *
	 * @return A byte array containing the field's data.
	 *
	 * @see org.pwsafe.lib.file.PwsField#getBytes()
	 */
	@Override
	public byte[] getBytes()
	{
		try {
			return ((String) super.getValue()).getBytes("UTF-8");
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Compares this <code>PwsStringField</code> to another returning a value less than zero if
	 * <code>this</code> is "less than" <code>that</code>, zero if they're equal and greater
	 * than zero if <code>this</code> is "greater than" <code>that</code>.
	 *
	 * @param that the other field to compare to.
	 *
	 * @return A value less than zero if <code>this</code> is "less than" <code>that</code>,
	 *         zero if they're equal and greater than zero if <code>this</code> is "greater
	 *         than" <code>that</code>.
	 */
	public int compareTo( @NonNull Object that )
	{
		return ((String) this.getValue()).compareTo((String) ((PwsStringUnicodeField) that).getValue());
	}

	/**
	 * Compares this object to another <code>PwsStringField</code> or <code>String</code> returning
	 * <code>true</code> if they're equal or <code>false</code> otherwise.
	 *
	 * @param arg0 the other object to compare to.
	 *
	 * @return <code>true</code> if they're equal or <code>false</code> otherwise.
	 */
	@Override
	public boolean equals( Object arg0 )
	{
		if ( arg0 instanceof PwsStringUnicodeField )
		{
			return equals( (PwsStringUnicodeField) arg0 );
		}
		else if ( arg0 instanceof String )
		{
			return equals( (String) arg0 );
		}
		throw new ClassCastException();
	}

	/**
	 * Compares this object to another <code>PwsStringField</code> returning
	 * <code>true</code> if they're equal or <code>false</code> otherwise.
	 *
	 * @param arg0 the other object to compare to.
	 *
	 * @return <code>true</code> if they're equal or <code>false</code> otherwise.
	 */
	public boolean equals( PwsStringUnicodeField arg0 )
	{
		return super.getValue().equals(arg0.getValue());
	}

	/**
	 * Compares this object to a <code>String</code> returning <code>true</code> if they're equal
	 * or <code>false</code> otherwise.
	 *
	 * @param arg0 the other object to compare to.
	 *
	 * @return <code>true</code> if they're equal or <code>false</code> otherwise.
	 */
	public boolean equals( String arg0 )
	{
		return super.getValue().equals(arg0);
	}
}
