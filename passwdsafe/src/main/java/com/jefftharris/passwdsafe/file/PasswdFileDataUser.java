/*
 * Copyright (©) 2015 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.file;

import android.support.annotation.NonNull;

/**
 * Interface for users of password file data
 */
public interface PasswdFileDataUser
{
    /**
     * Callback to use the password file data
     */
    void useFileData(@NonNull PasswdFileData fileData);
}
