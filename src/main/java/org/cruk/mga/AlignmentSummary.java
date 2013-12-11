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

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Class used to represent the summary of an alignment of a sequence
 * dataset against a specific reference genome.
 *
 * @author eldrid01
 */
public class AlignmentSummary
{
    private String referenceGenomeId;
    private int totalAlignedSequenceLength = 0;
    private int totalMismatchCount = 0;
    private int alignedCount = 0;
    private int assignedCount = 0;
    private Set<Integer> sequenceIds = new HashSet<Integer>();

    /**
     * @return the referenceGenomeId
     */
    public String getReferenceGenomeId()
    {
        return referenceGenomeId;
    }

    /**
     * @param referenceGenomeId the referenceGenomeId to set
     */
    public void setReferenceGenomeId(String referenceGenomeId)
    {
        this.referenceGenomeId = referenceGenomeId;
    }

    /**
     * @return the total aligned sequence length.
     */
    public int getTotalAlignedSequenceLength()
    {
        return totalAlignedSequenceLength;
    }

    /**
     * Adds the given aligned sequence length to the total.
     *
     * @param alignedSequenceLength the aligned sequence length.
     */
    public void addAlignedSequenceLength(int alignedSequenceLength)
    {
        totalAlignedSequenceLength += alignedSequenceLength;
    }

    /**
     * @return the total number of mismatches in aligned sequences.
     */
    public int getTotalMismatchCount()
    {
        return totalMismatchCount;
    }

    /**
     * Adds the given number of mismatches to the total.
     *
     * @param mismatchCount the number of mismatches.
     */
    public void addMismatchCount(int mismatchCount)
    {
        totalMismatchCount += mismatchCount;
    }

    /**
     * @return the errorRate
     */
    public float getErrorRate()
    {
        return totalAlignedSequenceLength == 0 ? 0.0f : (float)totalMismatchCount / totalAlignedSequenceLength;
    }

    /**
     * @return the alignedCount
     */
    public int getAlignedCount()
    {
        return alignedCount;
    }

    /**
     * Sets the aligned sequence count.
     *
     * @param alignedCount the number of aligned sequences.
     */
    public void setAlignedCount(int alignedCount)
    {
        this.alignedCount = alignedCount;
    }

    /**
     * @return the assignedCount
     */
    public int getAssignedCount()
    {
        return assignedCount;
    }

    /**
     * Sets the assigned sequence count.
     *
     * @param assignedCount the number of assigned sequences.
     */
    public void setAssignedCount(int assignedCount)
    {
        this.assignedCount = assignedCount;
    }

    /**
     * @return the sequence identifiers associated with alignment to the
     *         reference genome.
     */
    public Set<Integer> getSequenceIds()
    {
        return sequenceIds;
    }

    /**
     * Returns the number of sequences associated with alignment to the
     * reference genome.
     *
     * @return the sequence count.
     */
    public int getSequenceCount()
    {
        return sequenceIds.size();
    }

    /**
     * Adds the given sequence identifier to the set of sequences associated
     * with alignment to the reference genome.
     *
     * @param id the sequence identifier.
     */
    public void addSequenceId(int id)
    {
        sequenceIds.add(id);
    }

    /**
     * Adds the given sequence identifiers to the set of sequences associated
     * with alignment to the reference genome.
     *
     * @param ids the sequence identifiers.
     */
    public void addSequenceIds(Collection<Integer> ids)
    {
        sequenceIds.addAll(ids);
    }

    /**
     * Removes the following sequence identifiers from the set of sequences
     * associated with alignment to the reference genome.
     *
     * @param ids
     */
    public void removeSequenceIds(Collection<Integer> ids)
    {
        sequenceIds.removeAll(ids);
    }
}
