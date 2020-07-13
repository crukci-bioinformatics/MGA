package org.cruk.mga.export;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;

import org.cruk.mga.MultiGenomeAlignmentSummary;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "datasetId", "sequenceCount", "sampledCount", "adapterCount", "unmappedCount", "alignmentSummaries", "samples" })
public class MGASummary implements Serializable
{
    private static final long serialVersionUID = 7436117996232221147L;

    @XmlElement(name = "DatasetId", required = true)
    private String datasetId;

    @XmlElement(name = "SequenceCount")
    private long sequenceCount;

    @XmlElement(name = "SampledCount")
    private long sampledCount;

    @XmlElement(name = "AdapterCount")
    private long adapterCount;

    @XmlElement(name = "UnmappedCount")
    private long unmappedCount;

    @XmlElementWrapper(name = "AlignmentSummaries")
    @XmlElement(name = "AlignmentSummary")
    private List<MGAAlignmentSummary> alignmentSummaries;

    @XmlElementWrapper(name = "Samples")
    @XmlElement(name = "Sample")
    private List<Sample> samples;

    public MGASummary()
    {
    }

    public MGASummary(String datasetId, MultiGenomeAlignmentSummary mgas)
    {
        setDatasetId(datasetId);
        sequenceCount = mgas.getSequenceCount();
        sampledCount = mgas.getSampledCount();
        adapterCount = mgas.getAdapterCount();
        unmappedCount = mgas.getUnmappedCount();
    }

    public String getDatasetId()
    {
        return datasetId;
    }

    public void setDatasetId(String datasetId)
    {
        this.datasetId = datasetId;
    }

    public long getSequenceCount()
    {
        return sequenceCount;
    }

    public void setSequenceCount(long sequenceCount)
    {
        this.sequenceCount = sequenceCount;
    }

    public long getSampledCount()
    {
        return sampledCount;
    }

    public void setSampledCount(long sampledCount)
    {
        this.sampledCount = sampledCount;
    }

    public long getAdapterCount()
    {
        return adapterCount;
    }

    public void setAdapterCount(long adapterCount)
    {
        this.adapterCount = adapterCount;
    }

    public long getUnmappedCount()
    {
        return unmappedCount;
    }

    public void setUnmappedCount(long unmappedCount)
    {
        this.unmappedCount = unmappedCount;
    }

    public List<MGAAlignmentSummary> getAlignmentSummaries()
    {
        if (alignmentSummaries == null)
        {
            alignmentSummaries = new ArrayList<>();
        }
        return alignmentSummaries;
    }

    public List<Sample> getSamples()
    {
        if (samples == null)
        {
            samples = new ArrayList<>();
        }
        return samples;
    }

}
