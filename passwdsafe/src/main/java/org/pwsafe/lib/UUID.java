/*
 * $Id: UUID.java 376 2009-04-21 23:30:19Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib;

import android.support.annotation.NonNull;

import java.io.Serializable;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;


/**
 * A naive implementation of a UUID class.
 *
 * @author Kevin Preece
 */
@SuppressWarnings("ALL")
public class UUID implements Comparable<Object>, Serializable
{
	/**
     *
     */
    private static final long serialVersionUID = 1L;
    private final byte []		TheUUID	= new byte[ 16 ];

	/**
	 * Construct the object, generating a new UUID.
	 */
	public UUID()
	{
		long		time;
		int			time_hi;
		int			time_lo;

		time	  = new GregorianCalendar( 1582, 9, 15 ).getTimeInMillis();	// 15-Oct-1582
		time	  = Calendar.getInstance(TimeZone.getTimeZone("UCT")).getTimeInMillis() - time;
		time	<<= 19;
		time	 &= 0x0fffffffffffffffL;
		time	 |= 0x1000000000000000L;
		time_hi	  = (int)(time >>> 32);
		time_lo	  = (int) time;

		Util.putIntToByteArray( TheUUID, time_lo, 0 );
		Util.putIntToByteArray( TheUUID, time_hi, 4 );

		TheUUID[0]	= Util.newRand();
		TheUUID[1]	= Util.newRand();
		TheUUID[2]	= (byte)(Util.newRand() & 0x07);
		TheUUID[8]	= (byte)(Util.newRand() & 0x3f);
		TheUUID[9]	= Util.newRand();
		TheUUID[10]	= Util.newRand();
		TheUUID[11]	= Util.newRand();
		TheUUID[12]	= Util.newRand();
		TheUUID[13]	= Util.newRand();
		TheUUID[14]	= Util.newRand();
		TheUUID[15]	= Util.newRand();
	}

	/**
	 * Construct the UUID from the 16 byte array <code>uuid</code>.
	 *
	 * @param uuid the 16 bytes to use as the UUID.
	 */
	public UUID( byte [] uuid )
	{
		if ( uuid.length != TheUUID.length )
		{
			throw new IllegalArgumentException();
		}
		System.arraycopy( uuid, 0, TheUUID, 0, TheUUID.length );
	}

	/**
	 * Compares this <code>UUID</code> to another returning <code>true</code> if
	 * they're equal or <code>false</code> otherwise.
	 *
	 * @param ob the other <code>UUID</code> to compare to.
	 *
	 * @return <code>true</code> if the <code>UUID</code>s are equal or <code>false</code> otherwise.
	 */
	@Override
	public boolean equals( Object ob )
	{
	    return ob instanceof UUID && equals((UUID) ob);
	}

	/**
	 * Compares this <code>UUID</code> to another returning <code>true</code> if
	 * they're equal or <code>false</code> otherwise.
	 *
	 * @param that the other <code>UUID</code> to compare to.
	 *
	 * @return <code>true</code> if the <code>UUID</code>s are equal or <code>false</code> otherwise.
	 */
	public boolean equals( UUID that ) {
		if (that == this)
			return true;
		if (that == null)
			return false;

		byte	b1[];
		byte	b2[];

		b1	= this.getBytes();
		b2	= that.getBytes();

		for ( int ii = 0; ii < b1.length; ++ii )
		{
			if ( b1[ii] != b2[ii] )
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * Compares this <code>UUID</code> to another returning a value less than zero if
	 * <code>this</code> is "less than" <code>other</code>, zero if they're equal and greater
	 * than zero if <code>this</code> is "greater than" <code>other</code>.
	 *
	 * @param other the other field to compare to.
	 *
	 * @return A value less than zero if <code>this</code> is "less than" <code>other</code>,
	 *         zero if they're equal and greater than zero if <code>this</code> is "greater
	 *         than" <code>other</code>.
	 */
	public int compareTo( @NonNull Object other )
	{
		return compareTo( (UUID) other );
	}

	/**
	 * Compares this <code>UUID</code> to another returning a value less than zero if
	 * <code>this</code> is "less than" <code>other</code>, zero if they're equal and greater
	 * than zero if <code>this</code> is "greater than" <code>other</code>.
	 *
	 * @param other the other field to compare to.
	 *
	 * @return A value less than zero if <code>this</code> is "less than" <code>other</code>,
	 *         zero if they're equal and greater than zero if <code>this</code> is "greater
	 *         than" <code>other</code>.
	 */
	public int compareTo( UUID other )
	{
		for ( int ii = 0; ii < TheUUID.length; ++ii )
		{
			int	b1 = this.TheUUID[ii] & 0x0ff;
			int	b2 = other.TheUUID[ii] & 0x0ff;

			if ( b1 < b2 )
			{
				return -1;
			}
			else if ( b1 > b2 )
			{
				return 1;
			}
		}
		return 0;
	}

	/**
	 * Returns a byte array containing a copy of the 16 byte UUID.
	 *
	 * @return A byte array containing a copy of the 16 byte UUID.
	 */
	public byte [] getBytes()
	{
		return Util.cloneByteArray( TheUUID );
	}

	/**
	 * Converts this UUID into human-readable form.  The string has the format:
	 * {01234567-89ab-cdef-0123-456789abcdef}.
	 *
	 * @return A <code>String</code> representation of this <code>UUID</code>.
	 */
	@Override
	public String toString()
	{
		return toString( TheUUID );
	}

	/**
	 * Converts <code>uuid</code> into human-readable form.  The string has the format:
	 * {01234567-89ab-cdef-0123-456789abcdef}.
	 *
	 * @param uuid the 16 byte array to convert.
	 *
	 * @return A <code>String</code> representation of this <code>UUID</code>.
	 */
	public static String toString( byte [] uuid )
	{
		if ( uuid.length != 16 )
		{
			throw new IllegalArgumentException();
		}

	    return "{" + Util.bytesToHex(uuid, 0, 4) + '-' +
		   Util.bytesToHex(uuid, 4, 2) + '-' +
		   Util.bytesToHex(uuid, 6, 2) + '-' +
		   Util.bytesToHex(uuid, 8, 2) + '-' +
		   Util.bytesToHex(uuid, 10, 6) + '}';
	}
}
