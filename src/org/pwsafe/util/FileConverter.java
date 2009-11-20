/*
 * $Id: FileConverter.java 404 2009-09-21 19:19:25Z roxon $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.util;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

import org.pwsafe.lib.Log;
import org.pwsafe.lib.exception.PasswordSafeException;
import org.pwsafe.lib.file.PwsFile;
import org.pwsafe.lib.file.PwsFileStorage;
import org.pwsafe.lib.file.PwsFileV1;
import org.pwsafe.lib.file.PwsFileV2;
import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV1;
import org.pwsafe.lib.file.PwsRecordV2;

/**
 * This singleton class contains methods for converting PasswordSafe databases
 * between formats.
 * 
 * @author Kevin Preece
 */
public class FileConverter
{
	private static final Log LOGGER = Log.getInstance(FileConverter.class.getName());
	/**
	 * Private for the singleton pattern.
	 */
	private FileConverter()
	{
		super();
	}

	/**
	 * Converts <code>oldFile</code> to the latest supported version, currently version 2.  If
	 * the file is already the latest format no new file is created and the reference is simply
	 * returned as-is.
	 * 
	 * @param oldFile the file to be converted.
	 * 
	 * @return A file in the latest format containing the data from <code>oldFile</code>.
	 * 
	 * @throws IOException
	 * @throws PasswordSafeException 
	 */
	public static PwsFile convertToLatest( PwsFile oldFile )
	throws IOException, PasswordSafeException
	{
		if ( oldFile instanceof PwsFileV2 )
		{
			return oldFile;
		}
		return convertV1ToV2( (PwsFileV1) oldFile );
	}

	/**
	 * Converts a version 1 PasswordSafe database to version 2.
	 * 
	 * @param oldFile the database to convert.
	 * 
	 * @return The version 2 database.
	 * 
	 * @throws IOException
	 * @throws PasswordSafeException 	 
	 */ 
	public static PwsFile convertV1ToV2( PwsFileV1 oldFile )
	throws IOException, PasswordSafeException
	{
		PwsFileV2	newFile;
		PwsRecordV1	oldRec;
		PwsRecord	newRec;

		newFile = new PwsFileV2();

		newFile.setPassphrase( new StringBuilder( oldFile.getPassphrase() ));
		PwsFileStorage oldStorage = (PwsFileStorage) oldFile.getStorage();
		PwsFileStorage newStorage = new PwsFileStorage(makeNewFilename(oldStorage.getFilename(), "v2-"));
		newFile.setStorage( newStorage );

		for ( Iterator iter = oldFile.getRecords(); iter.hasNext(); )
		{
			oldRec	= (PwsRecordV1) iter.next();
			newRec	= newFile.newRecord();

			newRec.setField( oldRec.getField(PwsRecordV1.TITLE) );
			newRec.setField( oldRec.getField(PwsRecordV1.USERNAME) );
			newRec.setField( oldRec.getField(PwsRecordV1.PASSWORD) );
			newRec.setField( oldRec.getField(PwsRecordV1.NOTES) );
			newFile.add(newRec);
		}

		return newFile;
	}

	/**
	 * Converts a version 2 PasswordSafe database to version 1.  Note this will
	 * result in data loss since fields not supported by version 1 will be
	 * silently dropped.
	 * 
	 * @param oldFile the file to be converted.
	 * 
	 * @return The version 1 database.
	 * 
	 * @throws IOException
	 * @throws PasswordSafeException 
	 */
	public static PwsFile convertV2ToV1( PwsFileV2 oldFile )
	throws IOException, PasswordSafeException
	{
		PwsFileV1	newFile;
		PwsRecordV2	oldRec;
		PwsRecord	newRec;

		newFile = new PwsFileV1();

		newFile.setPassphrase( new StringBuilder(oldFile.getPassphrase()) );
		PwsFileStorage oldStorage = (PwsFileStorage) oldFile.getStorage();
		PwsFileStorage newStorage = new PwsFileStorage(makeNewFilename(oldStorage.getFilename(), "v1-"));
		newFile.setStorage( newStorage );
		for ( Iterator iter = oldFile.getRecords(); iter.hasNext(); )
		{
			oldRec	= (PwsRecordV2) iter.next();
			newRec	= newFile.newRecord();

			newRec.setField( oldRec.getField(PwsRecordV2.TITLE) );
			newRec.setField( oldRec.getField(PwsRecordV2.USERNAME) );
			newRec.setField( oldRec.getField(PwsRecordV2.PASSWORD) );
			newRec.setField( oldRec.getField(PwsRecordV2.NOTES) );
			newFile.add(newRec);
		}
		return newFile;
	}

	/**
	 * Makes a new filename from the given filename and prefix.  The prefix
	 * is prepended to the name.  For example if <code>filename</code> is
	 * "C:\Program Files\Java\PasswordSafe\MyPasswords.dat" and
	 * <code>prefix</code> is "V2-", the new filename would be
	 * "C:\Program Files\Java\PasswordSafe\V2-MyPasswords.dat"
	 *  
	 * @param filename
	 * @param prefix
	 * @return
	 */
	private static String makeNewFilename( String filename, String prefix )	{
		
		File 			file	= new File( filename );
		String			path	= file.getParent() != null ? file.getParent() : "";
		String			name	= file.getName();
		StringBuilder 	sb		= new StringBuilder(path.length() + prefix.length() + name.length());
		String			newName	= sb.append(path).append(prefix).append(name).toString();
		
		file	= new File( newName );

		if ( file.exists() )
		{
			// TODO generate a temporary filename
			throw new IllegalStateException("new File already exists: " + file.getAbsolutePath());
		}

		return newName;
	}
}
