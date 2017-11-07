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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;

public class AlignmentReader
{
    private String[] alignmentFiles;
    private BufferedReader[] readers;
    private int[] lineNumbers;
    private String[] referenceGenomeIds;
    private TreeMap<Alignment, Integer> lookup = new TreeMap<Alignment, Integer>();

    public AlignmentReader(String[] alignmentFiles, String runId) throws IOException
    {
        this.alignmentFiles = alignmentFiles;

        int n = alignmentFiles.length;

        readers = new BufferedReader[n];
        lineNumbers = new int[n];
        referenceGenomeIds = new String[n];

        for (int i = 0; i < n; i++)
        {
            File file = new File(alignmentFiles[i]);

            readers[i] = new BufferedReader(new FileReader(file));
            lineNumbers[i] = 0;

            String referenceGenomeId = file.getName().replaceAll("\\.bowtie\\.alignment$", "").replaceAll("^" + runId + "\\.", "");
            int index = referenceGenomeId.indexOf(".");
            if (index == -1)
                throw new RuntimeException("Error determining reference genome for file: " + file.getAbsolutePath());
            referenceGenomeId = referenceGenomeId.substring(index + 1);
            if (referenceGenomeId.isEmpty())
                throw new RuntimeException("Error determining reference genome for file: " + file.getAbsolutePath());
            referenceGenomeIds[i] = referenceGenomeId;

            Alignment alignment = readAlignment(i);

            if (alignment == null)
                readers[i] = null;
            else
                lookup.put(alignment, i);
        }
    }

    public Set<String> getReferenceGenomeIds()
    {
        Set<String> referenceGenomeIdSet = new HashSet<String>();
        Collections.addAll(referenceGenomeIdSet, referenceGenomeIds);
        return referenceGenomeIdSet;
    }

    public int getLineNumber(int index)
    {
        return lineNumbers[index];
    }

    public List<Alignment> getNextAlignments() throws IOException
    {
        List<Alignment> alignments = new ArrayList<Alignment>();

        Alignment alignment = getNextAlignment();
        if (alignment == null) return alignments;

        alignments.add(alignment);

        String datasetId = alignment.getDatasetId();
        int sequenceId = alignment.getSequenceId();

        while (true)
        {
            Entry<Alignment, Integer> entry = lookup.firstEntry();
            if (entry == null) break;

            alignment = entry.getKey();
            if (alignment == null || !alignment.getDatasetId().equals(datasetId) || alignment.getSequenceId() != sequenceId) break;

            alignments.add(alignment);

            getNextAlignment();
        }

        return alignments;
    }

    public Alignment getNextAlignment() throws IOException
    {
        if (lookup.isEmpty()) return null;

        Entry<Alignment, Integer> entry = lookup.pollFirstEntry();
        Alignment alignment = entry.getKey();
        int index = entry.getValue();

        Alignment newAlignment = readAlignment(index);

        if (newAlignment == null)
            readers[index] = null;
        else
        {
            if (alignment.compareTo(newAlignment) >= 0)
                throw new RuntimeException("Alignments in unexpected sort order at line " + lineNumbers[index] + " in file " + alignmentFiles[index]);

            lookup.put(newAlignment, index);
        }

        return alignment;
    }

    private Alignment readAlignment(int index) throws IOException
    {
        BufferedReader reader = readers[index];

        String line = reader.readLine();
        if (line == null)
        {
            reader.close();
            return null;
        }

        String[] fields = line.split("\\t", -1);
        lineNumbers[index]++;

        int separatorIndex = fields[0].lastIndexOf("_");
        if (separatorIndex == -1)
            throw new RuntimeException("Incorrect sequence identifier (" + fields[0] + ") at line " + lineNumbers[index] + " in file " + alignmentFiles[index]);

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
}
