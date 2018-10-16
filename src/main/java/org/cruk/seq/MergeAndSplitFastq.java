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
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.cruk.util.CommandLineUtility;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.fastq.FastqRecord;

/**
 * Utility for combining FASTQ files and splitting into chunks with up to a
 * specified number of records.
 *
 * @author eldrid01
 */
public class MergeAndSplitFastq extends CommandLineUtility
{
    public static String DEFAULT_OUTPUT_PREFIX = "sequences";
    public static int DEFAULT_RECORDS_PER_FILE = 5000000;

    private String[] fastqFilenames;
    private String outputFilePrefix;
    private int recordsPerFile;

    /**
     * Runs the MergeAndSplitFastq utility with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        System.setProperty("line.separator", "\n");

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
     * Parse command line arguments.
     *
     * @param args
     */
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new DefaultParser();

        options.addOption("p", "output-prefix", true, "The prefix to use for merged/split FASTQ output files (default: " + outputFilePrefix + ")");
        options.addOption("n", "records-per-file", true, "The maximum number of records per FASTQ output file (default: " + recordsPerFile + ")");

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("output-prefix"))
            {
                outputFilePrefix = commandLine.getOptionValue("output-prefix");
                outputFilePrefix = outputFilePrefix.replaceAll("\\.$", "");
            }
            else
            {
                outputFilePrefix = DEFAULT_OUTPUT_PREFIX;
            }

            if (commandLine.hasOption("records-per-file"))
            {
                try
                {
                    recordsPerFile = Integer.parseInt(commandLine.getOptionValue("records-per-file"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: the number of records per file must be an integer number");
                }
            }
            else
            {
                recordsPerFile = DEFAULT_RECORDS_PER_FILE;
            }

            fastqFilenames = commandLine.getArgs();
        }
        catch (ParseException e)
        {
            error("Error parsing command-line options: " + e.getMessage(), true);
        }
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

                FastqRecord fastq;
                while ((fastq = reader.readFastq()) != null)
                {
                    if (writer == null)
                    {
                        outputFileCount++;
                        writer = new PrintWriter(new FileWriter(this.outputFilePrefix + "." + outputFileCount + ".fq"));
                    }
                    writer.println(fastq.toFastQString());
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
        catch (SAMException e)
        {
            error(e.getMessage());
        }
    }
}
