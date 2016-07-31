/*
 * $Id: I18nHelperBase.java 317 2009-01-26 20:20:54Z ronys $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 *
 */
@SuppressWarnings("ALL")
public class I18nHelperBase
{
	/**
	 * Log4j logger
	 */
	private static final Log		LOG	= Log.getInstance( I18nHelperBase.class.getPackage().getName() );

	/**
	 * The localised message store.
	 */
	private static ResourceBundle	TheBundle	= null;

	/**
	 * The users preferred locale or the default locale if no preference was given.
	 */
	private static Locale			TheLocale	= Locale.getDefault();

	/**
	 *
	 */
	protected I18nHelperBase()
	{
		super();
	}

	/**
	 * Loads the localised strings using the current locale.
	 *
	 * @return The localised <code>ResourceBundle</code>.
	 */
	private synchronized ResourceBundle getBundle()
	{
		if ( TheBundle == null )
		{
			TheBundle = ResourceBundle.getBundle( getFilename(), TheLocale );
			// TODOlib handle the case where the file cannot be found
			// catch MissingResourceException
		}
		return TheBundle;
	}

	/**
	 * Returns the base name of the properties file that contains the localised
	 * strings.
	 *
	 * @return
	 */
	public String getFilename()
	{
		return "CorelibStrings";
	}

	/**
	 * Sets the locale and forces the <code>ResourceBundle</code> to be reloaded.
	 *
	 * @param locale the locale.
	 */
	public synchronized void setLocale( Locale locale )
	{
		TheLocale	= locale;
		TheBundle	= null;
	}
}
