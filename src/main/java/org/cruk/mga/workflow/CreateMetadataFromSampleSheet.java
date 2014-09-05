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

package org.cruk.mga.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.cruk.util.CommandLineUtility;
import org.cruk.workflow.assembly.MetaDataLoader;
import org.cruk.workflow.util.ApplicationContextFactory;
import org.cruk.workflow.util.FileFinder;
import org.cruk.workflow.xml2.metadata.MetaData;
import org.cruk.workflow.xml2.metadata.ModeConfiguration;
import org.cruk.workflow.xml2.pipeline.FilenamePattern;
import org.cruk.workflow.xml2.pipeline.TaskVariableSet;
import org.springframework.context.ApplicationContext;

/**
 * Creates the pipeline configuration metadata from a sample sheet.
 *
 * @author eldrid01
 */
public class CreateMetadataFromSampleSheet extends CommandLineUtility
{
    private static final String DEFAULT_WORKING_DIR = "@{user.dir}";
    private static final String DEFAULT_TEMP_DIR = "${work}/temp";
    private static final String DEFAULT_RESOURCES_DIR = "${install}/resources";
    private static final String DEFAULT_DATA_DIR = "${work}";
    private static final String DEFAULT_OUTPUT_DIR = "${work}";
    private static final String DEFAULT_BOWTIE_EXECUTABLE = "bowtie";
    private static final String DEFAULT_EXONERATE_EXECUTABLE = "exonerate";
    private static final boolean DEFAULT_SEPARATE_DATASET_REPORTS = false;
    private static final String DEFAULT_LSF_QUEUE = "bioinformatics";

    private FileFinder fileFinder = new FileFinder();

    private String sampleSheetFilename;
    private String workingDirectory;
    private String temporaryDirectory;
    private String resourcesDirectory;
    private String dataDirectory;
    private String outputDirectory;
    private String bowtieExecutable;
    private String exonerateExecutable;
    private boolean separateDatasetReports;

    private String runId;

    private MetaData meta;
    private ModeConfiguration lsf;
    private ModeConfiguration local;

    public static void main(String[] args)
    {
        CreateMetadataFromSampleSheet createMetadataFromSampleSheet = new CreateMetadataFromSampleSheet(args);
        createMetadataFromSampleSheet.execute();
    }

    CreateMetadataFromSampleSheet(String[] args)
    {
        super("sample_sheet_file", args);
        meta = new MetaData();
    }

    /**
     * Parse command line arguments.
     *
     * @param args
     */
    @Override
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new GnuParser();

        options.addOption("o", "output-metadata-file", true, "Output pipeline metadata file (default: stdout)");
        options.addOption("w", "working-directory", true, "The working directory (default: current directory, referred to as ${work})");
        options.addOption("t", "temp-directory", true, "The temporary directory (default: " + DEFAULT_TEMP_DIR + ")");
        options.addOption("r", "resources-directory", true, "The MGA resources directory containing the bowtie-indexes subdirectory, the adaptor sequences and the reference genome mapping (default: " + DEFAULT_RESOURCES_DIR + ")");
        options.addOption(null, "data-directory", true, "The data directory containing the FASTQ sequence files (default: " + DEFAULT_DATA_DIR + ")");
        options.addOption(null, "output-directory", true, "The output directory in which the report is created (default: " + DEFAULT_OUTPUT_DIR + ")");
        options.addOption("b", "bowtie-executable", true, "The path for the bowtie executable (default: " + DEFAULT_BOWTIE_EXECUTABLE + ")");
        options.addOption("e", "exonerate-executable", true, "The path for the exonerate executable (default: " + DEFAULT_EXONERATE_EXECUTABLE + ")");
        options.addOption(null, "separate-dataset-reports", false, "If separate reports for each dataset are required");

