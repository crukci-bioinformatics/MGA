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

package org.cruk.seq;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.cruk.util.CommandLineUtility;

/**
 * Utility for converting a FASTQ file to FASTA format.
 *
 * @author eldrid01
 */
public class FastqToFasta extends CommandLineUtility
{
    private String fastqFilename;

    /**
     * Runs the FastqToFasta utility with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        FastqToFasta fastqToFasta = new FastqToFasta(args);
        fastqToFasta.execute();
    }

    /**
     * Initializes a new FastqToFasta utility instance with the given command-line arguments.
     *
     * @param args
     */
    private FastqToFasta(String[] args)
    {
        super("fastq_filename", args);
    }

    /**
     * Parse command line arguments.
     *
     * @param args
     */
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new GnuParser();

        options.addOption("o", "output-file", true, "Output file for FASTA sequences (default: stdout)");

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("output-file"))
            {
                outputFilename = commandLine.getOptionValue("output-file");
            }

            args = commandLine.getArgs();

            if (args.length == 0)
            {
                error("Error parsing command line: missing FASTQ filename", true);
            }
            if (args.length > 1)
            {
                error("Error parsing command line: additional arguments and/or unrecognized options");
            }

            fastqFilename = args[0];
        }
        catch (ParseException e)
        {
            error("Error parsing command-line options: " + e.getMessage(), true);
        }
    }

    /**
     * Runs the FASTQ to FASTA conversion utility.
     *
     * @throws Exception 
     */
    protected void run() throws Exception
    {
        try
        {
            FastqReader reader = new FastqReader(fastqFilename);
            Fastq fastq;
            while ((fastq = reader.readFastq()) != null)
            {
                out.print(fastq.toFasta());
            }
            reader.close();
        }
        catch (FastqFormatException e)
        {
            error(e.getMessage());
        }
    }
}
