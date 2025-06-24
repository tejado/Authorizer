/*
 * Copyright (©) 2017 Jeff Harris <jefftharris@gmail.com>
 * All rights reserved. Use of the code is allowed under the
 * Artistic License 2.0 terms, as specified in the LICENSE file
 * distributed with this code, or available from
 * http://www.opensource.org/licenses/artistic-license-2.0.php
 */
package net.tjado.passwdsafe.lib;

import java.util.ArrayList;
import java.util.List;

/**
 * Set of managed tasks
 */
public final class ManagedTasks
{
    private List<ManagedTask<?,?>> itsTasks = new ArrayList<>();

    /**
     * Constructor
     */
    public ManagedTasks()
    {
    }

    /**
     * Start a managed task
     */
    public void startTask(ManagedTask<?,?> task)
    {
        itsTasks.add(task);
        task.start();
    }

    /**
     * Cancel the tasks
     */
    public void cancelTasks()
    {
        List<ManagedTask<?,?>> tasks = itsTasks;
        itsTasks = new ArrayList<>();
        for (ManagedTask<?,?> task: tasks) {
            task.cancel();
        }
    }

    /**
     * Update the tasks when one is finished
     */
    public void taskFinished(ManagedTask<?,?> task)
    {
        itsTasks.remove(task);
    }
}
