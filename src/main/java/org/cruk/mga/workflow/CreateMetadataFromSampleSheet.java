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
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.ParseException;
import org.cruk.util.CommandLineUtility;
import org.cruk.workflow.assembly.MetaDataLoader;
import org.cruk.workflow.util.ApplicationContextFactory;
import org.cruk.workflow.xml2.metadata.MetaData;
import org.cruk.workflow.xml2.metadata.ModeConfiguration;
import org.cruk.workflow.xml2.metadata.Specialisation;
import org.cruk.workflow.xml2.metadata.SpecialisationSet;
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
    private static final String DEFAULT_LSF_QUEUE = "bioinformatics";

    String sampleSheetFilename;
    String workingDirectory;
    String temporaryDirectory;
    String resourcesDirectory;
    String dataDirectory;
    String outputDirectory;

    private String runId;
    private List<String> datasetIds = new ArrayList<String>();
    private Map<String, String> filenameMappings = new HashMap<String, String>();

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

        workingDirectory = DEFAULT_WORKING_DIR;
        temporaryDirectory = DEFAULT_TEMP_DIR;
        resourcesDirectory = DEFAULT_RESOURCES_DIR;
        dataDirectory = DEFAULT_DATA_DIR;
        outputDirectory = DEFAULT_OUTPUT_DIR;

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

        if (runId == null)
        {
            logger.error("Run ID must be specified");
            System.exit(1);
        }

        meta.setMode("local");
        meta.setTempDirectory(temporaryDirectory);
        meta.setPipeline("${install}/pipelines/mga.xml");

        lsf = new ModeConfiguration("lsf");
        lsf.setVar("lsfOutputDirectory", "${work}/lsfOutput");
        lsf.setVar("queue", DEFAULT_LSF_QUEUE);
        lsf.setVar("maximumSubmittedJobs", "50");
        meta.getModeConfigurations().add(lsf);

        local = new ModeConfiguration("local");
        local.setVar("maxCpuResources", "1");
        meta.getModeConfigurations().add(local);

        meta.setVariable("runId", runId);
        meta.setVariable("sampleSheetFile", sampleSheetFilename);
        meta.setVariable("sampleSize", "100000");
        meta.setVariable("maxNumberOfRecordsToSampleFrom", "5000000");
        meta.setVariable("chunkSize", "5000000");
        meta.setVariable("trimLength", "36");
        meta.setVariable("work", workingDirectory);
        meta.setVariable("dataDir", dataDirectory);
        meta.setVariable("outputDir", outputDirectory);
        meta.setVariable("resourcesDir", resourcesDirectory);
        meta.setVariable("bowtieIndexDir", "${resourcesDir}/bowtie_indexes");
        meta.setVariable("adapterFastaFile", "${resourcesDir}/adapters.fa");
        meta.setVariable("referenceGenomeMappingFile", "${resourcesDir}/reference_genome_mappings.txt");
        meta.setVariable("bowtieExecutable", "bowtie");
        meta.setVariable("exonerateExecutable", "exonerate");
        meta.setVariable("separateDatasetReports", "true");

        meta.getSpecialisationSets().add(new SpecialisationSet());
        SpecialisationSet specialisationSet = meta.getSpecialisationSet(0);
        for (String dataset : datasetIds)
        {
            Specialisation specialisation = new Specialisation(dataset);
            specialisation.setActive(true);
            specialisation.setVariable("fastqFile", "${dataDir}/" + filenameMappings.get(dataset));
            specialisationSet.add(specialisation);
        }
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
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                String[] fields = line.split("\\t");
                if (inDatasetSection)
                {
                    if (fields.length < 2) break;
                    String id = fields[0].trim();
                    String filename = fields[1].trim();
                    if (id.length() == 0 || filename.length() == 0) break;
                    if (!filenameMappings.containsKey(id))
                    {
                        datasetIds.add(id);
                        filenameMappings.put(id, filename);
                    }
                }
                else
                {
                    if (fields.length > 1)
                    {
                        if (fields[0].trim().equals("DatasetId") && fields[1].trim().equals("File"))
                        {
                            inDatasetSection = true;
                        }
                        else if (runId == null && fields[0].equals("Run ID") && fields[1].trim().length() > 0)
                        {
                            runId = fields[1];
                        }
                    }
                }
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
