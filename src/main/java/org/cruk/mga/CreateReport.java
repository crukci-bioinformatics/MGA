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

package org.cruk.mga;

import static org.cruk.mga.MGAConfig.DEFAULT_PLOT_WIDTH;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PatternOptionBuilder;
import org.cruk.mga.report.SummaryPlotter;
import org.cruk.mga.report.XMLReportWriter;
import org.cruk.mga.report.YAMLReportWriter;
import org.cruk.util.CommandLineUtility;
import org.cruk.util.OrderedProperties;

import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ValidityException;

public class CreateReport extends CommandLineUtility
{
    protected MGAConfig config;
    protected String[] resultsFiles;

    protected ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping = new ReferenceGenomeSpeciesMapping();
    protected Map<String, MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries = new TreeMap<>();
    protected Map<String, String> datasetDisplayLabels = new HashMap<>();

    protected Builder xmlParser = new Builder();

    /**
     * Runs the CreateReport utility with the given command-line arguments.
     *
     * @param args
     */
    public static void main(String[] args)
    {
        CreateReport createReport = new CreateReport(args);
        createReport.execute();
    }

    /**
     * Initializes a new CreateFastq utility instance with the given command-line arguments.
     *
     * @param args The raw command line arguments given to the JVM.
     */
    protected CreateReport(String[] args)
    {
        super("results_files", args);
    }

