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

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class AbstractAlignmentReader implements Closeable
{
    protected List<String> alignmentFiles;
    protected String[] referenceGenomeIds;
    protected TreeMap<Alignment, Integer> lookup = new TreeMap<Alignment, Integer>();

    protected AbstractAlignmentReader(String alignerName, String[] allAlignmentFiles, String runId) throws IOException
    {
        // Filter for those alignment files created by the named aligner only.

        String acceptedSuffix = "." + alignerName + ".alignment";

        alignmentFiles = Stream.of(allAlignmentFiles).filter(f -> f.endsWith(acceptedSuffix)).collect(Collectors.toList());

        int n = alignmentFiles.size();

        referenceGenomeIds = new String[n];

        for (int i = 0; i < n; i++)
        {
            File file = new File(alignmentFiles.get(i));

            String referenceGenomeId = file.getName().replaceAll("\\Q" + acceptedSuffix + "\\E$", "").replaceAll("^\\Q" + runId + ".\\E", "");
            int index = referenceGenomeId.indexOf(".");
            if (index == -1)
                throw new RuntimeException("Error determining reference genome for file: " + file.getAbsolutePath());
            referenceGenomeId = referenceGenomeId.substring(index + 1);
            if (referenceGenomeId.isEmpty())
                throw new RuntimeException("Error determining reference genome for file: " + file.getAbsolutePath());
            referenceGenomeIds[i] = referenceGenomeId;
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        close();
    }

    public List<String> getAlignmentFiles()
    {
        return Collections.unmodifiableList(alignmentFiles);
    }

    public Set<String> getReferenceGenomeIds()
    {
        Set<String> referenceGenomeIdSet = new HashSet<String>();
        Collections.addAll(referenceGenomeIdSet, referenceGenomeIds);
        return referenceGenomeIdSet;
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
        {
            readerFinished(index);
        }
        else
        {
            if (alignment.compareTo(newAlignment) >= 0)
            {
                alignment.compareTo(newAlignment);
                throw new RuntimeException("Alignments in unexpected sort order at line " + getLineNumber(index) + " in file " + alignmentFiles.get(index));
            }

            lookup.put(newAlignment, index);
        }

        return alignment;
    }

    protected abstract Alignment readAlignment(int index) throws IOException;

    protected abstract int getLineNumber(int index);

    protected abstract void readerFinished(int index) throws IOException;

}
