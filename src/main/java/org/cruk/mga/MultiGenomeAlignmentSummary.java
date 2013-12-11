/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Cancer Research UK Cambridge Institute
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cruk.util.OrderedProperties;

/**
 * Class used to represent the summary of alignments of a sequence
 * dataset against a multiple reference genomes.
 * 
 * @author eldrid01
 *
 */
public class MultiGenomeAlignmentSummary
{
    private String datasetId;
    private long sequenceCount;
    private int sampledCount;
    private int trimLength;
    private int adapterCount;
    private int unmappedCount;
    private Map<String, AlignmentSummary> alignmentSummaries = new HashMap<String, AlignmentSummary>();
    private List<OrderedProperties> sampleProperties = new ArrayList<OrderedProperties>();

    /**
     * @return the datasetId
     */
    public String getDatasetId()
    {
        return datasetId;
    }

    /**
     * @param datasetId the datasetId to set
     */
    public void setDatasetId(String datasetId)
    {
        this.datasetId = datasetId;
    }

    /**
     * @return the sequenceCount
     */
    public long getSequenceCount()
    {
        return sequenceCount;
    }

    /**
     * @param sequenceCount the sequenceCount to set
     */
    public void setSequenceCount(long sequenceCount)
    {
        this.sequenceCount = sequenceCount;
    }

    /**
     * @return the sampledCount
     */
    public int getSampledCount()
    {
        return sampledCount;
    }

    /**
     * @param sampledCount the sampledCount to set
     */
    public void setSampledCount(int sampledCount)
    {
        this.sampledCount = sampledCount;
    }

    /**
     * @return the trim length
     */
    public int getTrimLength()
    {
        return trimLength;
    }

    /**
     * @param trimLength the trim length
     */
    public void setTrimLength(int trimLength)
    {
        this.trimLength = trimLength;
    }

    /**
     * @return the adapterCount
     */
    public int getAdapterCount()
    {
        return adapterCount;
    }

    /**
     * @param adapterCount the adapterCount to set
     */
    public void setAdapterCount(int adapterCount)
    {
        this.adapterCount = adapterCount;
    }

    /**
     * @return the unmappedCount
     */
    public int getUnmappedCount()
    {
        return unmappedCount;
    }

    /**
     * @param unmappedCount the unmappedCount to set
     */
    public void setUnmappedCount(int unmappedCount)
    {
        this.unmappedCount = unmappedCount;
    }

    /**
     * @return the alignmentSummaries
     */
    public AlignmentSummary[] getAlignmentSummaries()
    {
        return alignmentSummaries.values().toArray(new AlignmentSummary[0]);
    }

    /**
     * Add an alignment summary for a specific reference genome.
     *
     * @param alignmentSummary the alignment summary.
     */
    public void addAlignmentSummary(AlignmentSummary alignmentSummary)
    {
        alignmentSummaries.put(alignmentSummary.getReferenceGenomeId(), alignmentSummary);
    }

    /**
     * Returns the alignment summary for the given reference genome.
     *
     * @param referenceGenomeId the reference genome identifier.
     * @return
     */
    public AlignmentSummary getAlignmentSummary(String referenceGenomeId)
    {
        return alignmentSummaries.get(referenceGenomeId);
    }

    /**
     * @return the sample properties
     */
    public List<OrderedProperties> getSampleProperties()
    {
        return sampleProperties;
    }
}
