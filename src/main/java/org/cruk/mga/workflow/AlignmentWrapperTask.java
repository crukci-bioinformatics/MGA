/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Cancer Research UK Cambridge Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.cruk.mga.workflow;

import java.io.File;
import java.io.IOException;

import org.cruk.workflow.execution.TaskMonitor;
import org.cruk.workflow.execution.TaskRunner;
import org.cruk.workflow.tasks.AbstractJavaTask;
import org.cruk.workflow.xml2.pipeline.Subtasks;
import org.cruk.workflow.xml2.pipeline.Task;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Wrapper for alignment tasks that detects zero-sized query file and
 * creates an empty alignment output file instead of calling the aligner.
 *
 * @author eldrid01
 */
public class AlignmentWrapperTask extends AbstractJavaTask
{
    private File queryFile;
    private File alignmentFile;

    @Autowired
    private TaskRunner taskRunner;

    public File getQueryFile()
    {
        return queryFile;
    }

    public void setQueryFile(File queryFile)
    {
        this.queryFile = queryFile;
    }

    public File getAlignmentFile()
    {
        return alignmentFile;
    }

    public void setAlignmentFile(File alignmentFile)
    {
        this.alignmentFile = alignmentFile;
    }

    @Override
    public void execute(TaskMonitor arg0) throws Throwable
    {
        // if zero-size query fasta file create an empty alignment file to avoid
        // exonerate error
        if (queryFile.length() == 0)
        {
            boolean success = alignmentFile.createNewFile();
            if (!success)
            {
                throw new IOException("Failed to create alignment file " + alignmentFile.getAbsolutePath());
            }
        }
        else
        {
            Subtasks subtasks = task.getSubtasks();
            if (subtasks.size() != 1)
            {
                throw new Exception("Expecting a single subtask but found " + subtasks.size());
            }
            else
            {
                Task subtask = subtasks.iterator().next();
                TaskMonitor subtaskMonitor = taskRunner.runTask(configuration, subtask);
                waitForMonitorsAndThrow("Alignment task failed", subtaskMonitor);
            }
        }
    }
}
