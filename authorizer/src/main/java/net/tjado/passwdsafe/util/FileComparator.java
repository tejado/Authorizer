/*
 * Copyright (©) 2016 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.util;

import java.io.File;
import java.util.Comparator;

/**
 * File comparator that sorts files before directories
 */
public class FileComparator implements Comparator<File>
{
    /** Compare the two files */
    public int compare(File obj1, File obj2)
    {
        if (obj1.isDirectory() && !obj2.isDirectory()) {
            return 1;
        } else if (!obj1.isDirectory() && obj2.isDirectory()) {
            return -1;
        }
        return obj1.compareTo(obj2);
    }
}
