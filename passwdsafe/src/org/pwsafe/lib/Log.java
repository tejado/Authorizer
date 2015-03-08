/*
 * $Id: Log.java 411 2009-09-25 18:19:34Z roxon $
 *
 * Copyright (c) 2008-2009 David Muller <roxon@users.sourceforge.net>.
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package org.pwsafe.lib;

/**
 * This class provides logging facilities using commons logging.
 *
 * @author Kevin Preece
 */
public class Log
{
    private String itsTag = "Log";
	private int	DebugLevel;

	private Log( String name )
	{
	    itsTag = name;
		setDebugLevel( 3 );
	}

	/**
	 * Returns an instance of <code>Log</code> for the logger named <code>name</code>.
	 *
	 * @param name the logger name.
	 *
	 * @return An <code>Log</code> instance.
	 */
	public static Log getInstance( String name )
	{
		return new Log( name );
	}

	/**
	 * Returns an instance of <code>Log</code> for the logger named <code>Class.name</code>.
	 *
	 * @param name the logger name.
	 *
	 * @return An <code>Log</code> instance.
	 */
	public static Log getInstance( Class<?> aClass )
	{
		return new Log( aClass.getName() );
	}

	/**
	 * Writes a message at debug level 1
	 *
	 * @param msg the message to issue.
	 */
	public final void debug1( String msg )
	{
		if ( isDebug1Enabled() )
		{
		    android.util.Log.d(itsTag, msg);
		}
	}

	/**
	 * Writes a message at debug level 2
	 *
	 * @param msg the message to issue.
	 */
	public final void debug2( String msg )
	{
		if ( isDebug2Enabled() )
		{
		    android.util.Log.d(itsTag, msg);
		}
	}

	/**
	 * Writes a message at debug level 3
	 *
	 * @param msg the message to issue.
	 */
	public final void debug3( String msg )
	{
		if ( isDebug3Enabled() )
		{
		    android.util.Log.d( itsTag, msg );
		}
	}

	/**
	 * Writes a message at debug level 4
	 *
	 * @param msg the message to issue.
	 */
	public final void debug4( String msg )
	{
		if ( isDebug4Enabled() )
		{
		    android.util.Log.d( itsTag, msg );
		}
	}

	/**
	 * Writes a message at debug level 5
	 *
	 * @param msg the message to issue.
	 */
	public final void debug5( String msg )
	{
		if ( isDebug5Enabled() )
		{
		    android.util.Log.d( itsTag, msg );
		}
	}

	/**
	 * Logs entry to a method.
	 *
	 * @param method the method name.
	 */
	public final void enterMethod( String method )
	{
		if ( isDebug1Enabled() )
		{
			if ( !method.endsWith( ")" ) )
			{
				method = method + "()";
			}
			android.util.Log.d( itsTag, "Entering method " + method );
		}
	}

	/**
	 * Writes a message at error level
	 *
	 * @param msg the message to issue.
	 */
	public final void error( String msg )
	{
	    android.util.Log.e( itsTag, msg );
	}

	/**
	 * Writes a message at error level along with details of the exception
	 *
	 * @param msg    the message to issue.
	 * @param except the exeption to be logged.
	 */
	public final void error( String msg, Throwable except )
	{
	    android.util.Log.e( itsTag, msg, except );
	}

	/**
	 * Logs the exception at a level of error.
	 *
	 * @param except the <code>Exception</code> to log.
	 */
	public final void error( Throwable except )
	{
		android.util.Log.e( itsTag, "An Exception has occurred", except );
	}

	/**
	 * Returns the current debug level.
	 *
	 * @return Returns the debugLevel.
	 */
	public final int getDebugLevel()
	{
		return DebugLevel;
	}

	/**
	 * Writes a message at info level
	 *
	 * @param msg the message to issue.
	 */
	public final void info( String msg )
	{
	    android.util.Log.i( itsTag, msg );
	}

	/**
	 * Returns <code>true</code> if debuuging at level 1 is enabled, <code>false</code> if it isn't.
	 *
	 * @return <code>true</code> if debuuging at level 1 is enabled, <code>false</code> if it isn't.
	 */
	public final boolean isDebug1Enabled()
	{
	    return android.util.Log.isLoggable(itsTag, android.util.Log.DEBUG);
	}

	/**
	 * Returns <code>true</code> if debuuging at level 2 is enabled, <code>false</code> if it isn't.
	 *
	 * @return <code>true</code> if debuuging at level 2 is enabled, <code>false</code> if it isn't.
	 */
	public final boolean isDebug2Enabled()
	{
		return isDebug1Enabled() && (DebugLevel >= 2);
	}

	/**
	 * Returns <code>true</code> if debuuging at level 3 is enabled, <code>false</code> if it isn't.
	 *
	 * @return <code>true</code> if debuuging at level 3 is enabled, <code>false</code> if it isn't.
	 */
	public final boolean isDebug3Enabled()
	{
		return isDebug1Enabled() && (DebugLevel >= 3);
	}

	/**
	 * Returns <code>true</code> if debuuging at level 4 is enabled, <code>false</code> if it isn't.
	 *
	 * @return <code>true</code> if debuuging at level 4 is enabled, <code>false</code> if it isn't.
	 */
	public final boolean isDebug4Enabled()
	{
		return isDebug1Enabled() && (DebugLevel >= 4);
	}

	/**
	 * Returns <code>true</code> if debuuging at level 5 is enabled, <code>false</code> if it isn't.
	 *
	 * @return <code>true</code> if debuuging at level 5 is enabled, <code>false</code> if it isn't.
	 */
	public final boolean isDebug5Enabled()
	{
		return isDebug1Enabled() && (DebugLevel >= 5);
	}

	/**
	 * Logs exit from a method.
	 *
	 * @param method the method name.
	 */
	public final void leaveMethod( String method )
	{
		if ( isDebug1Enabled() )
		{
			if ( !method.endsWith( ")" ) )
			{
				method = method + "()";
			}
			android.util.Log.d( itsTag, "Leaving method " + method );
		}
	}

	/**
	 * Sets the debug level.
	 *
	 * @param debugLevel The debugLevel to set.
	 */
	public final void setDebugLevel( int debugLevel )
	{
		if ( debugLevel < 1 )
		{
			debugLevel = 1;
		}
		else if ( debugLevel > 5 )
		{
			debugLevel = 5;
		}
		DebugLevel = debugLevel;
	}

	/**
	 * Logs a message at the warning level.
	 *
	 * @param msg the message to issue.
	 */
	public final void warn( String msg )
	{
	    android.util.Log.w( itsTag, msg );
	}

}
