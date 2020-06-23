package org.cruk.mga.report;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.TransformerException;

import org.cruk.mga.AlignmentSummary;
import org.cruk.mga.MGAConfig;
import org.cruk.mga.MultiGenomeAlignmentSummary;
import org.cruk.mga.ReferenceGenomeSpeciesMapping;
import org.cruk.mga.export.AllMGASummaries;
import org.cruk.mga.export.MGAAlignmentSummary;
import org.cruk.mga.export.MGASummary;
import org.cruk.mga.export.ReferenceGenome;
import org.cruk.mga.export.Sample;
import org.cruk.util.OrderedProperties;

public abstract class MGAReportWriter
{
    protected String outputEncoding = StandardCharsets.UTF_8.displayName();

    public MGAReportWriter()
    {
    }

    public void writeReport(MGAConfig config,
                            ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping,
                            Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries,
                            Map<String, String> datasetDisplayLabels,
                            OrderedProperties runProperties)
    throws IOException, TransformerException, JAXBException, XMLStreamException
    {
        AllMGASummaries root = new AllMGASummaries(config, runProperties);

        Set<String> referenceGenomeIds = new HashSet<>();

        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            String datasetId = multiGenomeAlignmentSummary.getDatasetId();
            String datasetDisplayLabel = datasetDisplayLabels.getOrDefault(datasetId, datasetId);

            MGASummary multiGenomeAlignmentSummaryElement = new MGASummary(datasetDisplayLabel, multiGenomeAlignmentSummary);
            root.getMultiGenomeAlignmentSummaries().add(multiGenomeAlignmentSummaryElement);

            for (AlignmentSummary alignmentSummary : multiGenomeAlignmentSummary.getAlignmentSummaries())
            {
                MGAAlignmentSummary alignmentSummaryElement = new MGAAlignmentSummary(alignmentSummary);
                multiGenomeAlignmentSummaryElement.getAlignmentSummaries().add(alignmentSummaryElement);

                String referenceGenomeId = alignmentSummary.getReferenceGenomeId();
                referenceGenomeIds.add(referenceGenomeId);

                alignmentSummaryElement.setReferenceGenome(referenceGenomeId, getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId));
            }

            for (OrderedProperties sampleProperties : multiGenomeAlignmentSummary.getSampleProperties())
            {
                multiGenomeAlignmentSummaryElement.getSamples().add(new Sample(sampleProperties));
            }
        }

        for (String referenceGenomeId : referenceGenomeIds)
        {
            root.getReferenceGenomes().add(new ReferenceGenome(referenceGenomeId, getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId)));
        }

        writeTheReport(config, root);
    }

    protected abstract void writeTheReport(MGAConfig config, AllMGASummaries summaries)
    throws IOException, TransformerException, JAXBException, XMLStreamException;

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
