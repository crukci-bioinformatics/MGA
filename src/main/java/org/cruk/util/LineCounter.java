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

package org.cruk.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipInputStream;

/**
 * Utility class for count lines in a file.
 *
 * @author eldrid01
 */
public class LineCounter
{
    /**
     * Returns the number of lines in the specified file.
     *
     * @param filename the name of the file
     * @return
     * @throws IOException
     */
    public long getLineCount(String filename) throws IOException
    {

        long lineCount = 0;
        try
        {
            lineCount = getLineCountUnix(filename); 
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        if (lineCount == 0)
        {
            InputStream inputStream = new FileInputStream(filename);
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
            LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream));
//            while (reader.readLine() != null) {}
            // slightly more efficient than reading the file line by line using readLine
            reader.skip(Long.MAX_VALUE);
            lineCount = reader.getLineNumber();
            reader.close();
        }

        return lineCount;
    }

    /**
     * Using Unix commands, principally wc -l, to obtain the number of lines
     * in a file.
     *
     * Can handle gzipped compressed files.
     *
     * @param filename the file name
     * @return the line count
     * @throws Exception
     */
    private long getLineCountUnix(String filename) throws Exception
    {
        String[] command = new String[] { "wc", "-l", filename };
        if (filename.endsWith(".gz"))
        {
            command = new String[] { "sh",  "-c", "gunzip -c " + filename + " | wc -l" };
        }
        else if (filename.toLowerCase().endsWith(".zip"))
        {
            command = new String[] { "sh",  "-c", "unzip -p " + filename + " | wc -l" };
        }
        Process process = new ProcessBuilder(command).start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        long lineCount = Long.parseLong(reader.readLine().trim().split("\\s")[0]);
        reader.close();
        return lineCount;
    }

    /**
     * Estimate the number of lines in the given file based on the length of
     * the file in bytes and the length of the first 1000 lines.
     *
     * N.B. this doesn't work for compressed files.
     *
     * @param filename the name of the file.
     * @return an estimate of the number of lines.
     */
    public long estimateLineCount(String filename) throws IOException
    {
        int n = 1000;
        int count = 0;
        File file = new File(filename);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        try
        {
            for (int i = 0; i < n; i++)
            {
                String line = reader.readLine();
                if (line == null) return (long)i;
                count += line.length() + 1;
            }
            return (long)(1000 * (file.length() / (double)count));
        }
        finally
        {
            reader.close();
        }
    }
}
