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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import android.text.TextUtils;

import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.jefftharris.passwdsafe.lib.PasswdSafeUtil;

/**
 * The FileFolders class encapsulates handling of folders for GDrive files
 */
public class FileFolders
{
    private final Drive itsDrive;
    private final HashMap<String, File> itsFileCache;
    private final HashMap<String, FolderRefs> itsFolderRefs;

    private static final String TAG = "FileFolders";

    /** Constructor with external cache and folder refs collections */
    public FileFolders(Drive drive,
                       HashMap<String, File> fileCache,
                       HashMap<String, FolderRefs> folderRefs)
    {
        itsDrive = drive;
        itsFileCache = fileCache;
        itsFolderRefs = folderRefs;
    }


    /** Constructor with internal helper collections */
    public FileFolders(Drive drive)
    {
        this(drive, new HashMap<String, File>(),
                new HashMap<String, FolderRefs>());
    }


    /** Check if any files are contained in the given folder */
    public void checkFolderFiles(File folder,
                                 HashMap<String, File> folderFiles)
            throws IOException
    {
        FolderRefs folderRefs = itsFolderRefs.get(folder.getId());
        if (folderRefs != null) {
            for (String fileId: folderRefs.itsFileRefs) {
                File refFile = getCachedFile(fileId);
                folderFiles.put(fileId, refFile);
            }
        }

    }


    /** Compute the folders for the given file */
    public String computeFileFolders(File file)
            throws IOException
    {
        String id = file.getId();
        Map<String, String> folders = computeFilesFolders(
                Collections.singletonMap(id, file));
        return folders.get(id);
    }


    /** Compute the folders for the given files */
    public Map<String, String>
    computeFilesFolders(Map<String, File> remfiles)
            throws IOException
    {
        HashMap<String, String> fileFolders = new HashMap<>();
        for (File remfile: remfiles.values()) {
            if (remfile == null) {
                continue;
            }

            // Remove the file from the folder refs to handle moves.
            // The file will be re-added to the correct refs
            String id = remfile.getId();
            for (FolderRefs refs: itsFolderRefs.values()) {
                refs.removeRef(id);
            }

            String folders = computeFolders(remfile);
            fileFolders.put(id, folders);
        }
        // Purge empty folder references
        Iterator<Map.Entry<String, FolderRefs>> iter =
                itsFolderRefs.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<String, FolderRefs> entry = iter.next();
            Set<String> fileRefs = entry.getValue().itsFileRefs;
            if (PasswdSafeUtil.DEBUG) {
                PasswdSafeUtil.dbginfo(TAG, "cached folder %s, refs [%s]",
                        entry.getKey(), TextUtils.join(", ", fileRefs));
            }
            if (fileRefs.isEmpty()) {
                iter.remove();
            }
        }

        return fileFolders;
    }


    /** Internal computation for the folders of a file */
    private String computeFolders(File remfile)
            throws IOException
    {
        String fileId = remfile.getId();
        ArrayList<String> folders = new ArrayList<>();
        for (String parentId: remfile.getParents()) {
            traceParentRefs(parentId, "", folders, fileId);
        }
        Collections.sort(folders);
        String foldersStr = TextUtils.join(", ", folders);
        PasswdSafeUtil.dbginfo(TAG, "compFolders %s: %s",
                               remfile.getName(), foldersStr);
        return foldersStr;
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
            file = itsDrive.files().get(id).setFields(
                    GDriveProvider.FILE_FIELDS).execute();
            itsFileCache.put(id, file);
        }
        return file;
    }
}
