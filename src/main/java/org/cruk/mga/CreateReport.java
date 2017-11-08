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

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import javax.imageio.ImageIO;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.codec.binary.Base64;
import org.cruk.util.CommandLineUtility;
import org.cruk.util.OrderedProperties;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.Serializer;
import nu.xom.ValidityException;

public class CreateReport extends CommandLineUtility
{
    private static final String[] SPECIES_PROPERTY_NAMES = new String[] { "Species", "species" };
    private static final String[] CONTROL_PROPERTY_NAMES = new String[] { "Control", "control" };
    private static final int DEFAULT_WIDTH = 800;
    private static final int MINIMUM_WIDTH = 600;
    private static final long MINIMUM_SEQUENCE_COUNT = 10;
    private static final int[] INTERVALS = new int[] {5, 10, 25};
    private static final int OPTIMUM_NO_INTERVALS = 6;
    private static final float ROW_HEIGHT_SCALING_FACTOR = 1.5f;
    private static final float ROW_GAP_SCALING_FACTOR = 2.0f;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int DEFAULT_AXIS_FONT_SIZE = 10;
    private static final int DEFAULT_GAP_SIZE = 10;
    private static final Color ADAPTER_COLOR = new Color(255, 102, 255);
    private static final float MAX_ALPHA = 1.0f;
    private static final float MIN_ALPHA = 0.1f;
    private static final float MIN_ERROR = 0.0025f;
    private static final float MAX_ERROR = 0.01f;

    private String runId;
    private Integer trimStart;
    private Integer trimLength;
    private String outputPrefix;
    private String sampleSheetFilename;
    private String referenceGenomeMappingFilename;
    private String xslStyleSheetFilename;
    private boolean separateDatasetReports;
    private String datasetReportFilenamePrefix;
    private int plotWidth;
    private long minimumSequenceCount;
    private String[] resultsFiles;