    /**
     * {@inheritDoc}
     */
    protected void setupOptions()
    {
        Option option = new Option("i", "run-id", true, "The run identifier");
        option.setRequired(true);
        options.addOption(option);

        options.addOption("o", "output-filename-prefix", true, "File name prefix for output report, image and xml file");
        options.addOption("s", "sample-sheet-file", true, "Sample sheet file");
        options.addOption("r", "reference-genome-mapping-file", true, "Reference genome to species mapping file");
        options.addOption("x", "xsl-stylesheet-file", true, "XSL stylesheet file");
        options.addOption("d", "separate-dataset-reports", false, "To create individual reports for each dataset");
        options.addOption("p", "dataset-report-filename-prefix", true, "File name prefix for creating separate report for each dataset");

        option = new Option("m", "minimum-sequence-count", true, "The minimum number of sequences to display on the x-axis.");
        option.setType(PatternOptionBuilder.NUMBER_VALUE);
        option.setArgName("<int>");
        options.addOption(option);

        option = new Option("w", "plot-width", true, "The width of the plot in pixels (default: " + DEFAULT_PLOT_WIDTH + ")");
        option.setType(PatternOptionBuilder.NUMBER_VALUE);
        option.setArgName("<size>");
        options.addOption(option);

        option = new Option(null, "trim-start", true, "The position within sequences from which to start trimming for alignment; any bases before this position will be trimmed");
        option.setType(PatternOptionBuilder.NUMBER_VALUE);
        option.setArgName("<int>");
        options.addOption(option);

        option = new Option(null, "trim-length", true, "The length to trim sequences to for alignment");
        option.setType(PatternOptionBuilder.NUMBER_VALUE);
        option.setArgName("<int>");
        options.addOption(option);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void parseCommandLine(CommandLine commandLine) throws ParseException
    {
        config = new MGAConfig();

        config.setRunId(commandLine.getOptionValue("run-id"));

        config.setOutputPrefix(commandLine.getOptionValue("output-filename-prefix", "results"));

        config.setSampleSheetFilename(commandLine.getOptionValue("sample-sheet-file"));

        config.setReferenceGenomeMappingFilename(commandLine.getOptionValue("reference-genome-mapping-file"));

        config.setXSLStyleSheetFilename(commandLine.getOptionValue("xsl-stylesheet-file"));

        config.setSeparateDatasetReports(commandLine.hasOption("separate-dataset-reports"));

        config.setDatasetReportFilenamePrefix(commandLine.getOptionValue("dataset-report-filename-prefix", "results_"));

        config.setPlotWidth((Number)commandLine.getParsedOptionValue("plot-width"));

        config.setMinimumSequenceCount((Number)commandLine.getParsedOptionValue("minimum-sequence-count"));

        config.setTrimStart((Number)commandLine.getParsedOptionValue("trim-start"));

        config.setTrimLength((Number)commandLine.getParsedOptionValue("trim-length"));

        resultsFiles = commandLine.getArgs();

        if (resultsFiles.length == 0)
        {
            error("Error parsing command line: missing arguments", true);
        }
    }

    @Override
    protected void run() throws Exception
    {
        readSourceFiles();
        writeReportFiles();
    }

    protected void readSourceFiles() throws IOException, ValidityException, ParsingException
    {
        readReferenceGenomeMapping();
        readCountSummaryFiles();
        readSamplingSummaryFiles();
        readAdapterAlignmentFiles();
        readAlignments();
    }

    protected void writeReportFiles() throws Exception
    {
        OrderedProperties runProperties = readSampleSheet();

        new SummaryPlotter().createSummaryPlot(config, referenceGenomeSpeciesMapping, multiGenomeAlignmentSummaries.values(), datasetDisplayLabels);

        new XMLReportWriter().writeReport(config, referenceGenomeSpeciesMapping, multiGenomeAlignmentSummaries.values(), datasetDisplayLabels, runProperties);

        // new YAMLReportWriter().writeReport(config, referenceGenomeSpeciesMapping, multiGenomeAlignmentSummaries.values(), datasetDisplayLabels, runProperties);
    }

    /**
     * Read reference genome mapping file.
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    protected void readReferenceGenomeMapping() throws FileNotFoundException, IOException
    {
        if (config.hasReferenceGenomeMapping())
        {
            referenceGenomeSpeciesMapping.loadFromPropertiesFile(config.getReferenceGenomeMappingFile());
        }
    }

    /**
     * Look up the name (species) for the given reference genome ID.
     *
     * @param referenceGenomeId
     * @return
     */
    protected String getReferenceGenomeName(String referenceGenomeId)
    {
        String name = referenceGenomeSpeciesMapping.getSpecies(referenceGenomeId);
        return name == null ? referenceGenomeId : name;
    }

    /**
     * Look up the preferred name for a given species. Either returns the preferred
     * name if a match is found or the given name if not.
     *
     * @param species
     * @return
     */
    protected String getPreferredSpeciesName(String species)
    {
        String referenceGenomeId = referenceGenomeSpeciesMapping.getReferenceGenomeId(species);
        if (referenceGenomeId == null)
        {
            return species;
        }
        else
        {
            return referenceGenomeSpeciesMapping.getSpecies(referenceGenomeId);
        }
    }

    /**
     * Reads the sample sheet and adds sample information to the alignment summaries.
     */
    protected OrderedProperties readSampleSheet()
    {
        OrderedProperties properties = new OrderedProperties();

        if (config.hasSampleSheet())
        {
            File sampleSheetFile = config.getSampleSheetFile();

            try (BufferedReader reader = new BufferedReader(new FileReader(sampleSheetFile)))
            {
                boolean inDatasetSection = false;
                String[] samplePropertyNames = null;

                String line = null;
                while ((line = reader.readLine()) != null)
                {
                    line = line.trim();
                    if (line.startsWith("#")) continue;

                    String[] fields = line.split("\\t");
                    if (fields.length == 0) continue;
                    for (int i = 0; i < fields.length; i++)
                    {
                        fields[i] = fields[i].trim();
                    }

                    if (inDatasetSection)
                    {
                        String datasetDisplayLabel = fields[0].trim();
                        if (datasetDisplayLabel.length() == 0) continue;

                        String datasetId = datasetDisplayLabel.replaceAll("\\s+", "_");
                        datasetDisplayLabels.put(datasetId, datasetDisplayLabel);

                        MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = multiGenomeAlignmentSummaries.get(datasetId);
                        if (multiGenomeAlignmentSummary == null) continue;

                        OrderedProperties sampleProperties = new OrderedProperties();
                        multiGenomeAlignmentSummary.getSampleProperties().add(sampleProperties);

                        for (int i = 1; i < Math.min(samplePropertyNames.length, fields.length); i++)
                        {
                            String name = samplePropertyNames[i];
                            if (name.equalsIgnoreCase("File")) continue;
                            String value = fields[i].trim();
                            if (name.equalsIgnoreCase("Species"))
                            {
                                name = "Species";
                                value = getPreferredSpeciesName(value);
                            }
                            if (name.equalsIgnoreCase("Control"))
                            {
                                name = "Control";
                                if (value.equalsIgnoreCase("Y") || value.equalsIgnoreCase("Yes"))
                                {
                                    value = "Yes";
                                }
                                else if (value.equalsIgnoreCase("N") || value.equalsIgnoreCase("No"))
                                {
                                    value = "No";
                                }
                            }
                            sampleProperties.put(name, value);
                        }
                    }
                    else
                    {
                        if (fields[0].equalsIgnoreCase("DatasetId"))
                        {
                            inDatasetSection = true;
                            samplePropertyNames = fields;
                            for (int i = 0; i < samplePropertyNames.length; i++)
                            {
                                samplePropertyNames[i] = samplePropertyNames[i].trim();
                            }
                        }
                        else
                        {
                            if (fields.length > 1)
                            {
                                properties.put(fields[0], fields[1]);
                            }
                        }
                    }
                }
            }
            catch (FileNotFoundException e)
            {
                error("Error: could not find file " + sampleSheetFile.getName());
            }
            catch (IOException e)
            {
                error(e);
            }
        }

        return properties;
    }

    /**
     * Identifies and reads sequence count summary files among the input files provided.
     *
     * @throws ValidityException
     * @throws ParsingException
     * @throws IOException
     */
    protected void readCountSummaryFiles() throws ValidityException, ParsingException, IOException
    {
        for (String file : resultsFiles)
        {
            if (file.endsWith(".count.xml"))
            {
                readCountSummaryFile(file);
            }
        }
    }

    /**
     * Reads the given sequence count summary file and creates a new
     * MultiGenomeAlignmentSummary object for the corresponding dataset.
     *
     * @param file
     * @throws ValidityException
     * @throws ParsingException
     * @throws IOException
     */
    protected void readCountSummaryFile(String file) throws ValidityException, ParsingException, IOException
    {
        Document document = xmlParser.build(file);
        Element root = document.getRootElement();
        if (!root.getLocalName().equals("SequenceCountSummary"))
        {
            error("Sequence count summary file " + file + " does not contain root element with name SequenceCountSummary");
        }
        String datasetId = getValue(root, "DatasetId");
        long sequenceCount = getLongValue(root, "SequenceCount");
        MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = new MultiGenomeAlignmentSummary();
        multiGenomeAlignmentSummary.setDatasetId(datasetId);
        multiGenomeAlignmentSummary.setSequenceCount(sequenceCount);
        multiGenomeAlignmentSummaries.put(datasetId, multiGenomeAlignmentSummary);
    }

    /**
     * Identifies and reads sampling summary files among the input files provided.
     *
     * @throws ValidityException
     * @throws ParsingException
     * @throws IOException
     */
    protected void readSamplingSummaryFiles() throws ValidityException, ParsingException, IOException
    {
        for (String file : resultsFiles)
        {
            if (file.endsWith(".sampled.xml"))
            {
                readSamplingSummaryFile(file);
            }
        }
    }

    /**
     * Reads the given sampling summary file.
     *
     * @param file
     * @throws ValidityException
     * @throws ParsingException
     * @throws IOException
     */
    protected void readSamplingSummaryFile(String file) throws ValidityException, ParsingException, IOException
    {
        Document document = xmlParser.build(file);
        Element root = document.getRootElement();
        if (!root.getLocalName().equals("SamplingSummary"))
        {
            error("Sampling summary file " + file + " does not contain root element with name SamplingSummary");
        }
        String datasetId = getValue(root, "DatasetId");
        int sampledCount = getIntegerValue(root, "SampledCount");
        MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = multiGenomeAlignmentSummaries.get(datasetId);
        if (multiGenomeAlignmentSummary == null)
        {
            error("Missing sequence count file for dataset " + datasetId + " corresponding to sampling summary file " + file);
        }
        multiGenomeAlignmentSummary.setSampledCount(sampledCount);
    }

    /**
     * Returns the value of the specified child element for the given parent.
     *
     * @param parent
     * @param name
     * @return
     */
    protected String getValue(Element parent, String name)
    {
        Element child = parent.getFirstChildElement(name);
        if (child == null)
        {
            error("Could not find " + name + " child element of " + parent.getLocalName());
        }
        return child.getValue();
    }

    /**
     * Returns the integer value of the specified child element for the given
     * parent.
     *
     * @param parent
     * @param name
     * @return
     */
    protected int getIntegerValue(Element parent, String name)
    {
        String value = getValue(parent, name);
        int integerValue = 0;
        try
        {
            integerValue = Integer.parseInt(value);
        }
        catch (NumberFormatException e)
        {
            error("Non-integer value of " + name + " child element of " + parent.getLocalName());
        }
        return integerValue;
    }

    /**
     * Returns the long value of the specified child element for the given
     * parent.
     *
     * @param parent
     * @param name
     * @return
     */
    protected long getLongValue(Element parent, String name)
    {
        String value = getValue(parent, name);
        long longValue = 0;
        try
        {
            longValue = Long.parseLong(value);
        }
        catch (NumberFormatException e)
        {
            error("Non-integer value of " + name + " child element of " + parent.getLocalName());
        }
        return longValue;
    }

    /**
     * Identifies and reads adapter alignment results files among the input files provided.
     *
     * @throws IOException
     */
    protected void readAdapterAlignmentFiles() throws IOException
    {
        for (String file : resultsFiles)
        {
            if (file.endsWith(".adapter.exonerate.alignment"))
            {
                readAdapterAlignmentFile(file);
            }
        }
    }

    /**
     * Reads the given adapter alignment results file.
     *
     * @param file
     * @throws IOException
     */
    protected void readAdapterAlignmentFile(String file) throws IOException
    {
        Map<String, Set<Integer>> alignedIdsByDataset = new HashMap<String, Set<Integer>>();
        BufferedReader reader = new BufferedReader(new FileReader(file));
        int lineNumber = 0;
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            lineNumber++;
            String[] fields = line.split("\\t");
            int separatorIndex = fields[0].lastIndexOf("_");
            if (separatorIndex == -1)
            {
                error("Incorrect sequence identifier (" + fields[0] + ") at line " + lineNumber + " in file " + file);
            }
            String datasetId = fields[0].substring(0, separatorIndex);
            int sequenceId = -1;
            try
            {
                sequenceId = Integer.parseInt(fields[0].substring(separatorIndex + 1));
            }
            catch (NumberFormatException e)
            {
                error("Incorrect sequence identifier (" + fields[0] + ") at line " + lineNumber + " in file " + file);
            }
            Set<Integer> alignedIds = alignedIdsByDataset.get(datasetId);
            if (alignedIds == null)
            {
                alignedIds = new HashSet<Integer>();
                alignedIdsByDataset.put(datasetId, alignedIds);
            }
            alignedIds.add(sequenceId);
        }
        reader.close();
        for (String datasetId : alignedIdsByDataset.keySet())
        {
            MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = multiGenomeAlignmentSummaries.get(datasetId);
            if (multiGenomeAlignmentSummary == null)
            {
                error("Missing sequence count file for dataset " + datasetId + " corresponding to adapter alignment file " + file);
            }
            int count = multiGenomeAlignmentSummary.getAdapterCount() + alignedIdsByDataset.get(datasetId).size();
            multiGenomeAlignmentSummary.setAdapterCount(count);
        }
    }

