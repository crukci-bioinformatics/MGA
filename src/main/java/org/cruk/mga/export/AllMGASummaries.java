package org.cruk.mga.export;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.cruk.mga.MGAConfig;
import org.cruk.util.OrderedProperties;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "runId", "properties", "trimStart", "trimLength", "summaries", "referenceGenomes" })
@XmlRootElement(name = "MultiGenomeAlignmentSummaries")
public class AllMGASummaries implements Serializable
{
    private static final long serialVersionUID = -3897311196228179946L;

    @XmlElement(name = "RunId", required = true)
    private String runId;

    @XmlElement(name = "Properties")
    private Properties properties;

    @XmlElement(name = "TrimStart")
    private Integer trimStart;

    @XmlElement(name = "TrimLength")
    private Integer trimLength;

    @XmlElement(name = "MultiGenomeAlignmentSummary")
    private List<MGASummary> summaries;

    @XmlElementWrapper(name = "ReferenceGenomes")
    @XmlElement(name = "ReferenceGenome")
    private List<ReferenceGenome> referenceGenomes;

    public AllMGASummaries()
    {
    }

    public AllMGASummaries(MGAConfig config)
    {
        runId = config.getRunId();
        trimStart = config.getTrimStart();
        trimLength = config.getTrimLength();
    }

    public AllMGASummaries(MGAConfig config, OrderedProperties runProperties)
    {
        this(config);

        properties = new Properties(runProperties);
    }

    public String getRunId()
    {
        return runId;
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }

    public Integer getTrimStart()
    {
        return trimStart;
    }

    public void setTrimStart(Integer trimStart)
    {
        this.trimStart = trimStart;
    }

    public Integer getTrimLength()
    {
        return trimLength;
    }

    public void setTrimLength(Integer trimLength)
    {
        this.trimLength = trimLength;
    }

    public Properties getProperties()
    {
        return properties;
    }

    public void setProperties(OrderedProperties runProperties)
    {
        properties = null;
        if (runProperties != null)
        {
            properties = new Properties(runProperties);
        }
    }

    public List<MGASummary> getMultiGenomeAlignmentSummaries()
    {
        if (summaries == null)
        {
            summaries = new ArrayList<>();
        }
        return summaries;
    }

    public List<ReferenceGenome> getReferenceGenomes()
    {
        if (referenceGenomes == null)
        {
            referenceGenomes = new ArrayList<>();
        }
        return referenceGenomes;
    }

}
