/*
 * $Id: I18nHelper.java 317 2009-01-26 20:20:54Z ronys $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib;

/**
 * A singleton class to help provide messages in the users preferred language.  Messages
 * are stored in a file called CorelibStrings.properties and, where translations have
 * been provided, the appropriate localised version, CorelibStrings_en_US.properties
 * for example.
 * 
 * @author Kevin Preece
 */
public class I18nHelper extends I18nHelperBase
{
	private static final I18nHelper	TheInstance	= new I18nHelper();

	/**
	 * Private for the singleton pattern. 
	 */
	private I18nHelper()
	{
	}

	/**
	 * Returns an instance of I18nHelper.
	 * 
	 * @return An instance of I18nHelper.
	 */
	public static I18nHelper getInstance()
	{
		return TheInstance;
	}
}
