/*
 * $Id: Util.java 411 2009-09-25 18:19:34Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib;

import java.security.SecureRandom;
import java.util.Arrays;

import org.pwsafe.lib.crypto.SHA256Pws;

/**
 * This class exposes various utilty methods.
 *
 * @author Kevin Preece
 */
public final class Util
{
	private static final Log LOG = Log.getInstance(Util.class.getPackage().getName());

	private static final char	HEX_CHARS[] = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	private static final SecureRandom randGen = new SecureRandom();
	static {
		randGen.setSeed(System.currentTimeMillis());
	}

	/**
	 * Private to prevent instantiation.
	 */
	private Util()
	{
	}

	/**
	 * Adds more seed values to the internal {@link SecureRandom} generator.
	 * @param seeds
	 */
	public static void addSeedBytes (byte[] seeds) {
		randGen.setSeed(seeds);
	}

	/**
	 * Allocates a byte array with a length of <code>length</code> and fills it with
	 * random data.
	 *
	 * @param length the length of the array.
	 *
	 * @return A byte array initialised with random data.
	 */
	public static byte [] allocateByteArray( int length ) {
		byte	array [] = new byte [ length ];
		newRandBytes(array);
		return array;
	}

	/**
	 * Compares two byte arrays returning <code>true</code> if they're equal in length and
	 * content <code>false</code> if they're not.
	 *
	 * @param b1 the first byte array.
	 * @param b2 the second byte array.
	 *
	 * @return <code>true</code> if the arrays are equal, <code>false</code> if they are not.
	 */
	public static boolean bytesAreEqual( byte [] b1, byte [] b2 ) {
		return Arrays.equals(b1, b2);
	}

	/**
	 * Join two arrays to form a single array.
	 *
	 * @param a first array
	 * @param b second array
	 * @return first array appended with second array
	 */
	public static byte[] mergeBytes(byte[] a, byte[] b) {
	    final byte[] p = new byte[a.length + b.length];
		System.arraycopy(a, 0, p, 0, a.length);
		System.arraycopy(b, 0, p, a.length, b.length);
		return p;
	}

	/**
	 * Extracts a subset of a byte array as a new byte array.
	 *
	 * @param src the byte array to trim
	 * @param offset the offset to start at
	 * @param length the number of bytes to include
	 *
	 * @return a byte array containing the specified subset
	 */
	public static byte[] getBytes(byte[] src, int offset, int length) {

		final byte[] output = new byte[length];
		System.arraycopy(src, offset, output, 0, length);
		return output;
	}



	/**
	 * Copies the contents of src into target.
	 *
	 * @param src first array
	 * @param target second array
	 */
	public static void copyBytes(byte[] src, byte[] target) {
		System.arraycopy(src, 0, target, 0, src.length);
	}


	/**
	 * Converts a byte to its unsigned hexadecimal equivalent.  For example a value of -92 converts
	 * to "a4".
	 *
	 * @param b the byte to convert
	 *
	 * @return A <code>String</code> representation of the value in hexadecimal
	 */
	public static String byteToHex( byte b )
	{
	    return String.valueOf(new char[] {
	        HEX_CHARS[(b >>> 4) & 0x0f], HEX_CHARS[b & 0x0f]
	        });
	}

	/**
	 * Converts a byte array to a hexadecimal string.
	 *
	 * @param b the byte array to be converted to a hex string.
	 *
	 * @return The hexadecimal representation of the byte array contents.
	 */
	public static String bytesToHex( byte [] b )
	{
	    return bytesToHex( b, 0, b.length );
	}

	/**
	 * Converts a byte array to a hexadecimal string.  Conversion starts at byte <code>offset</code>
	 * and continues for <code>length</code> bytes.
	 *
	 * @param b      the array to be converted.
	 * @param offset the start offset within <code>b</code>.
	 * @param length the number of bytes to convert.
	 *
	 * @return A string representation of the byte array.
	 *
	 * @throws IllegalArgumentException if <code>length</code> is negative.
	 * @throws ArrayIndexOutOfBoundsException if <code>(offest + length) &gt; b.length</code>.
	 */
	public static String bytesToHex( byte [] b, int offset, int length )
	{
		if ( length < 0 )
		{
			throw new IllegalArgumentException( "Lengh must be not be negative." );
		}

		final StringBuilder sb = new StringBuilder( length << 1 );

		for ( int ii = offset; ii < (offset + length); ++ii )
		{
		    sb.append(HEX_CHARS[(b[ii] >>> 4) & 0x0f]);
		    sb.append(HEX_CHARS[b[ii] & 0x0f]);
		}
		return sb.toString();
	}

