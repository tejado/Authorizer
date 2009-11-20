/*
 * $Id$
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.crypto;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.MappedByteBuffer;
import java.nio.ShortBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;
import java.util.Arrays;

import org.pwsafe.lib.Log;

/**
 * Tries to hide create a key which is hidden from an occasional onlooker.
 * Uses a memory mapped temporary file to distribute the key data.
 * <p>
 * It's unchecked whether this makes any difference for swapped out memory.
 * <p><b>NOTE:</b> ATM, the temporary file is probably not deleted! On Windows, 
 * java ignores File.deleteOnExit  
 * 
 * @author roxon
 */
public class MemoryMappedKey {
	private final static Log LOG = Log.getInstance(MemoryMappedKey.class);
	
	private final static int BUFFER_SIZE = 1024;
	final short[] access;
	private File tempFile;
	FileChannel channel;
	RandomAccessFile outlet;
	MappedByteBuffer buffer;
	
	public MemoryMappedKey (short aSize) {
		access = new short[aSize];
	}

	public MemoryMappedKey (int aSize) {
		this( (short) aSize);
	}

	public void init () {
		final byte[] accessBytes = new byte[access.length * 2];
		org.pwsafe.lib.Util.newRandBytes(accessBytes);
		ShortBuffer accessShorts = ByteBuffer.wrap(accessBytes).asShortBuffer();
		for (int i = 0; i < access.length ; i++ ) {
			access[i] = accessShorts.get(i); 
		}
		
		try {
			tempFile = File.createTempFile("jpw", ".dat");
			LOG.info("Using temporary file " + tempFile);
			tempFile.deleteOnExit();
			
			outlet = new RandomAccessFile(tempFile, "rw");
			channel = outlet.getChannel();
			buffer = channel.map(MapMode.READ_WRITE, 0, BUFFER_SIZE);
			assert (buffer.isDirect());
			
			for (int i = 0; i < BUFFER_SIZE; i++) {
				buffer.put(org.pwsafe.lib.Util.newRand());
			}
			buffer.flip();
			
		} catch (IOException ioEx) {
			throw new RuntimeException(ioEx);
		}
	}
	
	public byte[] getKey () {
		if (buffer == null) {
			throw new IllegalStateException("InMemoryKey has not been intialised or been disposed");
		}
		
		final byte[] content = new byte[8];
		
		// TODO: use higher bits of short value for content rotate
		for (int i = 0; i < 8; i++) {
			final short pos = access[i];
			content[i] = buffer.get(Math.abs(pos) % BUFFER_SIZE);
		}
		return content;
	}
	
	public void dispose () {
		if (buffer != null) {
			if (buffer.hasArray()) {
				byte[] content = buffer.array();
				Arrays.fill(content, (byte) 0);
			}
			buffer = null;
		}
		if (channel != null && channel.isOpen()) {
			try {
				channel.close();
				channel = null;
			} catch (IOException e) {
				LOG.warn("Exception closing FileChannel: " + e);
			}		
		}
		try {
			if (outlet != null) {
				outlet.close();
			}
		} catch (IOException e) {
			LOG.warn("Exception closing FileChannel: " + e);
		}
		
		// TODO: WHY IS TEMP NOT DELETED????
		if (tempFile != null && tempFile.exists()) {
			final boolean isDeleted = tempFile.delete();
			if (! isDeleted) {
				LOG.warn("Couldn't delete temp key " + tempFile);
			}
		}	
	}

	/**
	 * Can be used to rotate the bytes of the content buffer.
	 *  
	 * @param b
	 * @param distance
	 * @return
	 */
	private byte rotateRight (final byte b, final int distance) {
		return (byte) ((b >>> distance) | (b << -distance));
	}

}
