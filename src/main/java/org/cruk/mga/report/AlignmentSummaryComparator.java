package org.cruk.mga.report;

import java.util.Comparator;

import org.cruk.mga.AlignmentSummary;

/**
 * Comparator for alignment summaries based on assigned counts.
 */
public class AlignmentSummaryComparator implements Comparator<AlignmentSummary>
{
    public int compare(AlignmentSummary alignmentSummary0, AlignmentSummary alignmentSummary1)
    {
        return alignmentSummary1.getAssignedCount() - alignmentSummary0.getAssignedCount();
    }
}
