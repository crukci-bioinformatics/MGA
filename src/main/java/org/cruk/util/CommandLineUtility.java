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

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for command line utility programs
 *
 * @author eldrid01
 */
public abstract class CommandLineUtility
{
    protected Logger log = LoggerFactory.getLogger(getClass());

    protected String argumentsDisplayString;
    protected Options options = new Options();
    protected String outputFilename;
    protected PrintStream out;

    /**
     * Initializes a new CommandLineUtility instance with the given command line
     * arguments.
     *
     * @param args
     */
    public CommandLineUtility(String[] args)
    {
        this(null, args);
    }

    /**
     * Initializes a new CommandLineUtility instance with the given arguments
     * display string and command line arguments.
     *
     * @param argumentsDisplayString
     * @param args
     */
    public CommandLineUtility(String argumentsDisplayString, String[] args)
    {
        this.argumentsDisplayString = argumentsDisplayString;
        setupOptions();
        parseCommandLineArguments(args);
    }

    /**
     * Populate the options object held by this utility.
     */
    protected abstract void setupOptions();

    /**
     * Parses command line arguments. Subclasses may implement this abstract
     * method to parse the given arguments and populate the command line options
     * object, but this default implementation would be typical to parse the
     * options and pass to {@link #parseCommandLine(CommandLine)} to process
     * the parsed values. Errors are reported and the utility terminated if there
     * are any.
     *
     * @param args The raw command line arguments given to the JVM.
     */
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new DefaultParser();

        try
        {
            CommandLine commandLine = parser.parse(options, args);
            parseCommandLine(commandLine);
        }
        catch (ParseException e)
        {
            error("Error parsing command-line options: " + e.getMessage(), true);
        }
    }

    /**
     * Process the parsed command line arguments.  Subclasses should implement this abstract
     * method to process the parsed options.
     *
     * @param commandLine The parsed command line.
     *
     * @throws ParseException if there is a problem with any argument.
     */
    protected abstract void parseCommandLine(CommandLine commandLine) throws ParseException;

    /**
     * Abstract method for running the utility.
     *
     * @throws Exception
     */
    protected abstract void run() throws Exception;

    /**
     * Runs the utility taking care of opening and closing the output writer.
     */
    public void execute()
    {
        if (outputFilename == null)
        {
            out = System.out;
        }
        else
        {
            try
            {
                out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFilename)));
            }
            catch (IOException e)
            {
                error("Error creating file " + outputFilename);
            }
        }

        try
        {
            run();
            out.flush();
        }
        catch (Exception e)
        {
            e.printStackTrace();
            error("Error: " + e.getClass().getName() + " " + e.getMessage());
        }
        finally
        {
            closeOutputStream();
        }
    }

    /**
     * Closes the file output stream.
     */
    private void closeOutputStream()
    {
        if (outputFilename != null && out != null)
        {
            out.close();
        }
    }

    /**
     * Prints the error message for the given exception and exits.
     *
     * @param e the exception
     */
    protected void error(Exception e)
    {
        error("Error: " + e.getMessage());
    }

    /**
     * Prints the given error message and exits.
     *
     * @param error the error message
     */
    protected void error(String error)
    {
        error(error, false);
    }

    /**
     * Prints the given error message, optionally prints usage details, and exits.
     *
     * @param error the error message
     * @param printUsage
     */
    protected void error(String error, boolean printUsage)
    {
        System.err.println(error);
        if (printUsage)
        {
            usage();
        }
        closeOutputStream();
        System.exit(1);
    }

    /**
     * Prints usage details.
     */
    protected void usage()
    {
        HelpFormatter formatter = new HelpFormatter();
        String commandLine = "java " + getClass().getName();
        if (argumentsDisplayString != null)
        {
            commandLine = commandLine + " " + argumentsDisplayString;
        }
        formatter.printHelp(commandLine, options);
    }
}
