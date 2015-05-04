/*
 * $Id: PasswordSafeException.java 317 2009-01-26 20:20:54Z ronys $
 * 
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib.exception;

/**
 * A generic exception.
 * 
 * @author Kevin Preece
 */
@SuppressWarnings("ALL")
public class PasswordSafeException extends Exception
{
	/**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
	 * 
	 */
	public PasswordSafeException()
	{
		super();
	}

	/**
	 * @param arg0
	 */
	public PasswordSafeException(String arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 */
	public PasswordSafeException(Throwable arg0)
	{
		super(arg0);
	}

	/**
	 * @param arg0
	 * @param arg1
	 */
	public PasswordSafeException(String arg0, Throwable arg1)
	{
		super(arg0, arg1);
	}
}