    private Font font = new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE);
    private Font axisFont = new Font("SansSerif", Font.PLAIN, DEFAULT_AXIS_FONT_SIZE);
    private int gapSize = DEFAULT_GAP_SIZE;
    private float scaleFactor = 1.0f;

    private ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping = new ReferenceGenomeSpeciesMapping();
    private Map<String, MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries = new TreeMap<String, MultiGenomeAlignmentSummary>();
    private Map<String, String> datasetDisplayLabels = new HashMap<String, String>();

    private Builder xmlParser = new Builder();

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
     * @param args
     */
    private CreateReport(String[] args)
    {
        super("results_files", args);
    }

    @Override
    protected void parseCommandLineArguments(String[] args)
    {
        CommandLineParser parser = new DefaultParser();

        Option option = new Option("i", "run-id", true, "The run identifier");
        option.setRequired(true);
        options.addOption(option);
        options.addOption("o", "output-filename-prefix", true, "File name prefix for output report, image and xml file");
        options.addOption("s", "sample-sheet-file", true, "Sample sheet file");
        options.addOption("r", "reference-genome-mapping-file", true, "Reference genome to species mapping file");
        options.addOption("x", "xsl-stylesheet-file", true, "XSL stylesheet file");
        options.addOption("d", "separate-dataset-reports", false, "To create individual reports for each dataset");
        options.addOption("p", "dataset-report-filename-prefix", true, "File name prefix for creating separate report for each dataset");
        options.addOption("w", "plot-width", true, "The width of the plot in pixels (default: " + DEFAULT_WIDTH + "");
        options.addOption("m", "minimum-sequence-count", true, "The minimum number of sequences to display on the x-axis.");
        options.addOption("s", "trim-start", true, "The position within sequences from which to start trimming for alignment; any bases before this position will be trimmed");
        options.addOption("l", "trim-length", true, "The length to trim sequences to for alignment");

        outputPrefix = "results";
        separateDatasetReports = false;
        datasetReportFilenamePrefix = "results_";

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            runId = commandLine.getOptionValue("run-id");

            if (commandLine.hasOption("output-filename-prefix"))
            {
                outputPrefix = commandLine.getOptionValue("output-filename-prefix");
            }
            outputFilename = outputPrefix + ".xml";

            if (commandLine.hasOption("sample-sheet-file"))
            {
                sampleSheetFilename = commandLine.getOptionValue("sample-sheet-file");
            }

            if (commandLine.hasOption("reference-genome-mapping-file"))
            {
                referenceGenomeMappingFilename = commandLine.getOptionValue("reference-genome-mapping-file");
            }

            if (commandLine.hasOption("xsl-stylesheet-file"))
            {
                xslStyleSheetFilename = commandLine.getOptionValue("xsl-stylesheet-file");
            }

            if (commandLine.hasOption("separate-dataset-reports"))
            {
                separateDatasetReports = true;
            }

            if (commandLine.hasOption("dataset-report-filename-prefix"))
            {
                datasetReportFilenamePrefix = commandLine.getOptionValue("dataset-report-filename-prefix");
            }

            plotWidth = DEFAULT_WIDTH;
            if (commandLine.hasOption("plot-width"))
            {
                try
                {
                    plotWidth = Integer.parseInt(commandLine.getOptionValue("plot-width"));
                }
                catch (NumberFormatException e)
                {
                    error("Width provided is not an integer value");
                }
            }
            if (plotWidth < MINIMUM_WIDTH)
            {
                log.warn("Minimum width of plot is 400 pixels.");
                plotWidth = MINIMUM_WIDTH;
            }

            minimumSequenceCount = MINIMUM_SEQUENCE_COUNT;
            if (commandLine.hasOption("minimum-sequence-count"))
            {
                try
                {
                    minimumSequenceCount = Long.parseLong(commandLine.getOptionValue("minimum-sequence-count"));
                }
                catch (NumberFormatException e)
                {
                    error("Minimum sequence count provided is not an integer value");
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
                    error("The trim length provided is not an integer value");
                }
            }

            if (args.length == 0)
            {
                error("Error parsing command line: missing arguments", true);
            }

            resultsFiles = commandLine.getArgs();
        }
        catch (ParseException e)
        {
            error("Error parsing command-line options: " + e.getMessage(), true);
        }
    }

    @Override
    protected void run() throws IOException, ValidityException, ParsingException, TransformerException
    {
        readReferenceGenomeMapping();
        readCountSummaryFiles();
        readSamplingSummaryFiles();
        readAdapterAlignmentFiles();
        readAlignments();
        OrderedProperties runProperties = readSampleSheet();
        String imageFilename = outputPrefix + ".png";
        String htmlFilename = outputPrefix + ".html";
        createSummaryPlot(multiGenomeAlignmentSummaries.values(), imageFilename);
        writeReport(multiGenomeAlignmentSummaries.values(), runProperties, out, imageFilename, outputFilename, htmlFilename);

        if (separateDatasetReports)
        {
            Collection<MultiGenomeAlignmentSummary> summaries = new ArrayList<MultiGenomeAlignmentSummary>();
            for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries.values())
            {
                summaries.clear();
                summaries.add(multiGenomeAlignmentSummary);
                String datasetId = multiGenomeAlignmentSummary.getDatasetId();
                String prefix = datasetReportFilenamePrefix + datasetId;
                htmlFilename = prefix + ".html";
                imageFilename = prefix + ".png";
                String xmlFilename = prefix + ".xml";
                createSummaryPlot(summaries, imageFilename);
                PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(xmlFilename)));
                writeReport(summaries, runProperties, printStream, imageFilename, xmlFilename, htmlFilename);
                printStream.close();
            }
        }
    }

    /**
     * Read reference genome mapping file.
     *
     * @throws IOException
     * @throws FileNotFoundException
     */
    private void readReferenceGenomeMapping() throws FileNotFoundException, IOException
    {
        if (referenceGenomeMappingFilename != null)
        {
            referenceGenomeSpeciesMapping.loadFromPropertiesFile(referenceGenomeMappingFilename);
        }
    }

    /**
     * Look up the name (species) for the given reference genome ID.
     *
     * @param referenceGenomeId
     * @return
     */
    private String getReferenceGenomeName(String referenceGenomeId)
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
    private String getPreferredSpeciesName(String species)
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
    private OrderedProperties readSampleSheet()
    {
        OrderedProperties properties = new OrderedProperties();
        if (sampleSheetFilename == null || sampleSheetFilename.isEmpty()) return properties;

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
                        sampleProperties.setProperty(name, value);
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
                            properties.setProperty(fields[0], fields[1]);
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

        return properties;
    }

    /**
     * Identifies and reads sequence count summary files among the input files provided.
     *
     * @throws ValidityException
     * @throws ParsingException
     * @throws IOException
     */
    private void readCountSummaryFiles() throws ValidityException, ParsingException, IOException
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
    private void readCountSummaryFile(String file) throws ValidityException, ParsingException, IOException
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
    private void readSamplingSummaryFiles() throws ValidityException, ParsingException, IOException
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
    private void readSamplingSummaryFile(String file) throws ValidityException, ParsingException, IOException
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
    private String getValue(Element parent, String name)
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
    private int getIntegerValue(Element parent, String name)
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
    private long getLongValue(Element parent, String name)
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
    private void readAdapterAlignmentFiles() throws IOException
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
    private void readAdapterAlignmentFile(String file) throws IOException
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
    private void readAlignments() throws IOException
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

        AlignmentReader reader = new AlignmentReader(alignmentFiles, runId);

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

        for (int i = 0; i < bestAlignmentCounts.length; i++)
        {
            System.out.println(i + "\t" + bestAlignmentCounts[i]);
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

        reader = new AlignmentReader(alignmentFiles, runId);

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

    /**
     * Writes the alignment summary report.
     *
     * @param multiGenomeAlignmentSummaries
     * @param runProperties
     * @param out
     * @param imageFilename
     * @param xmlFilename
     * @param htmlFilename
     * @throws IOException
     * @throws TransformerException
     */
    private void writeReport(Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries, OrderedProperties runProperties, PrintStream out, String imageFilename, String xmlFilename, String htmlFilename)
            throws IOException, TransformerException
    {
        Element root = new Element("MultiGenomeAlignmentSummaries");

        addElement(root, "RunId", runId);

        addProperties(root, runProperties);

        if (trimStart != null) addElement(root, "TrimStart", trimStart);
        if (trimLength != null) addElement(root, "TrimLength", trimLength);

        Set<String> referenceGenomeIds = new HashSet<String>();

        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            Element multiGenomeAlignmentSummaryElement = new Element("MultiGenomeAlignmentSummary");
            root.appendChild(multiGenomeAlignmentSummaryElement);

            String datasetId = multiGenomeAlignmentSummary.getDatasetId();
            String datasetDisplayLabel = datasetDisplayLabels.get(datasetId);

            addElement(multiGenomeAlignmentSummaryElement, "DatasetId", datasetDisplayLabel);
            addElement(multiGenomeAlignmentSummaryElement, "SequenceCount", Long.toString(multiGenomeAlignmentSummary.getSequenceCount()));
            addElement(multiGenomeAlignmentSummaryElement, "SampledCount", multiGenomeAlignmentSummary.getSampledCount());
            addElement(multiGenomeAlignmentSummaryElement, "AdapterCount", multiGenomeAlignmentSummary.getAdapterCount());
            addElement(multiGenomeAlignmentSummaryElement, "UnmappedCount", multiGenomeAlignmentSummary.getUnmappedCount());

            Element alignmentSummariesElement = new Element("AlignmentSummaries");
            multiGenomeAlignmentSummaryElement.appendChild(alignmentSummariesElement);

            for (AlignmentSummary alignmentSummary : multiGenomeAlignmentSummary.getAlignmentSummaries())
            {
                Element alignmentSummaryElement = new Element("AlignmentSummary");
                alignmentSummariesElement.appendChild(alignmentSummaryElement);

                String referenceGenomeId = alignmentSummary.getReferenceGenomeId();
                referenceGenomeIds.add(referenceGenomeId);
                Element referenceGenomeElement = new Element("ReferenceGenome");
                alignmentSummaryElement.appendChild(referenceGenomeElement);
                referenceGenomeElement.addAttribute(new Attribute("id", referenceGenomeId));
                referenceGenomeElement.addAttribute(new Attribute("name", getReferenceGenomeName(referenceGenomeId)));

                addElement(alignmentSummaryElement, "AlignedCount", alignmentSummary.getAlignedCount());
                addElement(alignmentSummaryElement, "ErrorRate", String.format("%.5f", alignmentSummary.getErrorRate()));
                addElement(alignmentSummaryElement, "UniquelyAlignedCount", alignmentSummary.getUniquelyAlignedCount());
                addElement(alignmentSummaryElement, "UniquelyAlignedErrorRate", String.format("%.5f", alignmentSummary.getUniquelyAlignedErrorRate()));
                addElement(alignmentSummaryElement, "PreferentiallyAlignedCount", alignmentSummary.getPreferentiallyAlignedCount());
                addElement(alignmentSummaryElement, "PreferentiallyAlignedErrorRate", String.format("%.5f", alignmentSummary.getPreferentiallyAlignedErrorRate()));
                addElement(alignmentSummaryElement, "AssignedCount", alignmentSummary.getAssignedCount());
                addElement(alignmentSummaryElement, "AssignedErrorRate", String.format("%.5f", alignmentSummary.getAssignedErrorRate()));
            }

            Element samplesElement = new Element("Samples");
            multiGenomeAlignmentSummaryElement.appendChild(samplesElement);

            for (OrderedProperties sampleProperties : multiGenomeAlignmentSummary.getSampleProperties())
            {
                Element sampleElement = new Element("Sample");
                samplesElement.appendChild(sampleElement);
                addProperties(sampleElement, sampleProperties);
            }
        }

        Element referenceGenomesElement = new Element("ReferenceGenomes");
        root.appendChild(referenceGenomesElement);
        for (String referenceGenomeId : referenceGenomeIds)
        {
            Element referenceGenomeElement = new Element("ReferenceGenome");
            referenceGenomesElement.appendChild(referenceGenomeElement);
            referenceGenomeElement.addAttribute(new Attribute("id", referenceGenomeId));
            referenceGenomeElement.addAttribute(new Attribute("name", getReferenceGenomeName(referenceGenomeId)));
        }

        Document document = new Document(root);

        Serializer serializer = new Serializer(out, "ISO-8859-1");
        serializer.setIndent(2);
        serializer.setLineSeparator("\n");
        serializer.write(document);
        out.close();

        if (xslStyleSheetFilename != null)
        {
            File imageFile = new File(imageFilename);
            FileInputStream imageInputStream = new FileInputStream(imageFile);
            byte imageByteArray[] = new byte[(int)imageFile.length()];
            imageInputStream.read(imageByteArray);
            imageInputStream.close();

            String imageBase64String = Base64.encodeBase64String(imageByteArray);
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(new File(xslStyleSheetFilename));
            Transformer transformer = factory.newTransformer(xslt);
            transformer.setParameter("image", imageBase64String);
            Source xmlSource = new StreamSource(new File(xmlFilename));
            transformer.transform(xmlSource, new StreamResult(new File(htmlFilename)));
        }
    }

    private void addElement(Element parent, String name, String value)
    {
        Element element = new Element(name);
        element.appendChild(value);
        parent.appendChild(element);
    }

    private void addElement(Element parent, String name, int value)
    {
        addElement(parent, name, Integer.toString(value));
    }

    private void addProperties(Element parent, OrderedProperties properties)
    {
        Element propertiesElement = new Element("Properties");
        parent.appendChild(propertiesElement);


        for (String name : properties.getPropertyNames())
        {
            String value = properties.getProperty(name);
            Element propertyElement = new Element("Property");
            propertyElement.addAttribute(new Attribute("name", name));
            if (value != null && value.length() > 0)
            {
                propertyElement.addAttribute(new Attribute("value", value));
            }
            propertiesElement.appendChild(propertyElement);
        }
    }

    /**
     * Creates a summary plot for the given set of multi-genome alignment summaries.
     *
     * @param multiGenomeAlignmentSummaries
     * @param the name of the image file
     * @throws IOException
     */
    private void createSummaryPlot(Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries, String imageFilename) throws IOException
    {
        if (imageFilename == null) return;

        int n = multiGenomeAlignmentSummaries.size();
        log.debug("Number of summaries = " + n);

        scaleForPlotWidth();

        int fontHeight = getFontHeight();
        int rowHeight = (int)(fontHeight * ROW_HEIGHT_SCALING_FACTOR);
        int labelOffset = (rowHeight - fontHeight) / 2;
        int rowGap = (int)(fontHeight * ROW_GAP_SCALING_FACTOR);
        int height = (rowHeight + rowGap) * (n + 3);
        int rowSeparation = rowHeight + rowGap;

        BufferedImage image = new BufferedImage(plotWidth, height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(Math.max(1.0f, 0.65f * scaleFactor)));

        g2.setFont(font);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, plotWidth, height);
        g2.setColor(Color.BLACK);

        int offset = rowGap + rowHeight - labelOffset;
        int x0 = drawLabels(g2, offset, rowSeparation, multiGenomeAlignmentSummaries);

        long maxSequenceCount = getMaximumSequenceCount(multiGenomeAlignmentSummaries);
        log.debug("Maximum sequence count: " + maxSequenceCount);

        maxSequenceCount = Math.max(maxSequenceCount, minimumSequenceCount);

        long tickInterval = (int)getTickInterval(maxSequenceCount);
        log.debug("Tick interval: " + tickInterval);
        int tickIntervals = (int)(Math.max(1, maxSequenceCount) / tickInterval);
        if (maxSequenceCount % tickInterval != 0) tickIntervals += 1;
        maxSequenceCount = tickIntervals * tickInterval;
        log.debug("No. tick intervals: " + tickIntervals);
        log.debug("Maximum sequence count: " + maxSequenceCount);

        int y = rowGap + n * rowSeparation;
        int x1 = drawAxisAndLegend(g2, x0, y, tickIntervals, maxSequenceCount);

        offset = rowGap;
        drawAlignmentBars(g2, offset, rowHeight, rowSeparation, x0, x1, maxSequenceCount, multiGenomeAlignmentSummaries);

        BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(imageFilename));
        ImageIO.write(image, "png", out);
        out.close();
    }

    /**
     * Scale the font and gap sizes for the size of the plot.
     */
    private void scaleForPlotWidth()
    {
        if (plotWidth > DEFAULT_WIDTH)
        {
            scaleFactor = ((float)plotWidth) / DEFAULT_WIDTH;

            int fontSize = (int)(scaleFactor * DEFAULT_FONT_SIZE);
            font = new Font("SansSerif", Font.PLAIN, fontSize);

            int axisFontSize = (int)(scaleFactor * DEFAULT_AXIS_FONT_SIZE);
            axisFont = new Font("SansSerif", Font.PLAIN, axisFontSize);

            gapSize = (int)(scaleFactor * DEFAULT_GAP_SIZE);
        }
    }

    /**
     * Returns the FONT height.
     *
     * @return
     */
    private int getFontHeight()
    {
        BufferedImage image = new BufferedImage(plotWidth, plotWidth, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();
        g2.setFont(font);
        int fontHeight = g2.getFontMetrics().getAscent();
        g2.dispose();

        return fontHeight;
    }

    /**
     * Returns the maximum sequence count for the given alignment summaries.
     *
     * @param multiGenomeAlignmentSummaries
     * @return
     */
    private long getMaximumSequenceCount(Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
    {
        long maxSequenceCount = 0;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            maxSequenceCount = Math.max(maxSequenceCount, multiGenomeAlignmentSummary.getSequenceCount());
        }
        log.debug("Maximum sequence read count: " + maxSequenceCount);
        return maxSequenceCount;
    }

    /**
     * Returns a reasonable choice for the interval on the x-axis
     * corresponding to the number of sequences given a maximum value.
     *
     * @param max the maximum number of sequences.
     * @return
     */
    private long getTickInterval(long max)
    {
        if (max <= 10) return 1l;
        long scaleFactor = 1l;
        while (true)
        {
            for (int i : INTERVALS)
            {
                long interval = i * scaleFactor;
                if (max / interval <= OPTIMUM_NO_INTERVALS) return interval;
            }
            scaleFactor *= 10;
        }
    }

    /**
     * Draws the labels for each dataset ID, returning the x coordinate for
     * subsequent drawing for each row.
     *
     * @param g2
     * @param offset
     * @param separation
     * @param multiGenomeAlignmentSummaries
     * @return
     */
    private int drawLabels(Graphics2D g2, int offset, int separation, Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
    {
        int n = multiGenomeAlignmentSummaries.size();
        boolean drawNumbers = false;
        if (n > 1)
        {
            int i = 0;
            for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
            {
                i++;
                String datasetId = multiGenomeAlignmentSummary.getDatasetId();
                String datasetDisplayLabel = datasetDisplayLabels.get(datasetId);
                if (!Integer.toString(i).equals(datasetDisplayLabel))
                {
                    drawNumbers = true;
                    break;
                }
            }
        }
        int x = gapSize;
        int y = offset;
        int maxWidth = 0;
        if (drawNumbers)
        {
            for (int i = 1; i <= n; i++)
            {
                String s = Integer.toString(i) + ".";
                g2.drawString(s, x, y);
                maxWidth = Math.max(maxWidth, g2.getFontMetrics().stringWidth(s));
                y += separation;
            }
            x += maxWidth + gapSize / 2;
        }
        y = offset;
        maxWidth = 0;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            String datasetId = multiGenomeAlignmentSummary.getDatasetId();
            String datasetDisplayLabel = datasetDisplayLabels.get(datasetId);
            g2.drawString(datasetDisplayLabel, x, y);
            maxWidth = Math.max(maxWidth, g2.getFontMetrics().stringWidth(datasetDisplayLabel));
            y += separation;
        }
        int acceptableWidth = (int)(0.15 * plotWidth);
        if (maxWidth > acceptableWidth)
        {
            Composite origComposite = g2.getComposite();
            y = offset - g2.getFontMetrics().getHeight() - separation / 4;
            for (int i = 0; i < n; i++)
            {
                g2.setColor(Color.WHITE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                g2.fillRect(x + acceptableWidth, y, gapSize, separation);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.fillRect(x + acceptableWidth + gapSize, y, plotWidth - x - acceptableWidth - gapSize, separation);
                y += separation;
            }
            maxWidth = acceptableWidth;
            g2.setComposite(origComposite);
        }
        return x + maxWidth + gapSize;
    }

    /**
     * Draws the x-axis for the number of sequences and the legend.
     *
     * @param g2
     * @param x0
     * @param y
     * @param tickIntervals
     * @param maxSequenceCount
     * @return
     */
    private int drawAxisAndLegend(Graphics2D g2, int x0, int y, int tickIntervals, long maxSequenceCount)
    {
        g2.setColor(Color.BLACK);
        g2.setFont(axisFont);

        boolean millions = maxSequenceCount / tickIntervals >= 1000000;
        long largestTickValue = maxSequenceCount;
        if (millions) largestTickValue /= 1000000;
        int w = g2.getFontMetrics().stringWidth(Long.toString(largestTickValue));
        int x1 = plotWidth - (w / 2) - gapSize;
        g2.drawLine(x0, y, x1, y);

        int tickFontHeight = g2.getFontMetrics().getAscent();
        int tickHeight = tickFontHeight / 2;
        for (int i = 0; i <= tickIntervals; i++)
        {
            int x = x0 + i * (x1 - x0) / tickIntervals;
            g2.drawLine(x, y, x, y + tickHeight);
            long tickValue = i * maxSequenceCount / tickIntervals;
            if (millions) tickValue /= 1000000;
            String s = Long.toString(tickValue);
            int xs = x - g2.getFontMetrics().stringWidth(s) / 2 + 1;
            int ys = y + tickHeight + tickFontHeight + 1;
            g2.drawString(s, xs, ys);
        }

        g2.setFont(font);
        int fontHeight = g2.getFontMetrics().getAscent();
        String s = "Number of sequences";
        if (millions) s += " (millions)";
        int xs = x0 + (x1 - x0 - g2.getFontMetrics().stringWidth(s)) / 2;
        int ys = y + tickHeight + tickFontHeight + fontHeight + fontHeight / 3;
        g2.drawString(s, xs, ys);

        int yl = ys + fontHeight * 2;
        int xl = x0;

        int barHeight = (int)(fontHeight * 0.7f);
        int barWidth = 3 * barHeight;
        int yb = yl + (int)(fontHeight * 0.3f);
        int gap = (int)(fontHeight * 0.4f);

        g2.setColor(Color.GREEN);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        g2.setFont(axisFont);
        String label = "Sequenced species/genome";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.ORANGE);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Control";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.RED);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Contaminant";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(ADAPTER_COLOR);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Adapter";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Unmapped";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.GRAY);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Unknown";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);

        return x1;
    }

    /**
     * Draws bars representing the total number of sequences for each dataset
     * and the assigned subsets for each species/reference genome to which
     * these have been aligned.
     *
     * @param g2
     * @param offset
     * @param height
     * @param separation
     * @param x0
     * @param x1
     * @param maxSequenceCount
     * @param multiGenomeAlignmentSummaries
     */
    private void drawAlignmentBars(Graphics2D g2, int offset, int height, int separation, int x0, int x1, long maxSequenceCount, Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
    {
        AlignmentSummaryComparator alignmentSummaryComparator = new AlignmentSummaryComparator();

        g2.setColor(Color.BLACK);

        int y = offset;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            int sampledCount = multiGenomeAlignmentSummary.getSampledCount();
            long sequenceCount = multiGenomeAlignmentSummary.getSequenceCount();
            log.debug(multiGenomeAlignmentSummary.getDatasetId() + " " + sequenceCount);

            Set<String> species = new HashSet<String>();
            Set<String> controls = new HashSet<String>();
            for (OrderedProperties sampleProperties : multiGenomeAlignmentSummary.getSampleProperties())
            {
                String value = sampleProperties.getProperty(SPECIES_PROPERTY_NAMES);
                if (value != null) species.add(value);
                String control = sampleProperties.getProperty(CONTROL_PROPERTY_NAMES);
                if ("Yes".equals(control)) controls.add(value);
            }

            double width = (double)sequenceCount * (x1 - x0) / maxSequenceCount;

            int total = 0;
            int x = x0;

            // iterate over alignments for various reference genomes drawing bar for each
            List<AlignmentSummary> alignmentSummaryList = Arrays.asList(multiGenomeAlignmentSummary.getAlignmentSummaries());
            Collections.sort(alignmentSummaryList, alignmentSummaryComparator);
            for (AlignmentSummary alignmentSummary : alignmentSummaryList)
            {
                total += alignmentSummary.getAssignedCount();
                int w = (int)(width * total / sampledCount) - x + x0;

                String referenceGenomeId = alignmentSummary.getReferenceGenomeId();
                String referenceGenomeName = getReferenceGenomeName(referenceGenomeId);
                Color color = Color.RED;
                if (controls.contains(referenceGenomeName))
                {
                    color = Color.ORANGE;
                }
                else if (species.contains(referenceGenomeName))
                {
                    color = Color.GREEN;
                }
                else if (species.isEmpty() || species.contains("Other") || species.contains("other"))
                {
                    color = Color.GRAY;
                }

                float alpha = MAX_ALPHA - (MAX_ALPHA - MIN_ALPHA) * (alignmentSummary.getAssignedErrorRate() - MIN_ERROR) / (MAX_ERROR - MIN_ERROR);
                alpha = Math.max(alpha, MIN_ALPHA);
                alpha = Math.min(alpha, MAX_ALPHA);
                if (alignmentSummary.getAssignedCount() >= 100)
                    log.debug(alignmentSummary.getReferenceGenomeId() + "\t" + alignmentSummary.getAssignedCount() + "\t" + alignmentSummary.getErrorRate() * 100.0f + "\t" + alpha);

                Composite origComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(color);
                g2.fillRect(x, y, w, height);
                g2.setComposite(origComposite);

                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, w, height);
                x += w;
            }

            // bar for all sequences
            g2.drawRect(x0, y, (int)width, height);

            // bar for adapter sequences
            int adapterCount = multiGenomeAlignmentSummary.getAdapterCount();
            log.debug("Adapter count: " + adapterCount + " / " + sampledCount);
            int ya = y + height + height / 5;
            double wa = width * adapterCount / sampledCount;
            if (wa > 2)
            {
                int ha = height / 3;
                g2.setColor(ADAPTER_COLOR);
                g2.fillRect(x0, ya, (int)wa, ha);
                g2.setColor(Color.BLACK);
                g2.drawRect(x0, ya, (int)wa, ha);
            }

            y += separation;
        }
    }

    /**
     * Comparator for alignment summaries based on assigned counts.
     */
    private class AlignmentSummaryComparator implements Comparator<AlignmentSummary>
    {
        public int compare(AlignmentSummary alignmentSummary0, AlignmentSummary alignmentSummary1)
        {
            return alignmentSummary1.getAssignedCount() - alignmentSummary0.getAssignedCount();
        }
    }
}
