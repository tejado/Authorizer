/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.file;

/**
 * Observer interface for password file changes
 */
public interface PasswdFileDataObserver
{
    /** Notification that the password file has changed */
    void passwdFileDataChanged(PasswdFileData fileData);
}