	/**
	 * Produces a string prepresentation of the byte array.
	 *
	 * @param b the array to be processed.
	 *
	 * @return A string representation of the byte array.
	 */
	public static String bytesToString( byte [] b )
	{
		final StringBuilder sb = new StringBuilder();

		sb.append( "{ " );
		for ( int ii = 0; ii < b.length; ++ii )
		{
			if ( ii > 0 )
			{
				sb.append( ", " );
			}
			sb.append( b[ii] );
		}
		sb.append( " }" );

		return sb.toString();
	}

	/**
	 * Converts an array from the native big-endian order to the little-endian order
	 * used by PasswordSafe.  The array is transformed in-place.
	 *
	 * @param src the array to be byte-swapped.
	 *
	 * @throws IllegalArgumentException if the array length is zero or not a multiple of four bytes.
	 */
	public static void bytesToLittleEndian( byte [] src )
	{
		byte	temp;

		if ( (src.length == 0) || ((src.length % 4) != 0) )
		{
			String	msg;

			msg = I18nHelper.getInstance().formatMessage( "E00009", new Object [] { new Integer(src.length) } );

			LOG.error( msg );
			LOG.leaveMethod( "Util.bytesToLittleEndian(byte[]) - by throwing IllegalArgumentException" );
			throw new IllegalArgumentException( msg );
		}

		if (LOG.isDebug2Enabled()) LOG.debug2( "Bytes to be swapped = " + bytesToString(src) );

		for ( int ii = 0; ii < src.length; ii += 4 )
		{
			temp		= src[ii];
			src[ii]		= src[ii+3];
			src[ii+3]	= temp;

			temp		= src[ii+1];
			src[ii+1]	= src[ii+2];
			src[ii+2]	= temp;
		}

		if (LOG.isDebug2Enabled()) LOG.debug2( "Bytes after swapping = " + bytesToString(src) );

		LOG.leaveMethod( "Util.bytesToLittleEndian(byte[])" );
	}

	/**
	 * Clears a StringBuilder by overwriting it's content and setting it to length 0.
	 * After this a toString will return "".
	 *
	 * @param someChars
	 */
	public static void clear (final StringBuilder someChars) {
		char[] filler = new char[someChars.length()];
		Arrays.fill(filler, '0');
		someChars.setLength(0);
		someChars.append(filler);
		someChars.setLength(0);
	}

	/**
	 * Creates a clone of the given byte array.
	 *
	 * @param src the array to be cloned.
	 *
	 * @return An array of bytes equal in length and content to <code>src</code>.
	 */
	public static byte [] cloneByteArray( byte [] src )
	{
	    final byte []	dst = new byte[ src.length ];
		System.arraycopy( src, 0, dst, 0, src.length );
		return dst;
	}

	/**
	 * Creates a new array of the given length.  If length is shorter than <code>src.length</code>
	 * then the new array contains the contents of <code>src</code> truncated at <code>length</code>
	 * bytes.  If length is greater than <code>src.length</code> then the new array is a copy of
	 * <code>src</code> with the excess bytes set to zero.
	 *
	 * @param src     the array to be cloned.
	 * @param length  the size of the new array.
	 *
	 * @return The new array.
	 */
	public static byte [] cloneByteArray( byte [] src, int length )
	{
	    int		max	= (length < src.length) ? length : src.length;
		final byte []	dst = new byte[ length ];

		System.arraycopy( src, 0, dst, 0, max );
		return dst;
	}

	/**
	 * Extracts an int from a byte array.  The value is four bytes in little-endian order starting
	 * at <code>offset</code>.
	 *
	 * @param buff   the array to extract the int from.
	 * @param offset the offset to start reading from.
	 *
	 * @return The value extracted.
	 *
	 * @throws IndexOutOfBoundsException if offset is negative or <code>buff.length</code> &lt; <code>offset + 4</code>.
	 */
	public static int getIntFromByteArray( byte [] buff, int offset )
	{
		int result;

		result = (buff[offset+0] & 0x000000ff)
			| ((buff[offset+1] & 0x000000ff) << 8 )
			| ((buff[offset+2] & 0x000000ff) << 16 )
			| ((buff[offset+3] & 0x000000ff) << 24 );

		return result;
	}

    /**
     * Extracts an short from a byte array.  The value is two bytes in little-endian order starting
     * at <code>offset</code>.
     *
     * @param buff   the array to extract the short from.
     * @param offset the offset to start reading from.
     *
     * @return The value extracted.
     *
     * @throws IndexOutOfBoundsException if offset is negative or <code>buff.length</code> &lt; <code>offset + 2</code>.
     */
    public static short getShortFromByteArray( byte [] buff, int offset )
    {
        short result;

        result = (short)((buff[offset+0] & 0x00ff)
                        | ((buff[offset+1] & 0x00ff) << 8 ));

        return result;
    }

