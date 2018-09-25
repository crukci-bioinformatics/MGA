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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;

/**
 * Class used to represent the summary of an alignment of a sequence
 * dataset against a specific reference genome.
 *
 * @author eldrid01
 */
public class AlignmentSummary
{
    private String referenceGenomeId;
    private int alignedCount = 0;
    private int totalAlignedSequenceLength = 0;
    private int totalMismatchCount = 0;
    private int uniquelyAlignedCount = 0;
    private int totalUniquelyAlignedSequenceLength = 0;
    private int totalUniquelyAlignedMismatchCount = 0;
    private int preferentiallyAlignedCount = 0;
    private int totalPreferentiallyAlignedSequenceLength = 0;
    private int totalPreferentiallyAlignedMismatchCount = 0;
    private int assignedCount = 0;
    private int totalAssignedSequenceLength = 0;
    private int totalAssignedMismatchCount = 0;

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
     * @return the alignedCount the number of aligned sequences.
     */
    public int getAlignedCount()
    {
        return alignedCount;
    }

    /**
     * Increments the aligned sequence count.
     */
    public void incrementAlignedCount()
    {
        alignedCount++;
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
     * @return the error rate
     */
    public float getErrorRate()
    {
        return totalAlignedSequenceLength == 0 ? 0.0f : (float)totalMismatchCount / totalAlignedSequenceLength;
    }

    /**
     * @return the uniquely aligned sequence count.
     */
    public int getUniquelyAlignedCount()
    {
        return uniquelyAlignedCount;
    }

    /**
     * Increments the number of uniquely aligned sequences.
     */
    public void incrementUniquelyAlignedCount()
    {
        uniquelyAlignedCount++;
    }

    /**
     * @return the total uniquely aligned sequence length.
     */
    public int getTotalUniquelyAlignedSequenceLength()
    {
        return totalUniquelyAlignedSequenceLength;
    }

    /**
     * Adds the given uniquely aligned sequence length to the total.
     *
     * @param alignedLength the aligned sequence length.
     */
    public void addUniquelyAlignedSequenceLength(int alignedLength)
    {
        totalUniquelyAlignedSequenceLength += alignedLength;
    }

    /**
     * @return the total number of mismatches in uniquely aligned sequences.
     */
    public int getTotalUniquelyAlignedMismatchCount()
    {
        return totalUniquelyAlignedMismatchCount;
    }

    /**
     * Adds the given number of mismatches to the total for uniquely aligned sequences.
     *
     * @param mismatchCount the number of mismatches.
     */
    public void addUniquelyAlignedMismatchCount(int mismatchCount)
    {
        totalUniquelyAlignedMismatchCount += mismatchCount;
    }

    /**
     * @return the error rate for uniquely aligned sequences.
     */
    public float getUniquelyAlignedErrorRate()
    {
        return totalUniquelyAlignedSequenceLength == 0 ? 0.0f : (float)totalUniquelyAlignedMismatchCount / totalUniquelyAlignedSequenceLength;
    }


    /**
     * @return the preferentially aligned sequence count.
     */
    public int getPreferentiallyAlignedCount()
    {
        return preferentiallyAlignedCount;
    }

    /**
     * Increments the number of preferentially aligned sequences.
     */
    public void incrementPreferentiallyAlignedCount()
    {
        preferentiallyAlignedCount++;
    }

    /**
     * @return the total preferentially aligned sequence length.
     */
    public int getTotalPreferentiallyAlignedSequenceLength()
    {
        return totalPreferentiallyAlignedSequenceLength;
    }

    /**
     * Adds the given preferentially aligned sequence length to the total.
     *
     * @param alignedLength the aligned sequence length.
     */
    public void addPreferentiallyAlignedSequenceLength(int alignedLength)
    {
        totalPreferentiallyAlignedSequenceLength += alignedLength;
    }

    /**
     * @return the total number of mismatches in preferentially aligned sequences.
     */
    public int getTotalPreferentiallyAlignedMismatchCount()
    {
        return totalPreferentiallyAlignedMismatchCount;
    }

    /**
     * Adds the given number of mismatches to the total for preferentially aligned sequences.
     *
     * @param mismatchCount the number of mismatches.
     */
    public void addPreferentiallyAlignedMismatchCount(int mismatchCount)
    {
        totalPreferentiallyAlignedMismatchCount += mismatchCount;
    }

    /**
     * @return the error rate for preferentially aligned sequences.
     */
    public float getPreferentiallyAlignedErrorRate()
    {
        return totalPreferentiallyAlignedSequenceLength == 0 ? 0.0f : (float)totalPreferentiallyAlignedMismatchCount / totalPreferentiallyAlignedSequenceLength;
    }

    /**
     * @return the assigned sequence count.
     */
    public int getAssignedCount()
    {
        return assignedCount;
    }

    /**
     * Increments the number of assigned sequences.
     */
    public void incrementAssignedCount()
    {
        assignedCount++;
    }

    /**
     * @return the total assigned sequence length.
     */
    public int getTotalAssignedSequenceLength()
    {
        return totalAssignedSequenceLength;
    }

    /**
     * Adds the given assigned sequence length to the total.
     *
     * @param alignedLength the aligned sequence length.
     */
    public void addAssignedSequenceLength(int alignedLength)
    {
        totalAssignedSequenceLength += alignedLength;
    }

    /**
     * @return the total number of mismatches in assigned sequences.
     */
    public int getTotalAssignedMismatchCount()
    {
        return totalAssignedMismatchCount;
    }

    /**
     * Adds the given number of mismatches to the total for assigned sequences.
     *
     * @param mismatchCount the number of mismatches.
     */
    public void addAssignedMismatchCount(int mismatchCount)
    {
        totalAssignedMismatchCount += mismatchCount;
    }

    /**
     * @return the error rate for assigned sequences.
     */
    public float getAssignedErrorRate()
    {
        return totalAssignedSequenceLength == 0 ? 0.0f : (float)totalAssignedMismatchCount / totalAssignedSequenceLength;
    }

    @Override
    public String toString()
    {
        ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("referenceGenomeId", referenceGenomeId);
        sb.append("alignedCount", alignedCount);
        return sb.toString();
    }
}
