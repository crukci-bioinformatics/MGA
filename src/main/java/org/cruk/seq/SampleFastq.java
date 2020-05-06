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

package org.cruk.seq;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.apache.commons.math3.random.RandomDataGenerator;
import org.cruk.util.CommandLineUtility;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * Program for sampling a specified number of records from a FASTQ file.
 *
 * @author eldrid01
 */
public class SampleFastq extends CommandLineUtility
{
    public static int DEFAULT_SAMPLE_SIZE = 100000;
    public static long DEFAULT_MAX_SAMPLE_FROM = 5000000;

    private String datasetId;
    private String[] fastqFilenames;
    private String summaryFilename;
    private String prefix;
    private int sampleSize;
    private long maxSampleFrom;

    /**
     * Runs the SampleFastq utility with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        SampleFastq sampleFastq = new SampleFastq(args);
        sampleFastq.execute();
    }

    /**
     * Initializes a new SampleFastq utility instance with the given command-line arguments.
     *
     * @param args
     */
    private SampleFastq(String[] args)
    {
        super("fastq_file(s)", args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setupOptions()
    {
        options.addOption("i", "dataset-id", true, "Identifier for the sequence dataset.");
        options.addOption("x", "summary-file", true, "Output file containing sampling summary statistics");
        options.addOption("p", "seq-id-prefix", true, "The prefix to use if renaming sequence identifiers");

        Option option = new Option("o", "output-file", true, "Output file for sampled FASTQ sequences");
        option.setRequired(true);
        options.addOption(option);

        option = new Option("s", "sample-size", true, "Number of sequences to sample for alignment (default: " + DEFAULT_SAMPLE_SIZE + ")");
        option.setType(PatternOptionBuilder.NUMBER_VALUE);
        option.setArgName("<int>");
        options.addOption(option);

        option = new Option("m", "max-sample-from", true, "Maximum number of sequences to sample from (default: " + DEFAULT_MAX_SAMPLE_FROM + ")");
        option.setType(PatternOptionBuilder.NUMBER_VALUE);
        option.setArgName("<int>");
        options.addOption(option);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseCommandLine(CommandLine commandLine) throws ParseException
    {
        datasetId = commandLine.getOptionValue("dataset-id");

        Number sampleSizeN = (Number)commandLine.getParsedOptionValue("sample-size");
        sampleSize = sampleSizeN == null ? DEFAULT_SAMPLE_SIZE : sampleSizeN.intValue();

        if (sampleSize < 1)
        {
            error("Error: invalid sample size.");
        }

        Number maxSampleFromN = (Number)commandLine.getParsedOptionValue("max-sample-from");
        maxSampleFrom = maxSampleFromN == null ? DEFAULT_MAX_SAMPLE_FROM : maxSampleFromN.longValue();

        if (maxSampleFrom < sampleSize)
        {
            error("Error: sample size cannot be greater than the number of records from which to sample.");
        }

        outputFilename = commandLine.getOptionValue("output-file");
        if (outputFilename == null)
        {
            error("Error: an output file must be specified.");
        }

        summaryFilename = commandLine.getOptionValue("summary-file");

        prefix = commandLine.getOptionValue("seq-id-prefix");

        String[] args = commandLine.getArgs();

        if (args.length == 0)
        {
            error("Error parsing command line: missing FASTQ filename.", true);
        }

        fastqFilenames = args;
    }

    /**
     * Runs the FASTQ sampling utility.
     *
     * @throws Exception
     */
    protected void run() throws Exception
    {
        try
        {
            Fastq[] records = reservoirSampling(fastqFilenames, sampleSize, maxSampleFrom, prefix != null);

            // Later code requires that the prefix ends with an underscore.
            String safePrefix = prefix;
            if (prefix != null && !prefix.endsWith("_"))
            {
                safePrefix = prefix + "_";
            }

            for (int i = 0; i < records.length; i++)
            {
                Fastq record = records[i];
                if (prefix != null)
                {
                    record.setDescription(safePrefix + (i + 1));
                }
                out.print(record);
            }

            writeSummary(datasetId, records.length);
        }
        catch (FastqFormatException e)
        {
            error(e.getMessage());
        }
    }

    /**
     * Samples records from a FASTQ file using reservoir sampling.
     *
     * @param fastqFilenames the FASTQ file(s).
     * @param sampleSize the number of records to sample.
     * @param maxRecordsToSample the maximum number of records to sample from.
     * @param removeDescriptions to remove sequence identifiers/descriptions to save on space.
     * @return the sampled FASTQ records.
     * @throws IOException
     * @throws FastqFormatException
     */
    private Fastq[] reservoirSampling(String[] fastqFilenames, int sampleSize, long maxSampleFrom, boolean removeDescriptions)
            throws IOException, FastqFormatException
    {
        FastqReader reader = new FastqReader(fastqFilenames, true);

        Fastq[] records = new Fastq[sampleSize];

        for (int i = 0; i < sampleSize; i++)
        {
            Fastq record = reader.readFastq();
            if (record == null) return Arrays.copyOf(records, i);
            if (removeDescriptions) record.setDescription(null);
            records[i] = record;
        }

        RandomDataGenerator rand = new RandomDataGenerator();

        for (long i = sampleSize; i < maxSampleFrom; i++)
        {
            Fastq record = reader.readFastq();
            if (record == null) break;

            long j = rand.nextLong(0l, i);
            if (j < sampleSize) records[(int)j] = record;
        }

        reader.close();

        return records;
    }

    /**
     * Writes an XML file containing a summary of the sampling.
     *
     * @param datasetId the dataset id
     * @param sampledCount the sample size
     */
    private void writeSummary(String datasetId, int sampledCount)
    {
        if (summaryFilename == null) return;

        BufferedOutputStream outputStream = null;
        try
        {
            outputStream = new BufferedOutputStream(new FileOutputStream(summaryFilename));
        }
        catch (IOException e)
        {
            error("Error creating file " + summaryFilename);
        }

        try
        {
            Element root = new Element("SamplingSummary");

            Element element = new Element("DatasetId");
            if (datasetId != null) element.appendChild(datasetId);
            root.appendChild(element);

            element = new Element("SampledCount");
            element.appendChild(Integer.toString(sampledCount));
            root.appendChild(element);

            Document document = new Document(root);

            Serializer serializer;
            serializer = new Serializer(outputStream, "ISO-8859-1");
            serializer.setIndent(2);
            serializer.setMaxLength(64);
            serializer.setLineSeparator("\n");
            serializer.write(document);

            outputStream.flush();
        }
        catch (UnsupportedEncodingException e)
        {
            error(e);
        }
        catch (IOException e)
        {
            error("Error writing summary XML file");
        }
        finally
        {
            try
            {
                outputStream.close();
            }
            catch (IOException e)
            {
                error("Error closing file " + summaryFilename);
            }
        }
    }
}
