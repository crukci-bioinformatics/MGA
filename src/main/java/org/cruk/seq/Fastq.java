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

/**
 * Class representing a FASTQ sequence with associated base qualities.
 *
 * @author eldrid01
 */
public class Fastq
{
    private String description;
    private String sequence;
    private String quality;

    public Fastq() {}

    public Fastq(String description, String sequence, String quality)
    {
        this.description = description;
        this.sequence = sequence;
        this.quality = quality;
        if (sequence == null || quality == null) 
            throw new IllegalStateException("Null values for FASTQ sequence and/or quality are not allowed");
        if (sequence.length() != quality.length())
            throw new IllegalStateException("Sequence and quality strings must be the same length");
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }

    public String getSequence()
    {
        return sequence;
    }

    public String getQuality()
    {
        return quality;
    }

    /**
     * Returns the length of the sequence.
     *
     * @return
     */
    public int getLength()
    {
        return sequence.length();
    }

    /**
     * Returns a new Fastq sequence which has been trimmed to the given length.
     * 
     * @param length
     * @return a trimmed Fastq sequence
     */
    public Fastq trim(int length)
    {
        return trim(0, length);
    }

    /**
     * Returns a new Fastq sequence which has been trimmed to the given length
     * from the given start position (zero-based).
     *
     * @param start
     * @param length
     * @return a trimmed Fastq sequence
     */
    public Fastq trim(int start, int length)
    {
        int end = Math.min(start + length, getLength());
        return new Fastq(
                getDescription(),
                getSequence().substring(start, end),
                getQuality().substring(start, end));
    }

    /**
     * Returns the Fastq sequence in FASTQ format.
     */
    public String toString()
    {
        return "@" + getDescription() + "\n"
            + getSequence() + "\n"
            + "+\n"
            + getQuality() + "\n";
    }

    /**
     * Returns the Fastq sequence in FASTA format.
     *
     * @return
     */
    public String toFasta()
    {
        StringBuilder sb = new StringBuilder();
        sb.append(">");
        sb.append(getDescription());
        sb.append("\n");
        String sequence = getSequence();
        int nchars = 70;
        int length = sequence.length();
        for (int i = 0; i < length; i += nchars)
        {
            sb.append(sequence.substring(i, Math.min(length, i + nchars)));
            sb.append("\n");
        }
        return sb.toString();
    }
}
