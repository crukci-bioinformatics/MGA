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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.imageio.ImageIO;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.ParsingException;
import nu.xom.ProcessingInstruction;
import nu.xom.Serializer;
import nu.xom.ValidityException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.cruk.util.CommandLineUtility;
import org.cruk.util.OrderedProperties;

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
    private Integer trimLength;
    private String outputImageFilename;
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

    private Builder xmlParser = new Builder();
    private Pattern idPattern = Pattern.compile("(.+)_(\\d+)");

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
        CommandLineParser parser = new GnuParser();

        Option option = new Option("i", "run-id", true, "The run identifier");
        option.setRequired(true);
        options.addOption(option);
        options.addOption("o", "output-xml-file", true, "Output alignment report XML file");
        options.addOption("s", "sample-sheet-file", true, "Sample sheet file");
        options.addOption("r", "reference-genome-mapping-file", true, "Reference genome to species mapping file");
        options.addOption("x", "xsl-stylesheet-file", true, "XSL stylesheet file");
        options.addOption("d", "separate-dataset-reports", false, "To create individual reports for each dataset");
        options.addOption("p", "dataset-report-filename-prefix", true, "File name prefix for creating separate report for each dataset");
        options.addOption("w", "plot-width", true, "The width of the plot in pixels (default: " + DEFAULT_WIDTH + "");
        options.addOption("m", "minimum-sequence-count", true, "The minimum number of sequences to display on the x-axis.");
        options.addOption("l", "trim-length", true, "Trim length of sequences following trimming from 3' end");

        separateDatasetReports = false;
        datasetReportFilenamePrefix = "results_";

        try
        {
            CommandLine commandLine = parser.parse(options, args);

            runId = commandLine.getOptionValue("run-id");

            if (commandLine.hasOption("output-xml-file"))
            {
                outputFilename = commandLine.getOptionValue("output-xml-file");
                outputImageFilename = FilenameUtils.removeExtension(outputFilename) + ".png";
            }

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
                logger.warn("Minimum width of plot is 400 pixels.");
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
    protected void run() throws IOException, ValidityException, ParsingException
    {
        readReferenceGenomeMapping();
        readResultsFiles();
        assignAlignedSequences();
        OrderedProperties runProperties = readSampleSheet(multiGenomeAlignmentSummaries);
        createSummaryPlot(multiGenomeAlignmentSummaries.values(), outputImageFilename);
        writeSummary(multiGenomeAlignmentSummaries.values(), runProperties, out, outputFilename, outputImageFilename);

        if (separateDatasetReports)
        {
            Collection<MultiGenomeAlignmentSummary> summaries = new ArrayList<MultiGenomeAlignmentSummary>();
            for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries.values())
            {
                summaries.clear();
                summaries.add(multiGenomeAlignmentSummary);
                String datasetId = multiGenomeAlignmentSummary.getDatasetId();
                String prefix = datasetReportFilenamePrefix + datasetId;
                String outputFilename = prefix + ".xml";
                String imageFilename = prefix + ".png";
                createSummaryPlot(summaries, imageFilename);
                PrintStream printStream = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputFilename)));
                writeSummary(summaries, runProperties, printStream, outputFilename, imageFilename);
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
    private OrderedProperties readSampleSheet(Map<String, MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
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
                    String datasetId = fields[0];
                    if (datasetId.length() == 0) continue;

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
     * Reads the results files.
     *
     * @throws ValidityException
     * @throws ParsingException
     * @throws IOException
     */
    private void readResultsFiles() throws ValidityException, ParsingException, IOException
    {
        for (String file : resultsFiles)
        {
            if (file.endsWith(".count.xml"))
            {
                readCountSummaryFile(file);
            }
            else if (file.endsWith(".sampled.xml"))
            {
                readSamplingSummaryFile(file);
            }
            else if (file.endsWith(".adapter.exonerate.alignment"))
            {
                readAdapterAlignmentFile(file);
            }
            else if (file.endsWith(".bowtie.alignment"))
            {
                readAlignmentFile(file);
            }
            else
            {
                error("Unrecognized results file: " + file);
            }
        }
    }

    /**
     * Reads the given sequence count summary file.
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
        MultiGenomeAlignmentSummary summary = getMultiGenomeAlignmentSummary(datasetId);
        summary.setSequenceCount(sequenceCount);
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
        MultiGenomeAlignmentSummary summary = getMultiGenomeAlignmentSummary(datasetId);
        summary.setSampledCount(sampledCount);
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
            Matcher matcher = idPattern.matcher(fields[0]);
            if (!matcher.matches())
            {
                error("Incorrect sequence identifier at line " + lineNumber + " in file " + file);
            }
            String datasetId = matcher.group(1);
            int sequenceId = Integer.parseInt(matcher.group(2));
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
            MultiGenomeAlignmentSummary summary = getMultiGenomeAlignmentSummary(datasetId);
            int count = summary.getAdapterCount() + alignedIdsByDataset.get(datasetId).size();
            summary.setAdapterCount(count);
        }
    }

    /**
     * Reads the given alignment results file.
     *
     * @param file
     * @throws IOException
     */
    private void readAlignmentFile(String filename) throws IOException
    {
        File file = new File(filename);
        String referenceGenomeId = file.getName().replaceAll("\\.bowtie\\.alignment$", "").replaceAll("^" + runId + "\\.", "");
        int index = referenceGenomeId.indexOf(".");
        if (index == -1) error("Error determining reference genome for file: " + file);
        referenceGenomeId = referenceGenomeId.substring(index + 1);
        if (referenceGenomeId.isEmpty()) error("Error determining reference genome for file: " + file);

        BufferedReader reader = new BufferedReader(new FileReader(file));

        int lineNumber = 0;
        String line = null;
        while ((line = reader.readLine()) != null)
        {
            lineNumber++;
            String[] fields = line.split("\\t");
            Matcher matcher = idPattern.matcher(fields[0]);
            if (!matcher.matches())
            {
                error("Incorrect sequence identifier at line " + lineNumber + " in file " + file);
            }
            String datasetId = matcher.group(1);
            int sequenceId = Integer.parseInt(matcher.group(2));

            AlignmentSummary alignmentSummary = getAlignmentSummary(datasetId, referenceGenomeId);

            alignmentSummary.addSequenceId(sequenceId);

            int length = fields[4].length();
            alignmentSummary.addAlignedSequenceLength(length);

            if (fields.length > 7)
            {
                String mismatches = fields[7];
                int mismatchCount = mismatches.split(",").length;
                alignmentSummary.addMismatchCount(mismatchCount);
            }
        }

        reader.close();
    }

    /**
     * Returns the multi-genome alignment summary for the given dataset.
     *
     * @param datasetId the dataset identifier.
     * @return the alignment summary.
     */
    private MultiGenomeAlignmentSummary getMultiGenomeAlignmentSummary(String datasetId)
    {
        MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = multiGenomeAlignmentSummaries.get(datasetId);
        if (multiGenomeAlignmentSummary == null)
        {
            multiGenomeAlignmentSummary = new MultiGenomeAlignmentSummary();
            multiGenomeAlignmentSummary.setDatasetId(datasetId);
            multiGenomeAlignmentSummaries.put(datasetId, multiGenomeAlignmentSummary);
        }
        return multiGenomeAlignmentSummary;
    }

    /**
     * Returns the alignment summary for the specified dataset and reference genome.
     *
     * @param datasetId the dataset identifier.
     * @param referenceGenomeId the reference genome identifier.
     * @return the alignment summary.
     */
    private AlignmentSummary getAlignmentSummary(String datasetId, String referenceGenomeId)
    {
        MultiGenomeAlignmentSummary multiGenomeAlignmentSummary = getMultiGenomeAlignmentSummary(datasetId);
        AlignmentSummary alignmentSummary = multiGenomeAlignmentSummary.getAlignmentSummary(referenceGenomeId);
        if (alignmentSummary == null)
        {
            alignmentSummary = new AlignmentSummary();
            alignmentSummary.setReferenceGenomeId(referenceGenomeId);
            multiGenomeAlignmentSummary.addAlignmentSummary(alignmentSummary);
        }
        return alignmentSummary;
    }

    /**
     * Assign sequences to reference genomes for all datasets.
     */
    private void assignAlignedSequences()
    {
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries.values())
        {
            assignAlignedSequences(multiGenomeAlignmentSummary);
        }
    }

    /**
     * Iterative process for assignment of aligned sequences to reference genomes
     * based on which genome has the most hits.
     *
     * @param alignmentSummaries
     * @return the total number of assigned sequences
     */
    private void assignAlignedSequences(MultiGenomeAlignmentSummary multiGenomeAlignmentSummary)
    {
        int assignedTotal = 0;

        Map<String, AlignmentSummary> alignmentSummaryLookup = new HashMap<String, AlignmentSummary>();
        for (AlignmentSummary alignmentSummary : multiGenomeAlignmentSummary.getAlignmentSummaries())
        {
            alignmentSummaryLookup.put(alignmentSummary.getReferenceGenomeId(), alignmentSummary);
            alignmentSummary.setAlignedCount(alignmentSummary.getSequenceCount());
        }

        while (!alignmentSummaryLookup.isEmpty())
        {
            String assignedReferenceGenomeId = null;
            int sequenceCount = 0;

            for (String referenceGenomeId : alignmentSummaryLookup.keySet())
            {
                AlignmentSummary alignmentSummary = alignmentSummaryLookup.get(referenceGenomeId);
                int count = alignmentSummary.getSequenceCount();
                if (assignedReferenceGenomeId == null || count > sequenceCount)
                {
                    assignedReferenceGenomeId = referenceGenomeId;
                    sequenceCount = count;
                }
            }

            if (sequenceCount == 0) break;

            assignedTotal += sequenceCount;

            AlignmentSummary assignedAlignmentSummary = alignmentSummaryLookup.remove(assignedReferenceGenomeId);
            assignedAlignmentSummary.setAssignedCount(sequenceCount);
            Collection<Integer> assignedSequenceIds = assignedAlignmentSummary.getSequenceIds();

            for (String id: alignmentSummaryLookup.keySet())
            {
                AlignmentSummary alignmentSummary = alignmentSummaryLookup.get(id);
                alignmentSummary.removeSequenceIds(assignedSequenceIds);
            }
        }

        int sampledCount = multiGenomeAlignmentSummary.getSampledCount();
        multiGenomeAlignmentSummary.setUnmappedCount(sampledCount - assignedTotal);
    }

    /**
     * Writes the alignment summary report.
     *
     * @param multiGenomeAlignmentSummaries
     * @param runProperties
     * @param out
     * @param outputFilename
     * @param imageFilename
     * @throws IOException
     */
    private void writeSummary(Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries, OrderedProperties runProperties, PrintStream out, String outputFilename, String imageFilename)
            throws IOException
    {
        Element root = new Element("MultiGenomeAlignmentSummaries");

        addElement(root, "RunId", runId);

        addProperties(root, runProperties);

        if (trimLength != null)
        {
            addElement(root, "TrimLength", trimLength);
        }

        if (imageFilename != null) addElement(root, "Image", FilenameUtils.getName(imageFilename));

        Set<String> referenceGenomeIds = new HashSet<String>();

        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            Element multiGenomeAlignmentSummaryElement = new Element("MultiGenomeAlignmentSummary");
            root.appendChild(multiGenomeAlignmentSummaryElement);

            addElement(multiGenomeAlignmentSummaryElement, "DatasetId", multiGenomeAlignmentSummary.getDatasetId());
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
//                addElement(alignmentSummaryElement, "AssignedCount", alignmentSummary.getAssignedCount());
                addElement(alignmentSummaryElement, "AssignedCount", alignmentSummary.getSequenceCount());
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

        if (outputFilename != null && xslStyleSheetFilename != null)
        {
            File outputFile = new File(outputFilename);
            File xslFile = new File(xslStyleSheetFilename);
            File destDir = outputFile.getParentFile();
            if (destDir == null)
            {
                destDir = new File(".");
            }
            FileUtils.copyFileToDirectory(xslFile, destDir);
            ProcessingInstruction stylesheet = new ProcessingInstruction("xml-stylesheet", "type=\"text/xsl\" href=\"" + xslFile.getName() + "\"");
            document.insertChild(stylesheet, 0);
        }

        Serializer serializer = new Serializer(out, "ISO-8859-1");
        serializer.setIndent(2);
        serializer.setLineSeparator("\n");
        serializer.write(document);  
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
        logger.debug(n);

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
        logger.debug("Maximum sequence count: " + maxSequenceCount);

        maxSequenceCount = Math.max(maxSequenceCount, minimumSequenceCount);

        long tickInterval = (int)getTickInterval(maxSequenceCount);
        logger.debug("Tick interval: " + tickInterval);
        int tickIntervals = (int)(Math.max(1, maxSequenceCount) / tickInterval);
        if (maxSequenceCount % tickInterval != 0) tickIntervals += 1;
        maxSequenceCount = tickIntervals * tickInterval;
        logger.debug("No. tick intervals: " + tickIntervals);
        logger.debug("Maximum sequence count: " + maxSequenceCount);

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
        logger.debug("Maximum sequence read count: " + maxSequenceCount);
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
                if (!Integer.toString(i).equals(datasetId))
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
            String s = multiGenomeAlignmentSummary.getDatasetId();
            g2.drawString(s, x, y);
            maxWidth = Math.max(maxWidth, g2.getFontMetrics().stringWidth(s));
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
            logger.debug(multiGenomeAlignmentSummary.getDatasetId() + " " + sequenceCount);

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

                float alpha = MAX_ALPHA - (MAX_ALPHA - MIN_ALPHA) * (alignmentSummary.getErrorRate() - MIN_ERROR) / (MAX_ERROR - MIN_ERROR);
                alpha = Math.max(alpha, MIN_ALPHA);
                alpha = Math.min(alpha, MAX_ALPHA);
                if (alignmentSummary.getAssignedCount() >= 100)
                    logger.debug(alignmentSummary.getReferenceGenomeId() + "\t" + alignmentSummary.getAssignedCount() + "\t" + alignmentSummary.getErrorRate() * 100.0f + "\t" + alpha);

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
            logger.debug("Adapter count: " + adapterCount + " / " + sampledCount);
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
