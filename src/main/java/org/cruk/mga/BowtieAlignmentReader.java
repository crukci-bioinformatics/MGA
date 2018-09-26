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

package org.cruk.mga;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;

public class BowtieAlignmentReader extends AbstractAlignmentReader
{
    private BufferedReader[] readers;
    private int[] lineNumbers;

    public BowtieAlignmentReader(String[] alignmentFiles, String runId) throws IOException
    {
        super("bowtie", alignmentFiles, runId);

        int n = alignmentFiles.length;

        readers = new BufferedReader[n];
        lineNumbers = new int[n];

        for (int i = 0; i < n; i++)
        {
            File file = new File(alignmentFiles[i]);

            readers[i] = new BufferedReader(new FileReader(file));
            lineNumbers[i] = 0;

            Alignment alignment = readAlignment(i);

            if (alignment == null)
            {
                readers[i] = null;
            }
            else
            {
                lookup.put(alignment, i);
            }
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        if (readers != null)
        {
            for (Reader r : readers)
            {
                try
                {
                    r.close();
                }
                catch (IOException e)
                {
                    // Ignore.
                }
            }
        }
    }

    @Override
    protected int getLineNumber(int index)
    {
        return lineNumbers[index];
    }

    @Override
    protected Alignment readAlignment(int index) throws IOException
    {
        BufferedReader reader = readers[index];
        if (reader == null)
        {
            return null;
        }

        String line = reader.readLine();
        if (line == null)
        {
            readerFinished(index);
            return null;
        }

        String[] fields = line.split("\\t", -1);
        lineNumbers[index]++;

        int separatorIndex = fields[0].lastIndexOf("_");
        if (separatorIndex == -1)
        {
            throw new RuntimeException("Incorrect sequence identifier (" + fields[0] + ") at line " + lineNumbers[index] + " in file " + alignmentFiles[index]);
        }

        String datasetId = fields[0].substring(0, separatorIndex);
        int sequenceId = -1;
        try
        {
            sequenceId = Integer.parseInt(fields[0].substring(separatorIndex + 1));
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("Incorrect sequence identifier (" + fields[0] + ") at line " + lineNumbers[index] + " in file " + alignmentFiles[index]);
        }

        int alignedLength = fields[4].length();

        String mismatches = fields[7];
        int mismatchCount = mismatches.isEmpty() ? 0 : mismatches.split(",").length;

        return new Alignment(datasetId, sequenceId, referenceGenomeIds[index], alignedLength, mismatchCount);
    }

    @Override
    protected void readerFinished(int index) throws IOException
    {
        if (readers[index] != null)
        {
            try
            {
                readers[index].close();
            }
            catch (IOException e)
            {
                // Ignore.
            }
            finally
            {
                readers[index] = null;
            }
        }
    }
}
