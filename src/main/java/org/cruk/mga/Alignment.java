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

import java.io.Serializable;

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import org.cruk.common.comparators.numericname.NumericAwareComparator;

public class Alignment implements Comparable<Alignment>, Serializable
{
    private static final long serialVersionUID = -8470534783944707744L;

    private String datasetId;
    private int sequenceId;
    private String referenceGenomeId;
    private int alignedLength;
    private int mismatchCount;

    public Alignment(String datasetId, int sequenceId, String referenceGenomeId, int alignedLength, int mismatchCount)
    {
        this.datasetId = datasetId;
        this.sequenceId = sequenceId;
        this.referenceGenomeId = referenceGenomeId;
        this.alignedLength = alignedLength;
        this.mismatchCount = mismatchCount;
    }

    public String getDatasetId()
    {
        return datasetId;
    }

    public int getSequenceId()
    {
        return sequenceId;
    }

    public String getReferenceGenomeId()
    {
        return referenceGenomeId;
    }

    public int getAlignedLength()
    {
        return alignedLength;
    }

    public int getMismatchCount()
    {
        return mismatchCount;
    }

    @Override
    public int compareTo(Alignment other)
    {
        int cmp = NumericAwareComparator.instance().compare(datasetId, other.datasetId);
        if (cmp == 0)
        {
            cmp = sequenceId - other.sequenceId;

            if (cmp == 0)
            {
                cmp = mismatchCount - other.mismatchCount;

                if (cmp == 0)
                {
                    cmp = referenceGenomeId.compareTo(other.referenceGenomeId);
                }
            }
        }
        return cmp;
    }

    @Override
    public String toString()
    {
        ToStringBuilder sb = new ToStringBuilder(this, ToStringStyle.SHORT_PREFIX_STYLE);
        sb.append("datasetId", datasetId);
        sb.append("sequenceId", sequenceId);
        sb.append("referenceGenomeId", referenceGenomeId);
        sb.append("alignedLength", alignedLength);
        sb.append("mismatchCount", mismatchCount);
        return sb.toString();
    }
}
