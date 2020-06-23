package org.cruk.mga.report;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.output.FileWriterWithEncoding;
import org.cruk.mga.MGAConfig;
import org.cruk.mga.export.AllMGASummaries;

import com.sun.xml.txw2.output.IndentingXMLStreamWriter;

public class XMLReportWriter extends MGAReportWriter
{
    protected JAXBContext jaxbContext;

    public XMLReportWriter() throws JAXBException
    {
        jaxbContext = JAXBContext.newInstance("org.cruk.mga.export");
    }

    protected void writeTheReport(MGAConfig config, AllMGASummaries summaries)
    throws IOException, TransformerException, JAXBException, XMLStreamException
    {
        Marshaller marshaller = jaxbContext.createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, true);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, outputEncoding);

        try (Writer writer = new BufferedWriter(new FileWriterWithEncoding(config.getXmlFile(), outputEncoding)))
        {
            XMLOutputFactory xmlOutputFactory = XMLOutputFactory.newInstance();
            IndentingXMLStreamWriter xmlStream = new IndentingXMLStreamWriter(xmlOutputFactory.createXMLStreamWriter(writer));
            xmlStream.setIndentStep("  ");
            xmlStream.writeStartDocument(outputEncoding, "1.0");
            marshaller.marshal(summaries, xmlStream);
            xmlStream.writeEndDocument();
            xmlStream.flush();
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
}
