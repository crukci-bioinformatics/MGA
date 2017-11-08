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

package org.cruk.mga.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.cruk.util.CommandLineUtility;
import org.cruk.workflow.assembly.MetaDataLoader;
import org.cruk.workflow.util.ApplicationContextFactory;
import org.cruk.workflow.util.FileFinder;
import org.cruk.workflow.xml2.metadata.LogLevel;
import org.cruk.workflow.xml2.metadata.MetaData;
import org.cruk.workflow.xml2.metadata.ModeSpecific;
import org.cruk.workflow.xml2.metadata.VersionsFileFormat;
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
    public static final String DEFAULT_RUN_ID = "MGA";

    public static final String DEFAULT_MODE = "local";
    public static final int DEFAULT_MAX_CPU_RESOURCES = 1;
    public static final String DEFAULT_QUEUE = "bioinformatics";
    public static final int DEFAULT_MAX_SUBMITTED_JOBS = 50;

    public static final String DEFAULT_WORKING_DIR = "@{user.dir}";
    public static final String DEFAULT_TEMP_DIR = "${work}/temp";
    public static final String DEFAULT_RESOURCES_DIR = "${install}/resources";
    public static final String DEFAULT_DATA_DIR = "${work}";
    public static final String DEFAULT_OUTPUT_DIR = "${work}";
    
    public static final String DEFAULT_BOWTIE_EXECUTABLE = "bowtie";
    public static final String DEFAULT_EXONERATE_EXECUTABLE = "exonerate";

    public static final int DEFAULT_SAMPLE_SIZE = 100000;
    public static final long DEFAULT_MAX_RECORDS_TO_SAMPLE_FROM = 5000000;
    public static final int DEFAULT_CHUNK_SIZE = 5000000;
    public static final int DEFAULT_TRIM_START = 1;
    public static final int DEFAULT_TRIM_LENGTH = 36;

    public static final int DEFAULT_PLOT_WIDTH = 800;
    public static final int DEFAULT_MIN_SEQUENCE_COUNT = 10;
    public static final boolean DEFAULT_SEPARATE_DATASET_REPORTS = false;

    private FileFinder fileFinder = new FileFinder();

    private String runId;
    private String mode;
    private int maxCpuResources;
    private String queue;
    private int maxSubmittedJobs;
    private String sampleSheetFilename;
    private String workingDirectory;
    private String temporaryDirectory;
    private String resourcesDirectory;
    private String dataDirectory;
    private String outputDirectory;
    private String bowtieExecutable;
    private String exonerateExecutable;
    private int sampleSize;
    private long maxNumberOfRecordsToSampleFrom;
    private int chunkSize;
    private int trimStart;
    private int trimLength;
    private int plotWidth;
    private int minimumSequenceCount;
    private boolean separateDatasetReports;

    private MetaData meta;

    public static void main(String[] args)
    {
        CreateMetadataFromSampleSheet createMetadataFromSampleSheet = new CreateMetadataFromSampleSheet(args);
        createMetadataFromSampleSheet.execute();
    }

    public CreateMetadataFromSampleSheet(String[] args)
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
        CommandLineParser parser = new DefaultParser();

        options.addOption(null, "run-id", true, "The run identifier used for naming jobs and as a file prefix (default: " + DEFAULT_RUN_ID + ")");
        options.addOption("o", "output-metadata-file", true, "Output pipeline metadata (configuration) file (default: stdout)");
        options.addOption("m", "mode", true, "The run mode, either local, slurm or lsf (default: " + DEFAULT_MODE + ")");
        options.addOption("n", "max-cpu-resources", true, "Maximum number of CPU processors to use when running in local mode (default: " + DEFAULT_MAX_CPU_RESOURCES + ")");
        options.addOption("q", "queue", true, "The queue to submit to when using the Slurm or LSF scheduler (default: " + DEFAULT_QUEUE + ")");
        options.addOption("j", "max-submitted-jobs", true, "Maximum number of submitted jobs when using the Slurm or LSF scheduler (default: " + DEFAULT_MAX_SUBMITTED_JOBS + ")");
        options.addOption("w", "working-directory", true, "The working directory (default: current directory, referred to as ${work})");
        options.addOption("t", "temp-directory", true, "The temporary directory (default: " + DEFAULT_TEMP_DIR + ")");
        options.addOption("r", "resources-directory", true, "The MGA resources directory containing the bowtie-indexes subdirectory, the adaptor sequences and the reference genome mapping (default: " + DEFAULT_RESOURCES_DIR + ", where ${install} refers to the MGA installation directory)");
        options.addOption("d", "data-directory", true, "The data directory containing the FASTQ sequence files (default: " + DEFAULT_DATA_DIR + ")");
        options.addOption(null, "output-directory", true, "The output directory in which the report is created (default: " + DEFAULT_OUTPUT_DIR + ")");
        options.addOption("b", "bowtie-executable", true, "The path for the bowtie executable (default: " + DEFAULT_BOWTIE_EXECUTABLE + ")");
        options.addOption("e", "exonerate-executable", true, "The path for the exonerate executable (default: " + DEFAULT_EXONERATE_EXECUTABLE + ")");
        options.addOption("s", "sample-size", true, "The number of FASTQ records to sample for each dataset (default: " + DEFAULT_SAMPLE_SIZE + ")");
        options.addOption(null, "max-records-to-sample-from", true, "The maximum number of FASTQ records to read (sample from) for each dataset (default: " + DEFAULT_MAX_RECORDS_TO_SAMPLE_FROM + ")");
        options.addOption("c", "chunk-size", true, "The maximum number of FASTQ records in each chunk/alignment job (default: " + DEFAULT_CHUNK_SIZE + ")");
        options.addOption(null, "trim-start", true, "The position within sequences from which to start trimming for alignment; any bases before this position will be trimmed (default: " + DEFAULT_TRIM_START + ")");
        options.addOption(null, "trim-length", true, "The length to trim sequences to for alignment (default: " + DEFAULT_TRIM_LENGTH + ")");
        options.addOption(null, "plot-width", true, "The width of the stacked bar plot in pixels (default: " + DEFAULT_PLOT_WIDTH + ")");
        options.addOption(null, "min-sequence-count", true, "The minimum sequence count to use on the y-axis when creating the stacked bar plot (default: " + DEFAULT_MIN_SEQUENCE_COUNT + ")");
        options.addOption(null, "separate-dataset-reports", false, "If separate reports for each dataset are required");

        mode = DEFAULT_MODE;
        maxCpuResources = DEFAULT_MAX_CPU_RESOURCES;
        queue = DEFAULT_QUEUE;
        maxSubmittedJobs = DEFAULT_MAX_SUBMITTED_JOBS;
        workingDirectory = DEFAULT_WORKING_DIR;
        temporaryDirectory = DEFAULT_TEMP_DIR;
        resourcesDirectory = DEFAULT_RESOURCES_DIR;
        dataDirectory = DEFAULT_DATA_DIR;
        outputDirectory = DEFAULT_OUTPUT_DIR;
        bowtieExecutable = DEFAULT_BOWTIE_EXECUTABLE;
        exonerateExecutable = DEFAULT_EXONERATE_EXECUTABLE;
        sampleSize = DEFAULT_SAMPLE_SIZE;
        maxNumberOfRecordsToSampleFrom = DEFAULT_MAX_RECORDS_TO_SAMPLE_FROM;
        chunkSize = DEFAULT_CHUNK_SIZE;
        trimStart = DEFAULT_TRIM_START;
        trimLength = DEFAULT_TRIM_LENGTH;
        plotWidth = DEFAULT_PLOT_WIDTH;
        minimumSequenceCount = DEFAULT_MIN_SEQUENCE_COUNT;
        separateDatasetReports = DEFAULT_SEPARATE_DATASET_REPORTS;

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("run-id"))
            {
                runId = commandLine.getOptionValue("run-id").trim();
            }

            if (commandLine.hasOption("output-metadata-file"))
            {
                outputFilename = commandLine.getOptionValue("output-metadata-file");
            }

            if (commandLine.hasOption("mode"))
            {
                mode = commandLine.getOptionValue("mode").toLowerCase();
                if (!mode.equals("local") && !mode.equals("slurm") && !mode.equals("lsf"))
                {
                    error("Error: unrecognized execution mode: " + commandLine.getOptionValue("mode") + " (can be either local, slurm or lsf)");
                }
            }

            if (commandLine.hasOption("max-cpu-resources"))
            {
                try
                {
                    maxCpuResources = Integer.parseInt(commandLine.getOptionValue("max-cpu-resources"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: max-cpu-resources must be an integer number.");
                }
            }

            if (commandLine.hasOption("queue"))
            {
                queue = commandLine.getOptionValue("queue");
            }

            if (commandLine.hasOption("max-submitted-jobs"))
            {
                try
                {
                    maxSubmittedJobs = Integer.parseInt(commandLine.getOptionValue("max-submitted-jobs"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: max-submitted-jobs must be an integer number.");
                }
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

            if (commandLine.hasOption("sample-size"))
            {
                try
                {
                    sampleSize = Integer.parseInt(commandLine.getOptionValue("sample-size"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: sample-size must be an integer number.");
                }
            }

            if (commandLine.hasOption("max-records-to-sample-from"))
            {
                try
                {
                    maxNumberOfRecordsToSampleFrom = Long.parseLong(commandLine.getOptionValue("max-records-to-sample-from"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: max-records-to-sample-from must be an integer number.");
                }
            }

            if (commandLine.hasOption("chunk-size"))
            {
                try
                {
                    chunkSize = Integer.parseInt(commandLine.getOptionValue("chunk-size"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: chunk-size must be an integer number.");
                }
            }

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
                    error("Error parsing command line option: trim-length must be an integer number.");
                }
            }

            if (commandLine.hasOption("plot-width"))
            {
                try
                {
                    plotWidth = Integer.parseInt(commandLine.getOptionValue("plot-width"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: plot-width must be an integer number.");
                }
            }

            if (commandLine.hasOption("min-sequence-count"))
            {
                try
                {
                    minimumSequenceCount = Integer.parseInt(commandLine.getOptionValue("min-sequence-count"));
                }
                catch (NumberFormatException e)
                {
                    error("Error parsing command line option: min-sequence-count must be an integer number.");
                }
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

        if (runId == null || runId.trim().length() == 0)
            runId = DEFAULT_RUN_ID;
        else
            runId = runId.trim().replaceAll("\\s+", "_");

        meta.setPipeline("${install}/pipelines/mga.xml");
        meta.setMode(mode);
        meta.setTempDirectory(temporaryDirectory);
        meta.setJobOutputDirectory("${work}/logs");
        meta.setSummaryFile("${work}/logs/${runId}.summary.csv", false, false);
        meta.setVersionsFile("${work}/logs/${runId}.versions.txt", VersionsFileFormat.TEXT);
        meta.setLogFile("${work}/logs/${runId}.pipeline.log", true, LogLevel.NORMAL);

        ModeSpecific local = new ModeSpecific("local");
        local.setVariable("maxCpuResources", Integer.toString(maxCpuResources));
        meta.getModeConfigurations().add(local);

        ModeSpecific lsf = new ModeSpecific("lsf");
        lsf.setVariable("queue", queue);
        lsf.setVariable("maximumSubmittedJobs", Integer.toString(maxSubmittedJobs));
        meta.getModeConfigurations().add(lsf);

        ModeSpecific slurm = new ModeSpecific("slurm");
        slurm.setVariable("queue", queue);
        slurm.setVariable("maximumSubmittedJobs", Integer.toString(maxSubmittedJobs));
        meta.getModeConfigurations().add(slurm);

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
        meta.setVariable("sampleSize", Integer.toString(sampleSize));
        meta.setVariable("maxNumberOfRecordsToSampleFrom", Long.toString(maxNumberOfRecordsToSampleFrom));
        meta.setVariable("chunkSize", Integer.toString(chunkSize));
        meta.setVariable("trimStart", Integer.toString(trimStart));
        meta.setVariable("trimLength", Integer.toString(trimLength));
        meta.setVariable("plotWidth", Integer.toString(plotWidth));
        meta.setVariable("minimumSequenceCount", Integer.toString(minimumSequenceCount));
        meta.setVariable("separateDatasetReports", Boolean.toString(separateDatasetReports));
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
                        error("Error: dataset section in sample sheet contains line with less than two columns at line " + lineNumber + " of sample sheet file " + sampleSheetFilename);
                    }

                    String id = fields[0].trim();
                    if (id.length() == 0)
                    {
                        error("Error: missing dataset identifier in first column at line " + lineNumber + " of sample sheet file " + sampleSheetFilename);
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
                                log.warn("Could not find any files matching pattern " + fields[1].trim() + " at line " + lineNumber + " of sample sheet file " + sampleSheetFilename);
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
                        else if ((runId == null || runId.trim().isEmpty()) && fields[0].equalsIgnoreCase("Run ID") && fields[1].trim().length() > 0)
                        {
                            runId = fields[1].trim();
                        }
                    }
                }
            }

            if (!inDatasetSection)
            {
                error("Error: missing dataset section in sample sheet file " + sampleSheetFilename);
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

    public String getWorkingDirectory()
    {
        return workingDirectory;
    }

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    public String getResourcesDirectory()
    {
        return resourcesDirectory;
    }

    public void setResourcesDirectory(String resourcesDirectory)
    {
        this.resourcesDirectory = resourcesDirectory;
    }
}