    /**
     * Read alignment files, update alignment summary objects and assign reads
     * to reference genomes.
     *
     * @throws IOException
     */
    protected void readAlignments() throws IOException
    {
        // determine which results files are bowtie alignment output files
        List<String> alignmentFileList = new ArrayList<String>();
        for (String resultFile : resultsFiles)
        {
            if (resultFile.endsWith(".bowtie.alignment"))
            {
                alignmentFileList.add(resultFile);
            }
        }
        String[] alignmentFiles = alignmentFileList.toArray(new String[0]);

        AlignmentReader reader = new AlignmentReader(alignmentFiles, config.getRunId());

        // initialize reference genome index mapping
        // initialize alignment summary for each reference genome and dataset
        Set<String> referenceGenomeIds = reader.getReferenceGenomeIds();
        Map<String, Integer> referenceGenomeIndexMapping = new HashMap<String, Integer>();
        int referenceGenomeCount = 0;
        for (String referenceGenomeId : referenceGenomeIds)
        {
            if (!referenceGenomeIndexMapping.containsKey(referenceGenomeId))
            {
                referenceGenomeIndexMapping.put(referenceGenomeId, referenceGenomeCount);
                referenceGenomeCount++;
            }

            for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries.values())
            {
                AlignmentSummary alignmentSummary = multiGenomeAlignmentSummary.getAlignmentSummary(referenceGenomeId);
                if (alignmentSummary == null)
                {
                    alignmentSummary = new AlignmentSummary();
                    alignmentSummary.setReferenceGenomeId(referenceGenomeId);
                    multiGenomeAlignmentSummary.addAlignmentSummary(alignmentSummary);
                }
            }
        }

