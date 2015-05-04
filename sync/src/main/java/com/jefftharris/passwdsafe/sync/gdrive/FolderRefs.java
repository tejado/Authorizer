/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.util.HashSet;
import java.util.Set;

/**
 * Information about the file references in a folder
 */
class FolderRefs
{
    public final Set<String> itsFileRefs = new HashSet<>();

    public FolderRefs()
    {
    }

    public final void addRef(String fileId)
    {
        itsFileRefs.add(fileId);
    }

    public final void removeRef(String fileId)
    {
        itsFileRefs.remove(fileId);
    }
}
