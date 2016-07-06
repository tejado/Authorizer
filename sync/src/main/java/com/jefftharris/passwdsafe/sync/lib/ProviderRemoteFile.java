/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.lib;

/**
 * Abstraction of a remote file for operations
 */
public interface ProviderRemoteFile
{
    String PATH_SEPARATOR = "/";

    /** Get the file's remote identifier */
    String getRemoteId();

    /** Get the file's path for display */
    String getDisplayPath();

    /** Get the file's title */
    String getTitle();

    /** Get the file's folder */
    String getFolder();

    /** Get the file's modification time */
    long getModTime();

    /** Get the file's hash code */
    String getHash();

    /** Is the file a folder */
    boolean isFolder();

    /** Get a debugging string for the file */
    String toDebugString();
}
