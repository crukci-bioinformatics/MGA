package org.cruk.mga;

import java.io.File;
import java.io.IOException;

import org.apache.commons.lang3.ClassUtils;

import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

public class SAMAlignmentReader extends AbstractAlignmentReader
{
    private SAMRecordIterator[] readers;
    private int[] lineNumbers;

    public SAMAlignmentReader(String[] alignmentFiles, String runId) throws IOException
    {
        super(alignmentFiles, runId);

        SamReaderFactory factory = SamReaderFactory.makeDefault();
        factory.validationStringency(ValidationStringency.SILENT);

        int n = alignmentFiles.length;

        readers = new SAMRecordIterator[n];
        lineNumbers = new int[n];

        for (int i = 0; i < n; i++)
        {
            File file = new File(alignmentFiles[i]);

            try
            {
                readers[i] = factory.open(file).iterator();
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
            catch (Exception e)
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
        return lineNumbers[index];
    }

    @Override
    protected Alignment readAlignment(int index) throws IOException
    {
        SAMRecordIterator reader = readers[index];

        if (reader == null)
        {
            return null;
        }
        if (!reader.hasNext())
        {
            readerFinished(index);
            return null;
        }

        SAMRecord next = reader.next();
        ++lineNumbers[index];

        int separatorIndex = next.getReadName().lastIndexOf("_");
        if (separatorIndex == -1)
        {
            throw new RuntimeException("Incorrect sequence identifier (" + next.getReadName() + ") at line " + lineNumbers[index] + " in file " + alignmentFiles[index]);
        }

        String datasetId = next.getReadName().substring(0, separatorIndex);
        int sequenceId = -1;
        try
        {
            sequenceId = Integer.parseInt(next.getReadName().substring(separatorIndex + 1));
        }
        catch (NumberFormatException e)
        {
            throw new RuntimeException("Incorrect sequence identifier (" + next.getReadName() + ") at line " + lineNumbers[index] + " in file " + alignmentFiles[index]);
        }

        int alignedLength = next.getAlignmentEnd() - next.getAlignmentStart();

        int mismatchCount = next.getCigarLength();

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
