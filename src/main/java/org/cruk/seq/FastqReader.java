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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.fastq.FastqRecord;

/**
 * Class for reading records from a FASTQ file or from a set of FASTQ files in
 * which case records are read from each file in turn in a round-robin fashion.
 *
 * @author eldrid01
 */
public class FastqReader
{
    private boolean roundRobin = false;
    private List<htsjdk.samtools.fastq.FastqReader> readers = new ArrayList<>();
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
        readers.add(getFastqReader(fastqFile));
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
            readers.add(getFastqReader(new File(fastqFilename)));
        }
    }

    /**
     * Creates a BufferedReader for the given file allowing for decompression
     * if the extension indicates either a gzipped or zippped file.
     *
     * @param file the file
     * @return the HTS JDK FastqReader
     * @throws IOException
     */
    private htsjdk.samtools.fastq.FastqReader getFastqReader(File file) throws IOException
    {
        return new htsjdk.samtools.fastq.FastqReader(file);
    }

    public void close() throws IOException
    {
        for (htsjdk.samtools.fastq.FastqReader reader : readers)
        {
            reader.close();
        }
        readers.clear();
    }

    /**
     * Reads the next FASTQ entry from the current reader and moves the
     * index of the current reader forward if in round-robin mode.
     *
     * @return a FastqRecord object or null if there are no more files containing records.
     * @throws SAMException if the FASTQ record is invalid.
     * @throws IOException
     */
    public FastqRecord readFastq() throws SAMException, IOException
    {
        while (!readers.isEmpty())
        {
            if (roundRobin && currentReaderIndex >= readers.size())
            {
                currentReaderIndex = 0;
            }

            htsjdk.samtools.fastq.FastqReader reader = readers.get(currentReaderIndex);
            try
            {
                FastqRecord fastq = reader.next();
                if (roundRobin)
                {
                    currentReaderIndex++;
                }
                return fastq;
            }
            catch (NoSuchElementException e)
            {
                reader.close();
                readers.remove(currentReaderIndex);
            }
        }
        return null;
    }
}
