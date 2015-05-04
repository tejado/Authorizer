/*
 * $Id: DefaultRecordWrapper.java 404 2009-09-21 19:19:25Z roxon $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.util;

import org.pwsafe.lib.file.PwsRecord;
import org.pwsafe.lib.file.PwsRecordV1;
import org.pwsafe.lib.file.PwsRecordV2;

/**
 * Swing helper class, will be moved to PasswordJ project.
 * @deprecated
 */
@SuppressWarnings("ALL")
@Deprecated
public class DefaultRecordWrapper
{
	private final PwsRecord	Record;

	/**
	 * 
	 * @param rec
	 */
	public DefaultRecordWrapper( PwsRecord rec )
	{
		Record	= rec;
	}

	/**
	 * 
	 * @return
	 */
	public PwsRecord getRecord()
	{
		return Record;
	}

	/**
	 * 
	 * 
	 * @return
	 */
	@Override
	public String toString()
	{
		if ( Record instanceof PwsRecordV1 )
		{
			return ((PwsRecordV1) Record).getField(PwsRecordV1.TITLE).toString();
		}
		else if ( Record instanceof PwsRecordV2 )
		{
			return ((PwsRecordV2) Record).getField(PwsRecordV2.TITLE).toString();
		}
		return null;
	}
}
