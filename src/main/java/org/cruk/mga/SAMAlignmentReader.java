package org.cruk.mga;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

import org.apache.commons.lang3.ClassUtils;

import htsjdk.samtools.SAMFlag;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SAMRecordIterator;
import htsjdk.samtools.SAMTagUtil;
import htsjdk.samtools.SamReaderFactory;
import htsjdk.samtools.ValidationStringency;

public class SAMAlignmentReader extends AbstractAlignmentReader
{
    private static final String INCORRECT_ID_MESSAGE = "Incorrect sequence identifier ({0}) for record {1} in file {2}";

    private static final int FLAG_MASK = SAMFlag.NOT_PRIMARY_ALIGNMENT.intValue() | SAMFlag.SUPPLEMENTARY_ALIGNMENT.intValue();

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
    public void close()
    {
        if (readers != null)
        {
            for (SAMRecordIterator r : readers)
            {
                try
                {
                    if (r != null)
                    {
                        r.close();
                    }
                }
                catch (Exception e)
                {
                    // Ignore.
                }
            }

            readers = null;
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

            // Skip reads that are secondary or supplementary reads.

            if ((next.getFlags() & FLAG_MASK) == 0)
            {
                int separatorIndex = next.getReadName().lastIndexOf("_");
                if (separatorIndex == -1)
                {
                    throw new RuntimeException(MessageFormat.format(INCORRECT_ID_MESSAGE, next.getReadName(), recordNumbers[index], alignmentFiles.get(index)));
                }

                String datasetId = next.getReadName().substring(0, separatorIndex);
                int sequenceId = -1;
                try
                {
                    sequenceId = Integer.parseInt(next.getReadName().substring(separatorIndex + 1));
                }
                catch (NumberFormatException e)
                {
                    throw new RuntimeException(MessageFormat.format(INCORRECT_ID_MESSAGE, next.getReadName(), recordNumbers[index], alignmentFiles.get(index)));
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
