package org.cruk.mga.report;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.cruk.mga.AlignmentSummary;
import org.cruk.mga.MGAConfig;
import org.cruk.mga.MultiGenomeAlignmentSummary;
import org.cruk.mga.ReferenceGenomeSpeciesMapping;
import org.cruk.util.OrderedProperties;

import nu.xom.Attribute;
import nu.xom.Builder;
import nu.xom.Document;
import nu.xom.Element;
import nu.xom.Serializer;

public class XMLReportWriter implements MGAReportWriter
{
    protected Builder xmlParser = new Builder();

    public XMLReportWriter()
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
        Element root = new Element("MultiGenomeAlignmentSummaries");

        addElement(root, "RunId", config.getRunId());

        addProperties(root, runProperties);

        if (config.getTrimStart() != null)
        {
            addElement(root, "TrimStart", config.getTrimStart().intValue());
        }
        if (config.getTrimLength() != null)
        {
            addElement(root, "TrimLength", config.getTrimLength().intValue());
        }

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
                referenceGenomeElement.addAttribute(new Attribute("name", getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId)));

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
            referenceGenomeElement.addAttribute(new Attribute("name", getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId)));
        }

        Document document = new Document(root);

        try (OutputStream out = new FileOutputStream(config.getXmlFile()))
        {
            Serializer serializer = new Serializer(out, "ISO-8859-1");
            serializer.setIndent(2);
            serializer.setLineSeparator("\n");
            serializer.write(document);
        }

        if (config.hasXSLStyleSheet())
        {
            File imageFile = config.getImageFile();
            FileInputStream imageInputStream = new FileInputStream(imageFile);
            byte imageByteArray[] = new byte[(int)imageFile.length()];
            imageInputStream.read(imageByteArray);
            imageInputStream.close();

            String imageBase64String = Base64.encodeBase64String(imageByteArray);
            TransformerFactory factory = TransformerFactory.newInstance();
            Source xslt = new StreamSource(config.getXSLStyleSheetFile());
            Transformer transformer = factory.newTransformer(xslt);
            transformer.setParameter("image", imageBase64String);
            Source xmlSource = new StreamSource(config.getXmlFile());
            transformer.transform(xmlSource, new StreamResult(config.getHtmlFile()));
        }
    }

    protected void addElement(Element parent, String name, String value)
    {
        Element element = new Element(name);
        element.appendChild(value);
        parent.appendChild(element);
    }

    protected void addElement(Element parent, String name, int value)
    {
        addElement(parent, name, Integer.toString(value));
    }

    protected void addProperties(Element parent, OrderedProperties properties)
    {
        Element propertiesElement = new Element("Properties");
        parent.appendChild(propertiesElement);

        for (Map.Entry<String, String> prop : properties.entrySet())
        {
            String value = prop.getValue();
            Element propertyElement = new Element("Property");
            propertyElement.addAttribute(new Attribute("name", prop.getKey()));
            if (value != null && value.length() > 0)
            {
                propertyElement.addAttribute(new Attribute("value", value));
            }
            propertiesElement.appendChild(propertyElement);
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

    /**
     * Look up the preferred name for a given species. Either returns the preferred
     * name if a match is found or the given name if not.
     *
     * @param species
     * @return
     */
    protected String getPreferredSpeciesName(ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping, String species)
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

}
