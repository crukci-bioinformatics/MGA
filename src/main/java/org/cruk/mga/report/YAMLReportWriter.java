package org.cruk.mga.report;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.USE_NATIVE_TYPE_ID;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;
import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.TransformerException;

import org.cruk.mga.AlignmentSummary;
import org.cruk.mga.MGAConfig;
import org.cruk.mga.MultiGenomeAlignmentSummary;
import org.cruk.mga.ReferenceGenomeSpeciesMapping;
import org.cruk.util.OrderedProperties;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.dataformat.yaml.YAMLGenerator;

public class YAMLReportWriter implements MGAReportWriter
{
    protected static YAMLFactory yfactory = new YAMLFactory();

    static
    {
        yfactory.disable(WRITE_DOC_START_MARKER);
        yfactory.disable(USE_NATIVE_OBJECT_ID);
        yfactory.disable(USE_NATIVE_TYPE_ID);
    }

    public YAMLReportWriter()
    {
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
    public void writeReport(MGAConfig config,
                            ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping,
                            Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries,
                            Map<String, String> datasetDisplayLabels,
                            OrderedProperties runProperties)
    throws IOException, TransformerException
    {
        ObjectNode root = JsonNodeFactory.instance.objectNode();

        root.put("RunId", config.getRunId());

        addProperties(root, runProperties);

        if (config.getTrimStart() != null)
        {
            root.put("TrimStart", config.getTrimStart().intValue());
        }
        if (config.getTrimLength() != null)
        {
            root.put("TrimLength", config.getTrimLength().intValue());
        }

        Set<String> referenceGenomeIds = new HashSet<String>();

        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            ObjectNode multiGenomeAlignmentSummaryElement = root.putObject("MultiGenomeAlignmentSummary");

            String datasetId = multiGenomeAlignmentSummary.getDatasetId();
            String datasetDisplayLabel = datasetDisplayLabels.get(datasetId);

            multiGenomeAlignmentSummaryElement.put("DatasetId", datasetDisplayLabel);
            multiGenomeAlignmentSummaryElement.put("SequenceCount", multiGenomeAlignmentSummary.getSequenceCount());
            multiGenomeAlignmentSummaryElement.put("SampledCount", multiGenomeAlignmentSummary.getSampledCount());
            multiGenomeAlignmentSummaryElement.put("AdapterCount", multiGenomeAlignmentSummary.getAdapterCount());
            multiGenomeAlignmentSummaryElement.put("UnmappedCount", multiGenomeAlignmentSummary.getUnmappedCount());

            ArrayNode alignmentSummariesElement = root.putArray("AlignmentSummaries");

            for (AlignmentSummary alignmentSummary : multiGenomeAlignmentSummary.getAlignmentSummaries())
            {
                ObjectNode alignmentSummaryElement = alignmentSummariesElement.addObject();

                String referenceGenomeId = alignmentSummary.getReferenceGenomeId();
                referenceGenomeIds.add(referenceGenomeId);
                ObjectNode referenceGenomeElement = alignmentSummaryElement.putObject("ReferenceGenome");
                referenceGenomeElement.put("id", referenceGenomeId);
                referenceGenomeElement.put("name", getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId));

                alignmentSummaryElement.put("AlignedCount", alignmentSummary.getAlignedCount());
                alignmentSummaryElement.put("ErrorRate", String.format("%.5f", alignmentSummary.getErrorRate()));
                alignmentSummaryElement.put("UniquelyAlignedCount", alignmentSummary.getUniquelyAlignedCount());
                alignmentSummaryElement.put("UniquelyAlignedErrorRate", String.format("%.5f", alignmentSummary.getUniquelyAlignedErrorRate()));
                alignmentSummaryElement.put("PreferentiallyAlignedCount", alignmentSummary.getPreferentiallyAlignedCount());
                alignmentSummaryElement.put("PreferentiallyAlignedErrorRate", String.format("%.5f", alignmentSummary.getPreferentiallyAlignedErrorRate()));
                alignmentSummaryElement.put("AssignedCount", alignmentSummary.getAssignedCount());
                alignmentSummaryElement.put("AssignedErrorRate", String.format("%.5f", alignmentSummary.getAssignedErrorRate()));
            }

            ArrayNode samplesElement = multiGenomeAlignmentSummaryElement.putArray("Samples");

            for (OrderedProperties sampleProperties : multiGenomeAlignmentSummary.getSampleProperties())
            {
                ObjectNode sampleElement = samplesElement.addObject();
                addProperties(sampleElement, sampleProperties);
            }
        }

        ArrayNode referenceGenomesElement = root.putArray("ReferenceGenomes");
        for (String referenceGenomeId : referenceGenomeIds)
        {
            ObjectNode referenceGenomeElement = referenceGenomesElement.addObject();
            referenceGenomeElement.put("id", referenceGenomeId);
            referenceGenomeElement.put("name", getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId));
        }

        try (OutputStream out = new FileOutputStream(config.getYamlFile()))
        {
            try (YAMLGenerator generator = yfactory.createGenerator(out))
            {
                generator.writeTree(root);
            }
        }
    }

    protected void addProperties(ObjectNode parent, OrderedProperties properties)
    {
        ObjectNode propsNode = parent.putObject("Properties");

        for (Map.Entry<String, String> prop : properties.entrySet())
        {
            String value = prop.getValue();

            if (isEmpty(value))
            {
                propsNode.putNull(prop.getKey());
            }
            else
            {
                propsNode.put(prop.getKey(), value);
            }
        }
    }

    /**
     * Look up the name (species) for the given reference genome ID.
     *
     * @param referenceGenomeId
     * @return
     */
    protected String getReferenceGenomeName(ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping, String referenceGenomeId)
    {
        String name = referenceGenomeSpeciesMapping.getSpecies(referenceGenomeId);
        return name == null ? referenceGenomeId : name;
    }
}