	/**
	 * Returns a random byte in the range -128 to +127.
	 *
	 * @return A random byte.
	 */
	public static byte newRand() {
		final byte[] rand = new byte[1];
		randGen.nextBytes(rand);
		return rand[0];
	}

	/**
     * fills <code>bytes[]</code> with random bytes using newRand()
     */
	public static void newRandBytes(byte[] bytes) {
       randGen.nextBytes(bytes);
    }

	/**
	 * Returns a random positive integer in the range 0 to <code>Integer.MAX_VALUE</code>.
	 *
	 * @return A random integer in the range 0 to <code>Integer.MAX_VALUE</code>.
	 */
	public static int positiveRand() {
		return Math.abs(randGen.nextInt());
	}

	/**
	 * Stores an integer in little endian order into <code>buff</code> starting at
	 * offset <code>offset</code>.
	 *
	 * @param buff   the buffer to store the integer into.
	 * @param value  the integer value to store.
	 * @param offset the offset at which to store the value.
	 */
	public static void putIntToByteArray( byte [] buff, int value, int offset )
	{
		buff[offset+0]	= (byte)(value & 0xff);
		buff[offset+1]	= (byte)((value & 0xff00) >>> 8);
		buff[offset+2]	= (byte)((value & 0xff0000) >>> 16);
		buff[offset+3]	= (byte)((value & 0xff000000) >>> 24);
	}

    /**
     * Stores an short in little endian order into <code>buff</code> starting at
     * offset <code>offset</code>.
     *
     * @param buff   the buffer to store the short into.
     * @param value  the short value to store.
     * @param offset the offset at which to store the value.
     */
    public static void putShortToByteArray( byte [] buff, short value, int offset )
    {
        buff[offset+0]  = (byte)(value & 0xff);
        buff[offset+1]  = (byte)((value & 0xff00) >>> 8);
    }

	/**
	 * Convert an unsigned set of bytes to a signed set.
	 *
	 * @param ints an array of signed bytes
	 * @return an array of unsigned bytes
	 */
	public static byte[] unsignedToSigned(int []ints) {
		byte[] result = new byte[ints.length];
		for (int i=0; i<ints.length; i++) {
			result[i] = (byte) (ints[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Extracts an milliseconds from seconds strored in a byte array.
	 * The value is four bytes in little-endian order starting
	 * at <code>offset</code>.
	 *
	 * @param buff   the array to extract the millis from.
	 * @param offset the offset to start reading from.
	 *
	 * @return The value extracted.
	 *
	 * @throws IndexOutOfBoundsException if offset is negative or <code>buff.length</code> &lt; <code>offset + 4</code>.
	 */
	public static long getMillisFromByteArray( byte [] buff, int offset )
	{

		long result;

		result = (buff[offset+0] & 0x000000ff)
			| ((buff[offset+1] & 0x000000ff) << 8 )
			| ((buff[offset+2] & 0x000000ff) << 16 )
			| ((buff[offset+3] & 0x000000ff) << 24 );

		result *= 1000L; // convert from seconds to millis

		return result;
	}

	/**
	 * Stores a long milliseconds as seconds in a byte array.
	 * The value is four bytes in little-endian order starting
	 * at <code>offset</code>.
	 *
	 * @param buff   the buffer to store the seconds into.
	 * @param value  the millis long value to store.
	 * @param offset the offset at which to store the value.
	 */
	public static void putMillisToByteArray( byte [] buff, long value, int offset )
	{
		value /= 1000L; // convert from millis to seconds

		buff[offset+0]	= (byte)(value & 0xff);
		buff[offset+1]	= (byte)((value & 0xff00) >>> 8);
		buff[offset+2]	= (byte)((value & 0xff0000) >>> 16);
		buff[offset+3]	= (byte)((value & 0xff000000) >>> 24);

	}

	/**
	 * Calculate stretched key.
	 *
	 * http://www.schneier.com/paper-low-entropy.pdf (Section 4.1),
	 * with SHA-256 as the hash function, and ITER iterations
	 * (at least 2048, i.e., t = 11).
	 * @param passphrase the user entered passphrase
	 * @param salt the salt from the file
	 * @param iter the number of iters from the file
	 * @return the stretched user key for comparison
	 */
	public static byte[] stretchPassphrase(byte[] passphrase, byte[] salt, int iter) {
		byte[] p = mergeBytes(passphrase, salt);

		return SHA256Pws.digestN(p, iter);
	}
}
