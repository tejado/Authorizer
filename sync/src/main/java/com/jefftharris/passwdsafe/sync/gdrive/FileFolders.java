/*
 * Copyright (©) 2014 Jeff Harris <jefftharris@gmail.com> All rights reserved.
 * Use of the code is allowed under the Artistic License 2.0 terms, as specified
 * in the LICENSE file distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package com.jefftharris.passwdsafe.sync.gdrive;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import android.text.TextUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;

/**
 * The FileFolders class encapsulates handling of folders for GDrive files
 */
public class FileFolders
{
    private final Drive itsDrive;
    private final HashMap<String, File> itsFileCache = new HashMap<>();
    private final HashMap<String, FolderRefs> itsFolderRefs = new HashMap<>();

    /** Constructor with internal helper collections */
    public FileFolders(Drive drive)
    {
        itsDrive = drive;
    }


    /** Compute the folders for the given file */
    public String computeFileFolders(File file)
            throws IOException
    {
        String fileId = file.getId();
        ArrayList<String> folders = new ArrayList<>();
        for (String parentId: file.getParents()) {
            traceParentRefs(parentId, "", folders, fileId);
        }
        Collections.sort(folders);
        return TextUtils.join(", ", folders);
    }


    /** Trace the parent references for a file to compute the full paths of
     *  its folders */
    private void traceParentRefs(String parentId,
                                 String suffix,
                                 ArrayList<String> folders,
                                 String fileId)
            throws IOException
    {
        File parentFile = getCachedFile(parentId);
        if (parentFile.getParents() == null) {
            suffix = parentFile.getName() + suffix;
            folders.add(suffix);
        } else {
            FolderRefs refs = itsFolderRefs.get(parentId);
            if (refs == null) {
                refs = new FolderRefs();
                itsFolderRefs.put(parentId, refs);
            }
            refs.addRef(fileId);
            suffix = "/" + parentFile.getName() + suffix;
            for (String parentParentId: parentFile.getParents()) {
                traceParentRefs(parentParentId, suffix, folders, fileId);
            }
        }
    }


    /** Get a cached file */
    private File getCachedFile(String id)
            throws IOException
    {
        File file = itsFileCache.get(id);
        if (file == null) {
            file = itsDrive.files().get(id)
                           .setFields(GDriveProvider.FILE_FIELDS).execute();
            itsFileCache.put(id, file);
        }
        return file;
    }
}
