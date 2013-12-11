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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class FastqReader
{
    protected Log logger = LogFactory.getLog(FastqReader.class);
    private BufferedReader reader;

    public FastqReader(String fastqFilename) throws IOException
    {
        this (new File(fastqFilename));
    }

    public FastqReader(File fastqFile) throws IOException
    {
        reader = getBufferedReader(fastqFile);
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
            ZipInputStream zipInputStream = new ZipInputStream(inputStream);
            // assumes single entry in the zip archive
            zipInputStream.getNextEntry();
            inputStream = zipInputStream;
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
        reader.close();
        reader = null;
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
            logger.error(message);
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
            logger.error(message);
            throw new FastqFormatException(message);
        }
        String description = descriptionLine.substring(1).trim();

        if (sequenceLine == null)
        {
            String message = "Invalid FASTQ entry: truncated after description line";
            logger.error(message);
            throw new FastqFormatException(message);
        }
        String sequence = sequenceLine.trim();
        if (sequence.length() == 0)
        {
            String message = "Invalid FASTQ entry: zero-length sequence for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }
        if (!sequence.matches("^[A-Za-z\\.~]+$"))
        {
            String message = "Invalid FASTQ entry: sequence string contains invalid characters for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }

        if (separatorLine == null)
        {
            String message = "Invalid FASTQ entry: truncated after sequence line";
            logger.error(message);
            throw new FastqFormatException(message);
        }
        if (!separatorLine.startsWith("+"))
        {
            String message = "Invalid FASTQ entry: separator line must begin with a + character for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }
        String separator = separatorLine.substring(1).trim();
        if (separator.length() > 0 && !separator.equals(description))
        {
            String message = "Invalid FASTQ entry: separator does not match descriptor for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }

        if (qualityLine == null)
        {
            String message = "Invalid FASTQ entry: truncated after separator line";
            logger.error(message);
            throw new FastqFormatException(message);
        }
        String quality = qualityLine.trim();
        if (quality.length() == 0)
        {
            String message = "Invalid FASTQ entry: zero-length quality for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }
        if (quality.length() != sequence.length())
        {
            String message = "Invalid FASTQ entry: sequence and quality strings of differing length for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }
        if (!quality.matches("^[!-~]+$"))
        {
            String message = "Invalid FASTQ entry: quality string contains invalid characters for entry " + description;
            logger.error(message);
            throw new FastqFormatException(message);
        }

        return new Fastq(description, sequence, quality);
    }

    /**
     * Reads the next FASTQ entry from the given reader.
     *
     * @return a Fastq object or null if at the end of the file.
     * @throws FastqFormatException
     * @throws IOException
     */
    public Fastq readFastq() throws FastqFormatException, IOException
    {
        if (reader == null)
        {
            String message = "Error reading from FASTQ file; possible cause is that the file has been closed.";
            logger.error(message);
            throw new FastqFormatException(message);

        }

        String[] lines = new String[4];
        for (int i = 0; i < 4; i++)
        {
            lines[i] = reader.readLine();
        }

        return createFastq(lines);
    }

    /**
     * Reads next FASTQ entry from the given reader.
     *
     * This method is used in sampling and does not assume that the current
     * position within the file reader is at the beginning of a line. The
     * first line read which could be part of a line is discarded, so it is
     * possible that the next FASTQ entry returned is the one after the one
     * starting at the current position.
     *
     * @return a Fastq object or null if at the end of the file.
     * @throws FastqFormatException
     * @throws IOException
     */
    public Fastq readNextFastq() throws FastqFormatException, IOException
    {
        if (reader == null)
        {
            String message = "Error reading from FASTQ file; possible cause is that the file has been closed.";
            logger.error(message);
            throw new FastqFormatException(message);

        }

        // ensure at beginning of current line or move to start of next line
        reader.readLine();

        // read subsequent lines until one that starts with @
        String line = null;
        for (int i = 0; i < 4; i++)
        {
            line = reader.readLine();
            if (line == null) return null;
            if (line.startsWith("@")) break;
        }
        if (!line.startsWith("@"))
        {
            String message = "Invalid FASTQ format: header not found (assumes entries are 4 lines long, i.e. no wrapping)";
            logger.error(message);
            throw new FastqFormatException(message);
        }

        String[] lines = new String[4];
        lines[0] = line;
        int linesRead = 1;

        // check for quality string starting with @ by reading next line
        // assumes sequences cannot contain @ character
        line = reader.readLine();
        if (line == null) return null;
        if (line.startsWith("@"))
        {
            // two consecutive lines starting with a @
            // must be quality followed by header
            linesRead = 0;
        }

        lines[linesRead] = line;
        for (int i = linesRead + 1; i < 4; i++)
        {
            lines[i] = reader.readLine();
            if (lines[i] == null) return null;
        }

        return createFastq(lines);
    }
}