        workingDirectory = DEFAULT_WORKING_DIR;
        temporaryDirectory = DEFAULT_TEMP_DIR;
        resourcesDirectory = DEFAULT_RESOURCES_DIR;
        dataDirectory = DEFAULT_DATA_DIR;
        outputDirectory = DEFAULT_OUTPUT_DIR;
        bowtieExecutable = DEFAULT_BOWTIE_EXECUTABLE;
        exonerateExecutable = DEFAULT_EXONERATE_EXECUTABLE;
        separateDatasetReports = DEFAULT_SEPARATE_DATASET_REPORTS;

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("output-metadata-file"))
            {
                outputFilename = commandLine.getOptionValue("output-metadata-file");
            }

            if (commandLine.hasOption("working-directory"))
            {
                workingDirectory = commandLine.getOptionValue("working-directory");
            }

            if (commandLine.hasOption("temp-directory"))
            {
                temporaryDirectory = commandLine.getOptionValue("temp-directory");
            }

            if (commandLine.hasOption("resources-directory"))
            {
                resourcesDirectory = commandLine.getOptionValue("resources-directory");
            }

            if (commandLine.hasOption("data-directory"))
            {
                dataDirectory = commandLine.getOptionValue("data-directory");
            }

            if (commandLine.hasOption("output-directory"))
            {
                outputDirectory = commandLine.getOptionValue("output-directory");
            }

            if (commandLine.hasOption("bowtie-executable"))
            {
            	bowtieExecutable = commandLine.getOptionValue("bowtie-executable");
            }

            if (commandLine.hasOption("exonerate-executable"))
            {
            	exonerateExecutable = commandLine.getOptionValue("exonerate-executable");
            }

            if (commandLine.hasOption("separate-dataset-reports"))
            {
            	separateDatasetReports = commandLine.hasOption("separate-dataset-reports");
            }

            args = commandLine.getArgs();

            if (args.length < 1)
            {
                error("Error parsing command line: missing arguments", true);
            }

            sampleSheetFilename = args[0];
        }
        catch (ParseException e)
        {
            error("Error parsing command-line options: " + e.getMessage(), true);
        }
    }

    protected void populateMeta()
    {
        readSampleSheet();

        meta.setMode("local");
        meta.setTempDirectory(temporaryDirectory);
        meta.setPipeline("${install}/pipelines/mga.xml");

        local = new ModeConfiguration("local");
        local.setVar("maxCpuResources", "1");
        local.setVar("outputDirectory", "${work}/localOutput");
        meta.getModeConfigurations().add(local);

        lsf = new ModeConfiguration("lsf");
        lsf.setVar("outputDirectory", "${work}/lsfOutput");
        lsf.setVar("queue", DEFAULT_LSF_QUEUE);
        lsf.setVar("maximumSubmittedJobs", "50");
        meta.getModeConfigurations().add(lsf);

        meta.setVariable("runId", runId);
        meta.setVariable("sampleSheetFile", sampleSheetFilename);
        meta.setVariable("work", workingDirectory);
        meta.setVariable("dataDir", dataDirectory);
        meta.setVariable("outputDir", outputDirectory);
        meta.setVariable("resourcesDir", resourcesDirectory);
        meta.setVariable("bowtieIndexDir", "${resourcesDir}/bowtie_indexes");
        meta.setVariable("adapterFastaFile", "${resourcesDir}/adapters.fa");
        meta.setVariable("referenceGenomeMappingFile", "${resourcesDir}/reference_genome_mappings.txt");
        meta.setVariable("bowtieExecutable", bowtieExecutable);
        meta.setVariable("exonerateExecutable", exonerateExecutable);
        meta.setVariable("sampleSize", "100000");
        meta.setVariable("maxNumberOfRecordsToSampleFrom", "5000000");
        meta.setVariable("chunkSize", "5000000");
        meta.setVariable("trimLength", "36");
        meta.setVariable("separateDatasetReports", Boolean.toString(separateDatasetReports));
        meta.setVariable("plotWidth", "800");
        meta.setVariable("minimumSequenceCount", "10");
    }

    /**
     * Retrieves run details from the LIMS and creates a sample sheet.
     *
     * @throws Exception
     */
    @Override
    protected void run() throws Exception
    {
        ApplicationContext appContext = ApplicationContextFactory.createStartupApplicationContext();

        populateMeta();

        MetaDataLoader metaLoader = appContext.getBean(MetaDataLoader.class);

        metaLoader.writeMetaData(meta, out);
    }

    /**
     * Returns a mapping of dataset IDs to file names.
     */
    private void readSampleSheet()
    {
        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(sampleSheetFilename));
        }
        catch (FileNotFoundException e)
        {
            error("Error: could not find file " + sampleSheetFilename);
        }

        try
        {
        	boolean inDatasetSection = false;

            int lineNumber = 0;
        	String line = null;

        	while ((line = reader.readLine()) != null)
            {
                lineNumber++;
                String[] fields = line.split("\\t");
                if (inDatasetSection)
                {
                    if (fields.length < 2)
                    {
                    	error("Dataset section in sample sheet contains line with less than two columns at line " + lineNumber + " of sample sheet file " + sampleSheetFilename);
                    }

                    String id = fields[0].trim();
                    if (id.length() == 0)
                    {
                    	error("Missing dataset identifier in first column at line " + lineNumber + " of sample sheet file " + sampleSheetFilename);
                    }

                    String filename = fields[1].trim();
                    if (filename.length() != 0)
                    {
                    	if (!dataDirectory.equals(DEFAULT_DATA_DIR))
                    	{
                    		filename = filename.replaceAll("\\$\\{dataDir\\}", dataDirectory);
                    	}
                    	String[] filenames = filename.split("\\|");
                    	for (int i = 0; i < filenames.length; i++)
                    	{
                    		File file = new File(filenames[i]);
                    		List<File> files = fileFinder.findFiles(file.getAbsolutePath(), FilenamePattern.WILDCARD, TaskVariableSet.INPUT);
                    		if (files.isEmpty())
                    		{
                    			logger.warn("Could not find any files matching pattern " + fields[1].trim() + " at line " + lineNumber + " of sample sheet file " + sampleSheetFilename);
                    		}
                    	}
                    }
                }
                else
                {
                    if (fields.length > 1)
                    {
                        if (fields[0].trim().equalsIgnoreCase("DatasetId") && fields[1].trim().equalsIgnoreCase("File"))
                        {
                            inDatasetSection = true;
                        }
                        else if (runId == null && fields[0].equalsIgnoreCase("Run ID") && fields[1].trim().length() > 0)
                        {
                            runId = fields[1];
                        }
                    }
                }
            }

        	if (runId == null)
        	{
        		error("Run ID must be specified in header section in sample sheet file " + sampleSheetFilename);
        	}

        	if (!inDatasetSection)
        	{
        		error("Missing dataset section in sample sheet file " + sampleSheetFilename);
        	}
        }
        catch (IOException e)
        {
            error(e);
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
                error("Error closing file " + sampleSheetFilename);
            }
        }
    }

    public MetaData getMeta() {
        return meta;
    }

    public ModeConfiguration getLsf() {
        return lsf;
    }
}
