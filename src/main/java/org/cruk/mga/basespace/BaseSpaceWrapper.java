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

package org.cruk.mga.basespace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.cruk.mga.workflow.CreateMetadataFromSampleSheet;
import org.cruk.util.CommandLineUtility;
import org.cruk.workflow.PipelineRunner;
import org.cruk.workflow.commandline.CommandLineOptions;
import org.cruk.workflow.util.ApplicationContextFactory;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.context.ApplicationContext;

public class BaseSpaceWrapper extends CommandLineUtility
{
    private static final File INPUT_DATA_DIR = new File("/data/input/samples");
    private static final String BASE_CALLS_SUBDIRECTORY = "Data/Intensities/BaseCalls";
    private static final File APP_RESULTS_DIR = new File("/data/output/appresults");

    private static final String DEFAULT_WORKING_DIR = "/home/apps/mga/work";

    private static final FilenameFilter FASTQ_FILENAME_FILTER = new FilenameFilter()
    {
        @Override
        public boolean accept(File dir, String name)
        {
            return name.endsWith(".fastq.gz");
        }
    };

    // TODO replace with actual controlled vocabulary term
    private static final String DEFAULT_CONTROL_SPECIES = "PhiX";

    private String projectId;
    private String resourcesDirectory;
    private String workingDirectory;
    private String controlSpecies;
    private String appSessionFilename;
    private File resultsDirectory;

    private List<String> sampleIds = new ArrayList<String>();
    private Map<String, SampleInfo> sampleInfoLookup = new HashMap<String, SampleInfo>();

    /**
     * Runs the BaseSpace wrapper with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        BaseSpaceWrapper basespaceWrapper = new BaseSpaceWrapper(args);
        basespaceWrapper.execute();
    }

    /**
     * Initializes a new BaseSpaceWrapper instance with the given command-line arguments.
     *
     * @param args
     */
    private BaseSpaceWrapper(String[] args)
    {
        super("AppSession_JSON_file", args);
    }

    @Override
    protected void setupOptions()
    {
        Option option = new Option("p", "project-id", true, "Project identifier.");
        option.setRequired(true);
        options.addOption(option);
        options.addOption("r", "resources-directory", true, "The MGA resources directory containing the bowtie-indexes subdirectory, the adaptor sequences and the reference genome mapping (default: " + CreateMetadataFromSampleSheet.DEFAULT_RESOURCES_DIR + ", where ${install} refers to the MGA installation directory)");
        options.addOption("w", "work-directory", true, "The working directory in which MGA will be run (default: " + DEFAULT_WORKING_DIR + ")");
        options.addOption("c", "control-species", true, "The control species (default: " + DEFAULT_CONTROL_SPECIES  + ")");
    }

    @Override
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new DefaultParser();

        resourcesDirectory = CreateMetadataFromSampleSheet.DEFAULT_RESOURCES_DIR;
        workingDirectory = DEFAULT_WORKING_DIR;
        controlSpecies = DEFAULT_CONTROL_SPECIES;

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            projectId = commandLine.getOptionValue("project-id");

            if (commandLine.hasOption("resources-directory"))
            {
                resourcesDirectory = commandLine.getOptionValue("resources-directory");
            }

            if (commandLine.hasOption("work-directory"))
            {
                workingDirectory = commandLine.getOptionValue("work-directory");
            }

            if (commandLine.hasOption("control-species"))
            {
                controlSpecies = commandLine.getOptionValue("control-species");
            }

            File projectOutputDirectory = new File(APP_RESULTS_DIR, projectId);
            projectOutputDirectory.mkdir();

            resultsDirectory = new File(projectOutputDirectory, "results");
            resultsDirectory.mkdir();

            outputFilename = new File(resultsDirectory, "results.txt").getAbsolutePath();

            args = commandLine.getArgs();

            if (args.length == 0)
            {
                error("Error parsing command line: missing AppSession JSON file name.", true);
            }
            if (args.length > 1)
            {
                error("Error parsing command line: additional arguments and/or unrecognized options.");
            }

