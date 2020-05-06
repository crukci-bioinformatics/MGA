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

import java.io.FileWriter;
import java.io.PrintWriter;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.cruk.util.CommandLineUtility;

/**
 * Utility for combining FASTQ files and splitting into chunks with up to a
 * specified number of records.
 *
 * @author eldrid01
 */
public class MergeAndSplitFastq extends CommandLineUtility
{
    public static String DEFAULT_OUTPUT_PREFIX = "sequences";
    public static long DEFAULT_RECORDS_PER_FILE = 5000000;

    private String[] fastqFilenames;
    private String outputFilePrefix;
    private long recordsPerFile;

    /**
     * Runs the MergeAndSplitFastq utility with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        MergeAndSplitFastq mergeAndSplitFastq = new MergeAndSplitFastq(args);
        mergeAndSplitFastq.execute();
    }

    /**
     * Initializes a new MergeAndSplitFastq utility instance with the given command-line arguments.
     *
     * @param args
     */
    private MergeAndSplitFastq(String[] args)
    {
        super("fastq_files", args);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void setupOptions()
    {
        options.addOption("p", "output-prefix", true, "The prefix to use for merged/split FASTQ output files (default: " + DEFAULT_OUTPUT_PREFIX + ")");

        Option option = new Option("n", "records-per-file", true, "The maximum number of records per FASTQ output file (default: " + DEFAULT_RECORDS_PER_FILE + ")");
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
        outputFilePrefix = commandLine.getOptionValue("output-prefix", DEFAULT_OUTPUT_PREFIX);
        outputFilePrefix = outputFilePrefix.replaceAll("\\.$", "");

        Number recordsPerFileN = (Number)commandLine.getParsedOptionValue("records-per-file");
        recordsPerFile = recordsPerFileN == null ? DEFAULT_RECORDS_PER_FILE : recordsPerFileN.longValue();

        fastqFilenames = commandLine.getArgs();
    }

    /**
     * Merges and splits FASTQ files into chunks of the specified size.
     *
     * @throws Exception
     */
    protected void run() throws Exception
    {
        try
        {
            int outputFileCount = 1;
            PrintWriter writer = new PrintWriter(new FileWriter(this.outputFilePrefix + "." + outputFileCount + ".fq"));
            int recordCount = 0;

            for (String fastqFilename : fastqFilenames)
            {
                FastqReader reader = new FastqReader(fastqFilename);

                Fastq fastq;
                while ((fastq = reader.readFastq()) != null)
                {
                    if (writer == null)
                    {
                        outputFileCount++;
                        writer = new PrintWriter(new FileWriter(this.outputFilePrefix + "." + outputFileCount + ".fq"));
                    }
                    writer.print(fastq);
                    recordCount++;
                    if (recordCount == recordsPerFile)
                    {
                        writer.close();
                        writer = null;
                        recordCount = 0;
                    }
                }

                reader.close();
            }

            if (writer != null) writer.close();
        }
        catch (FastqFormatException e)
        {
            error(e.getMessage());
        }
    }
}
