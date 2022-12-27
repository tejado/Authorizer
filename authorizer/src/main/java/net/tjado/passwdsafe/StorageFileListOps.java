/*
 * Copyright (©) 2022 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe;

/**
 * The StorageFileListOps interface provides operations needed by a
 * StorageFileListHolder from its owning fragment
 */
public interface StorageFileListOps
{
    /**
     * Notification that a storage file was clicked
     */
    void storageFileClicked(String uri, String title);

    /**
     * Get the icon resource to display for a file
     */
    int getStorageFileIcon();
}
