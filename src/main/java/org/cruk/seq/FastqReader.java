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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Class for reading records from a FASTQ file or from a set of FASTQ files in
 * which case records are read from each file in turn in a round-robin fashion.
 *
 * @author eldrid01
 */
public class FastqReader
{
    private static Log log = LogFactory.getLog(FastqReader.class);

    private boolean roundRobin = false;
    private List<BufferedReader> readers = new ArrayList<BufferedReader>();
    private int currentReaderIndex = 0;

    /**
     * Creates a new instance of FastqReader for reading from the given FASTQ file.
     *
     * @param fastqFilename the name of the FASTQ file.
     * @throws IOException
     */
    public FastqReader(String fastqFilename) throws IOException
    {
        this (new File(fastqFilename));
    }

    /**
     * Creates a new instance of FastqReader for reading from the given FASTQ file.
     *
     * @param fastqFile the FASTQ file.
     * @throws IOException
     */
    public FastqReader(File fastqFile) throws IOException
    {
        readers.add(getBufferedReader(fastqFile));
    }

    /**
     * Creates a new instance of FastqReader for reading from the given files,
     * optionally in a round-robin fashion.
     *
     * @param fastqFilenames the names of the FASTQ files.
     * @param roundRobin whether to read records from each file in turn or all records from one file before proceeding to the next.
     * @throws IOException
     */
    public FastqReader(String[] fastqFilenames, boolean roundRobin) throws IOException
    {
        this.roundRobin = roundRobin;
        for (String fastqFilename : fastqFilenames)
        {
            readers.add(getBufferedReader(new File(fastqFilename)));
        }
    }

    /**
     * Returns a new input stream for the given file allowing for decompression
     * if the extension indicates either a gzipped or zippped file.
     *
     * @param file the file
     * @return the input stream
     * @throws IOException
     */
    private InputStream getInputStream(File file) throws IOException
    {
        String filename = file.getName();
        InputStream inputStream = new FileInputStream(file);
        if (filename.endsWith(".gz"))
        {
            inputStream = new GZIPInputStream(inputStream);
        }
        else if (filename.toLowerCase().endsWith(".zip"))
        {
            // assumes single entry in the zip archive
            inputStream = new ZipInputStream(inputStream);
            ((ZipInputStream)inputStream).getNextEntry();
        }
        return inputStream;
    }

    /**
     * Creates a BufferedReader for the given file allowing for decompression
     * if the extension indicates either a gzipped or zippped file.
     *
     * @param file the file
     * @return the buffered reader
     * @throws IOException
     */
    private BufferedReader getBufferedReader(File file) throws IOException
    {
        InputStream inputStream = getInputStream(file);
        return new BufferedReader(new InputStreamReader(inputStream));
    }

    public void close() throws IOException
    {
        for (Reader reader : readers)
        {
            reader.close();
        }
        readers.clear();
    }

    /**
     * Reads the next FASTQ entry from the current reader and moves the
     * index of the current reader forward if in round-robin mode.
     *
     * @return a Fastq object or null if there are no more files containing records.
     * @throws FastqFormatException
     * @throws IOException
     */
    public Fastq readFastq() throws FastqFormatException, IOException
    {
        while (!readers.isEmpty())
        {
            if (roundRobin && currentReaderIndex >= readers.size()) currentReaderIndex = 0;
            BufferedReader reader = readers.get(currentReaderIndex);
            Fastq fastq = readFastq(reader);
            if (fastq == null)
            {
                reader.close();
                readers.remove(currentReaderIndex);
            }
            else
            {
                if (roundRobin) currentReaderIndex++;
                return fastq;
            }
        }
        return null;
    }

    /**
     * Reads the next FASTQ entry from the given reader.
     *
     * @param reader the FASTQ file reader.
     * @return a Fastq object or null if there are no more files containing records.
     * @throws FastqFormatException
     * @throws IOException
     */
    private Fastq readFastq(BufferedReader reader) throws FastqFormatException, IOException
    {
        String[] lines = new String[4];
        for (int i = 0; i < 4; i++)
        {
            lines[i] = reader.readLine();
        }
        return createFastq(lines);
    }

    /**
     * Creates a Fastq object from the given lines validating as appropriate.
     *
     * @param lines
     * @return
     * @throws FastqFormatException
     */
    private Fastq createFastq(String[] lines) throws FastqFormatException
    {
        if (lines.length != 4)
        {
            String message = "Invalid number of lines for FASTQ entry";
            log.error(message);
            throw new RuntimeException(message);
        }

        String descriptionLine = lines[0];
        String sequenceLine = lines[1];
        String separatorLine = lines[2];
        String qualityLine = lines[3];

        if (descriptionLine == null) return null;
        if (!descriptionLine.startsWith("@"))
        {
            String message = "Invalid FASTQ entry: description line must begin with a @ character";
            log.error(message);
            throw new FastqFormatException(message);
        }
        String description = descriptionLine.substring(1).trim();

        if (sequenceLine == null)
        {
            String message = "Invalid FASTQ entry: truncated after description line";
            log.error(message);
            throw new FastqFormatException(message);
        }
        String sequence = sequenceLine.trim();
        if (sequence.length() == 0)
        {
            String message = "Invalid FASTQ entry: zero-length sequence for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }
        if (!sequence.matches("^[A-Za-z\\.~]+$"))
        {
            String message = "Invalid FASTQ entry: sequence string contains invalid characters for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }

        if (separatorLine == null)
        {
            String message = "Invalid FASTQ entry: truncated after sequence line";
            log.error(message);
            throw new FastqFormatException(message);
        }
        if (!separatorLine.startsWith("+"))
        {
            String message = "Invalid FASTQ entry: separator line must begin with a + character for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }
        String separator = separatorLine.substring(1).trim();
        if (separator.length() > 0 && !separator.equals(description))
        {
            String message = "Invalid FASTQ entry: separator does not match descriptor for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }

        if (qualityLine == null)
        {
            String message = "Invalid FASTQ entry: truncated after separator line";
            log.error(message);
            throw new FastqFormatException(message);
        }
        String quality = qualityLine.trim();
        if (quality.length() == 0)
        {
            String message = "Invalid FASTQ entry: zero-length quality for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }
        if (quality.length() != sequence.length())
        {
            String message = "Invalid FASTQ entry: sequence and quality strings of differing length for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }
        if (!quality.matches("^[!-~]+$"))
        {
            String message = "Invalid FASTQ entry: quality string contains invalid characters for entry " + description;
            log.error(message);
            throw new FastqFormatException(message);
        }

        return new Fastq(description, sequence, quality);
    }
}
