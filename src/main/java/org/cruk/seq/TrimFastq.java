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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.cruk.util.CommandLineUtility;

import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

/**
 * Utility for trimming entries in a FASTQ file to a given trimLength.
 *
 * @author eldrid01
 */
public class TrimFastq extends CommandLineUtility
{
    public static int DEFAULT_START = 1;
    public static int DEFAULT_LENGTH = 36;

    private String fastqFilename;
    private String summaryFilename;
    private int trimStart;
    private int trimLength;

    /**
     * Runs the TrimFastq utility with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        TrimFastq trimFastq = new TrimFastq(args);
        trimFastq.execute();
    }

    /**
     * Initializes a new TrimFastq utility instance with the given command-line arguments.
     *
     * @param args
     */
    private TrimFastq(String[] args)
    {
        super("fastq_file", args);
    }

    /**
     * Parse command line arguments.
     *
     * @param args
     */
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new DefaultParser();

        options.addOption("s", "trim-start", true, "Start position for trimmed sequences (default: " + DEFAULT_START + ")");
        options.addOption("l", "trim-length", true, "Length of trimmed sequences (default: " + DEFAULT_LENGTH + ")");
        options.addOption("o", "output-file", true, "Output file for trimmed FASTQ sequences (default: stdout)");
        options.addOption("x", "summary-file", true, "Output file containing trimming summary statistics");

        trimStart = DEFAULT_START;
        trimLength = DEFAULT_LENGTH;

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("trim-start"))
            {
                try
                {
                    trimStart = Integer.parseInt(commandLine.getOptionValue("trim-start"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: trim-start must be an integer number.");
                }
            }

            if (commandLine.hasOption("trim-length"))
            {
                try
                {
                    trimLength = Integer.parseInt(commandLine.getOptionValue("trim-length"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: trim length must be an integer number");
                }
            }

            if (commandLine.hasOption("output-file"))
            {
                outputFilename = commandLine.getOptionValue("output-file");
            }

            if (commandLine.hasOption("summary-file"))
            {
                summaryFilename = commandLine.getOptionValue("summary-file");
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
     * Runs the FASTQ trimming utility.
     *
     * @throws Exception
     */
    protected void run() throws Exception
    {
        trimStart--;
        if (trimStart < 0)
            error("Invalid start position for trimming");

        if (trimLength < 1)
            error("Invalid trim length");

        int trimEnd = trimStart + trimLength;

        try
        {
            FastqReader reader = new FastqReader(fastqFilename);

            int minLength = 0;
            int maxLength = 0;

            Fastq fastq;
            while ((fastq = reader.readFastq()) != null)
            {
                int length = fastq.getLength();

                if (trimEnd > length)
                    error("Sequence too short for trimming (" + fastq.getDescription() + ", length " + length + ")");

                if (minLength == 0)
                {
                    minLength = length;
                }
                else
                {
                    minLength = Math.min(minLength, length);
                }

                maxLength = Math.max(maxLength, length);

                fastq = fastq.trim(trimStart, trimLength);

                out.print(fastq.toString());
            }

            reader.close();

            writeSummary(trimLength, minLength, maxLength);
        }
        catch (FastqFormatException e)
        {
            error(e.getMessage());
        }
    }

    /**
     * Writes an XML file containing a summary of the trimming.
     *
     * @param trimLength the length to which sequences were trimmed.
     * @param minLength the smallest sequence length
     * @param maxLength the largest sequence length
     */
    private void writeSummary(int trimLength, int minLength, int maxLength)
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
            Element root = new Element("TrimmingSummary");

            Element element = new Element("TrimLength");
            element.appendChild(Integer.toString(trimLength));
            root.appendChild(element);

            element = new Element("MinimumSequenceLength");
            element.appendChild(Integer.toString(minLength));
            root.appendChild(element);

            element = new Element("MaximumSequenceLength");
            element.appendChild(Integer.toString(maxLength));
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