            appSessionFilename = args[0];
        }
        catch (ParseException e)
        {
            error("Error parsing command-line options: " + e.getMessage(), true);
        }
    }

    @Override
    protected void run() throws Exception
    {
        // TODO how should errors be handled in BaseSpace apps

        readSampleInfo();
        File sampleSheetFile = createSampleSheet();
        File metadataFile = createPipelineMetadataFile(sampleSheetFile);

        // TODO create count.xml files for each sample

        ApplicationContext applicationContext = ApplicationContextFactory.createApplicationContext("local");
        CommandLineOptions options = applicationContext.getBean("commandLineOptions", CommandLineOptions.class);
        options.parseCommandLine(new String[0]);
        PipelineRunner pipelineRunner = applicationContext.getBean("pipelineRunner", PipelineRunner.class);
        pipelineRunner.runPipeline(metadataFile, "local");

        FileUtils.copyFile(new File(workingDirectory, "results.html"), new File(resultsDirectory, "results.html"));
        FileUtils.copyFile(new File(workingDirectory, "results.png"), new File(resultsDirectory, "results.png"));
    }

    /**
     * Reads sample information from the app session file.
     *
     * @throws IOException
     */
    private void readSampleInfo() throws IOException
    {
        StringBuffer sb = new StringBuffer();
        BufferedReader reader = new BufferedReader(new FileReader(appSessionFilename));
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            sb.append(line);
        }
        reader.close();

        JSONObject appSession = new JSONObject(sb.toString());

        JSONObject properties = appSession.getJSONObject("Properties");
        JSONArray items = properties.getJSONArray("Items");

        for (int i = 0; i < items.length(); i++)
         {
             Object object = items.get(i);

             if (object instanceof JSONObject)
             {
                 JSONObject item = (JSONObject)object;

                 if (item.getString("Name").equals("Input.Samples"))
                 {
                     JSONArray samples = item.getJSONArray("Items");

                     for (int j = 0; j < samples.length(); j++)
                     {
                         JSONObject sample = samples.getJSONObject(j);

                         String id = sample.getString("Id");
                         String name = sample.getString("SampleId");
                         boolean isPairedEnd = sample.getBoolean("IsPairedEnd");
                         int read1 = sample.getInt("Read1");
                         int read2 = isPairedEnd ? sample.getInt("Read2") : 0;
                         int numReadsPF = sample.getInt("NumReadsPF");
                         int numReadsRaw = sample.getInt("NumReadsRaw");

                         JSONObject genome = sample.getJSONObject("Genome");
                         String species = genome.getString("SpeciesName");

                         if (sampleInfoLookup.containsKey(id))
                         {
                            error("Error: duplicate sample entries for " + id + " in " + appSessionFilename);
                         }

                         SampleInfo sampleInfo = new SampleInfo();
                         sampleInfo.setId(id);
                         sampleInfo.setName(name);
                         sampleInfo.setPairedEnd(isPairedEnd);
                         sampleInfo.setRead1Length(read1);
                         sampleInfo.setRead2Length(read2);
                         sampleInfo.setNumReadsPF(numReadsPF);
                         sampleInfo.setNumReadsRaw(numReadsRaw);
                         sampleInfo.setSpecies(species);

                         sampleIds.add(id);
                         sampleInfoLookup.put(id, sampleInfo);
                     }
                 }
             }
         }
    }

    /**
     * Creates a sample sheet for the samples extracted from the app session,
     * locating the FASTQ files for each within the corresponding sample data
     * directory.
     *
     * @return the sample sheet file.
     * @throws IOException
     */
    private File createSampleSheet() throws IOException
    {
        File sampleSheetFile = new File(workingDirectory, "samplesheet.txt");
        PrintWriter writer = new PrintWriter(new FileWriter(sampleSheetFile));
        writer.println("Run ID\t" + projectId);
        writer.println("DatasetId\tFile\tSpecies\tControl");
        for (String sampleId : sampleIds)
        {
            SampleInfo sampleInfo = sampleInfoLookup.get(sampleId);
            String species = sampleInfo.getSpecies();
            boolean control = species.equals(controlSpecies);
            List<File> fastqFiles = findFastqFiles(sampleId);
            if (fastqFiles.isEmpty())
            {
                error("Error: no FASTQ files found for sample " + sampleId + " " + sampleInfo.getName());
            }
            for (File fastqFile : fastqFiles)
            {
                writer.println(sampleId + "\t" + fastqFile.getAbsolutePath() + "\t" + species + "\t" + (control ? "Yes" : "No"));
            }
            if (!control)
            {
                writer.println(sampleId + "\t\t" + controlSpecies + "\tYes");
            }
        }
        writer.close();
        return sampleSheetFile;
    }

    /**
     * Finds FASTQ files for the given sample within the corresponding
     * data directory.
     *
     * @param sampleId the sample identifier.
     * @return a list of FASTQ files for the given sample.
     */
    private List<File> findFastqFiles(String sampleId)
    {
        List<File> fastqFiles = new ArrayList<File>();

        File dataDirectory = new File(INPUT_DATA_DIR, sampleId);
        if (!dataDirectory.exists())
        {
            error("Error: missing sample datadirectory: " + dataDirectory.getAbsolutePath());
        }
        for (File file : dataDirectory.listFiles(FASTQ_FILENAME_FILTER))
        {
            fastqFiles.add(file);
        }

        dataDirectory = new File(dataDirectory, BASE_CALLS_SUBDIRECTORY);
        if (dataDirectory.exists())
        {
            for (File file : dataDirectory.listFiles(FASTQ_FILENAME_FILTER))
            {
                fastqFiles.add(file);
            }
        }

        return fastqFiles;
    }

    /**
     * Creates a pipeline metadata (configuration) file from the given sample sheet.
     *
     * @param sampleSheetFile the sample sheet file.
     * @return the pipeline metadata file.
     */
    private File createPipelineMetadataFile(File sampleSheetFile)
    {
        File metadataFile = new File(workingDirectory, "config.xml");
        CreateMetadataFromSampleSheet createMetadataFromSampleSheet = new CreateMetadataFromSampleSheet(new String[] { "-o", metadataFile.getAbsolutePath(), sampleSheetFile.getAbsolutePath() });
        createMetadataFromSampleSheet.setWorkingDirectory(workingDirectory);
        createMetadataFromSampleSheet.setResourcesDirectory(resourcesDirectory);
        createMetadataFromSampleSheet.execute();
        return metadataFile;
    }

    /**
     * Inner class for holding information about a sample.
     *
     * @author eldrid01
     */
    public class SampleInfo
    {
        private String id;
        private String name;
        private boolean pairedEnd;
        private int read1Length;
        private int read2Length;
        private int numReadsPF;
        private int numReadsRaw;
        private String species;

        public String getId()
        {
            return id;
        }

        public void setId(String id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public boolean isPairedEnd()
        {
            return pairedEnd;
        }

        public void setPairedEnd(boolean pairedEnd)
        {
            this.pairedEnd = pairedEnd;
        }

        public int getRead1Length()
        {
            return read1Length;
        }

        public void setRead1Length(int read1Length)
        {
            this.read1Length = read1Length;
        }

        public int getRead2Length()
        {
            return read2Length;
        }

        public void setRead2Length(int read2Length)
        {
            this.read2Length = read2Length;
        }

        public int getNumReadsPF()
        {
            return numReadsPF;
        }

        public void setNumReadsPF(int numReadsPF)
        {
            this.numReadsPF = numReadsPF;
        }

        public int getNumReadsRaw()
        {
            return numReadsRaw;
        }

        public void setNumReadsRaw(int numReadsRaw)
        {
            this.numReadsRaw = numReadsRaw;
        }

        public String getSpecies()
        {
            return species;
        }

        public void setSpecies(String species)
        {
            this.species = species;
        }
    }
}
