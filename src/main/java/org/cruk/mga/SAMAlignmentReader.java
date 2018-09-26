package org.cruk.mga;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMTagUtil;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

public class SAMAlignmentReader extends AbstractAlignmentReader
{
    private SAMRecordIterator[] readers;
    private int[] recordNumbers;

    public SAMAlignmentReader(String aligner, String[] allAlignmentFiles, String runId) throws IOException
    {
        super(aligner, allAlignmentFiles, runId);

        SamReaderFactory factory = SamReaderFactory.makeDefault();
        factory.validationStringency(ValidationStringency.SILENT);

        int n = alignmentFiles.size();

        readers = new SAMRecordIterator[n];
        recordNumbers = new int[n];

        for (int i = 0; i < n; i++)
        {
            File file = new File(alignmentFiles.get(i));

            try
            {
                readers[i] = factory.open(file).iterator();
                recordNumbers[i] = 0;

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
            catch (IOException e)
            {
                System.err.println("Could not open " + file.getName());
                System.err.println(ClassUtils.getShortClassName(e.getClass()) + ": " + e.getMessage());
            }
        }
    }

    @Override
    protected void finalize() throws Throwable
    {
        if (readers != null)
        {
            for (SAMRecordIterator r : readers)
            {
                try
                {
                    r.close();
                }
                catch (Exception e)
                {
                    // Ignore.
                }
            }
        }
    }

    @Override
    protected int getLineNumber(int index)
    {
        return recordNumbers[index];
    }

    @Override
    protected Alignment readAlignment(int index) throws IOException
    {
        SAMRecordIterator reader = readers[index];

        if (reader == null)
        {
            return null;
        }

        while (reader.hasNext())
        {
            SAMRecord next = reader.next();
            ++recordNumbers[index];

            if (next.getNotPrimaryAlignmentFlag() && !next.getReadUnmappedFlag())
            {
                continue;
            }

            int separatorIndex = next.getReadName().lastIndexOf("_");
            if (separatorIndex == -1)
            {
                throw new RuntimeException("Incorrect sequence identifier (" + next.getReadName() + ") for record " + recordNumbers[index] + " in file " + alignmentFiles.get(index));
            }

            String datasetId = next.getReadName().substring(0, separatorIndex);
            int sequenceId = -1;
            try
            {
                sequenceId = Integer.parseInt(next.getReadName().substring(separatorIndex + 1));
            }
            catch (NumberFormatException e)
            {
                throw new RuntimeException("Incorrect sequence identifier (" + next.getReadName() + ") for record " + recordNumbers[index] + " in file " + alignmentFiles.get(index));
            }

            int alignedLength = next.getReadLength();

            int mismatchCount = 255;
            Object nm = next.getAttribute(SAMTagUtil.getSingleton().NM);
            if (nm != null)
            {
                mismatchCount = ((Number)nm).intValue();
            }

            return new Alignment(datasetId, sequenceId, referenceGenomeIds[index], alignedLength, mismatchCount);
        }

        readerFinished(index);
        return null;
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
            catch (Exception e)
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
