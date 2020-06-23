package org.cruk.mga.report;

import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.USE_NATIVE_OBJECT_ID;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.USE_NATIVE_TYPE_ID;
import static com.fasterxml.jackson.dataformat.yaml.YAMLGenerator.Feature.WRITE_DOC_START_MARKER;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;

import javax.xml.transform.TransformerException;

import org.apache.commons.io.output.FileWriterWithEncoding;
import org.cruk.mga.MGAConfig;
import org.cruk.mga.export.AllMGASummaries;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class YAMLReportWriter extends MGAReportWriter
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

    protected void writeTheReport(MGAConfig config, AllMGASummaries summaries)
    throws IOException, TransformerException
    {
        ObjectMapper mapper = new ObjectMapper(yfactory);

        try (Writer writer = new BufferedWriter(new FileWriterWithEncoding(config.getYamlFile(), outputEncoding)))
        {
            mapper.writeValue(writer, summaries);
        }
    }
}
