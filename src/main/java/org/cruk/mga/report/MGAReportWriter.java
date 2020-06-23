package org.cruk.mga.report;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.cruk.mga.MGAConfig;
import org.cruk.mga.MultiGenomeAlignmentSummary;
import org.cruk.mga.ReferenceGenomeSpeciesMapping;
import org.cruk.util.OrderedProperties;

public interface MGAReportWriter
{
    void writeReport(MGAConfig config,
                     ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping,
                     Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries,
                     Map<String, String> datasetDisplayLabels,
                     OrderedProperties runProperties)
     throws IOException, TransformerException;

}