        // initialize dataset index mapping
        Map<String, Integer> datasetIndexMapping = new HashMap<String, Integer>();
        int datasetCount = 0;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries.values())
        {
            datasetIndexMapping.put(multiGenomeAlignmentSummary.getDatasetId(), datasetCount);
            datasetCount++;
        }

        // first pass through alignments
        int[] bestAlignmentCounts = new int[referenceGenomeIds.size() + 1];

        while (true)
        {
            List<Alignment> alignments = reader.getNextAlignments();
            if (alignments.isEmpty()) break;

            Alignment first = alignments.get(0);
            String datasetId = first.getDatasetId();
            int sequenceId = first.getSequenceId();
            int mismatchCount = first.getMismatchCount();

            MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = multiGenomeAlignmentSummaries.get(datasetId);
            if (multiGenomeAlignmentSummary == null)
            {
                error("Missing sequence count file for dataset " + first.getDatasetId());
            }
            if (sequenceId > multiGenomeAlignmentSummary.getSampledCount())
            {
                error("Sequence number " + sequenceId + " for dataset " + datasetId + " out of range, maximum value should be " + multiGenomeAlignmentSummary.getSampledCount());
            }

            multiGenomeAlignmentSummary.incrementAlignedCount();

            List<Alignment> bestAlignments = new ArrayList<Alignment>();

            for (Alignment alignment : alignments)
            {
                AlignmentSummary alignmentSummary = multiGenomeAlignmentSummary.getAlignmentSummary(alignment.getReferenceGenomeId());
                alignmentSummary.incrementAlignedCount();
                alignmentSummary.addAlignedSequenceLength(alignment.getAlignedLength());
                alignmentSummary.addMismatchCount(alignment.getMismatchCount());
                if (alignment.getMismatchCount() == mismatchCount)
                {
                    bestAlignments.add(alignment);
                    alignmentSummary.incrementPreferentiallyAlignedCount();
                    alignmentSummary.addPreferentiallyAlignedSequenceLength(alignment.getAlignedLength());
                    alignmentSummary.addPreferentiallyAlignedMismatchCount(alignment.getMismatchCount());
                }
            }

            if (bestAlignments.size() == 1)
            {
                Alignment alignment = bestAlignments.get(0);
                AlignmentSummary alignmentSummary = multiGenomeAlignmentSummary.getAlignmentSummary(alignment.getReferenceGenomeId());
                alignmentSummary.incrementUniquelyAlignedCount();
                alignmentSummary.addUniquelyAlignedSequenceLength(alignment.getAlignedLength());
                alignmentSummary.addUniquelyAlignedMismatchCount(alignment.getMismatchCount());
            }

            bestAlignmentCounts[bestAlignments.size()]++;
        }

        log.info("Histogram of best alignments");
        log.info("No. genomes tied as best aligning\tCount");
        for (int i = 1; i < bestAlignmentCounts.length; i++)
        {
            log.info(i + "\t" + bestAlignmentCounts[i]);
        }

        // generate scores for each species based on the assigned sequences from the
        // first pass
        Map<String, List<Double>> datasetScores = new HashMap<String, List<Double>>();
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries.values())
        {
            List<Double> scores = new ArrayList<Double>(referenceGenomeIds.size());
            for (int i = 0; i < referenceGenomeIds.size(); i++) scores.add(0.0);

            int preferentiallyAlignedTotal = 0;
            for (String referenceGenomeId : referenceGenomeIds)
            {
                preferentiallyAlignedTotal += multiGenomeAlignmentSummary.getAlignmentSummary(referenceGenomeId).getPreferentiallyAlignedCount();
            }

            for (String referenceGenomeId : referenceGenomeIds)
            {
                AlignmentSummary alignmentSummary = multiGenomeAlignmentSummary.getAlignmentSummary(referenceGenomeId);
                double score = ((double)alignmentSummary.getPreferentiallyAlignedCount()) / preferentiallyAlignedTotal;
//                if (score > 0.05)
//                {
//                    double score2 =  Math.max(1.0 - 100.0 * alignmentSummary.getPreferentiallyAlignedErrorRate(), 0.0);
//                    score += score2;
//                }
                int referenceGenomeIndex = referenceGenomeIndexMapping.get(referenceGenomeId);
                scores.set(referenceGenomeIndex, score);
            }

            datasetScores.put(multiGenomeAlignmentSummary.getDatasetId(), scores);
        }

        reader = new AlignmentReader(alignmentFiles, config.getRunId());

        while (true)
        {
            List<Alignment> alignments = reader.getNextAlignments();
            if (alignments.isEmpty()) break;

            Alignment first = alignments.get(0);
            String datasetId = first.getDatasetId();
            int mismatchCount = first.getMismatchCount();

            MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = multiGenomeAlignmentSummaries.get(datasetId);

            List<Alignment> bestAlignments = new ArrayList<Alignment>();
            for (Alignment alignment : alignments)
            {
                if (alignment.getMismatchCount() == mismatchCount)
                    bestAlignments.add(alignment);
                else
                    break;
            }

            List<Double> scores = datasetScores.get(datasetId);

            Alignment assigned = null;
            double assignedScore = 0.0;

            for (Alignment alignment : bestAlignments)
            {
                String referenceGenomeId = alignment.getReferenceGenomeId();
                int referenceGenomeIndex = referenceGenomeIndexMapping.get(referenceGenomeId);
                double score = scores.get(referenceGenomeIndex);
                if (assigned == null || score > assignedScore)
                {
                    assigned = alignment;
                    assignedScore = score;
                }
            }

            AlignmentSummary alignmentSummary = multiGenomeAlignmentSummary.getAlignmentSummary(assigned.getReferenceGenomeId());
            alignmentSummary.incrementAssignedCount();
            alignmentSummary.addAssignedSequenceLength(assigned.getAlignedLength());
            alignmentSummary.addAssignedMismatchCount(assigned.getMismatchCount());
        }
    }
}
