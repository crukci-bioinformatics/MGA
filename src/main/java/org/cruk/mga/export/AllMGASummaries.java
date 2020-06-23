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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonRootName;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "runId", "properties", "trimStart", "trimLength", "summaries", "referenceGenomes" })
@XmlRootElement(name = "MultiGenomeAlignmentSummaries")
@JsonPropertyOrder({ "runId", "properties", "trimStart", "trimLength", "summaries", "referenceGenomes" })
@JsonRootName("MultiGenomeAlignmentSummaries")
public class AllMGASummaries implements Serializable
{
    private static final long serialVersionUID = -3897311196228179946L;

    @XmlElement(name = "RunID", required = true)
    @JsonProperty(value = "RunID", required = true)
    private String runId;

    @XmlElement(name = "Properties")
    @JsonProperty("Properties")
    private Properties properties;

    @XmlElement(name = "TrimStart")
    @JsonProperty("TrimStart")
    private Long trimStart;

    @XmlElement(name = "TrimLength")
    @JsonProperty("TrimLength")
    private Long trimLength;

    @XmlElement(name = "MultiGenomeAlignmentSummary")
    @JsonProperty("MultiGenomeAlignmentSummary")
    private List<MGASummary> summaries;

    @XmlElementWrapper(name = "ReferenceGenomes")
    @XmlElement(name = "ReferenceGenome")
    @JsonProperty("ReferenceGenomes")
    private List<ReferenceGenome> referenceGenomes;

    public AllMGASummaries()
    {
    }

    public AllMGASummaries(MGAConfig config)
    {
        runId = config.getRunId();
        trimStart = config.getTrimStart() == null ? null : config.getTrimStart().longValue();
        trimLength = config.getTrimLength() == null ? null : config.getTrimLength().longValue();
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

    public Long getTrimStart()
    {
        return trimStart;
    }

    public void setTrimStart(Long trimStart)
    {
        this.trimStart = trimStart;
    }

    public Long getTrimLength()
    {
        return trimLength;
    }

    public void setTrimLength(Long trimLength)
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
