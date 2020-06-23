package org.cruk.mga.export;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.cruk.mga.AlignmentSummary;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(propOrder = { "referenceGenome", "alignedCount", "errorRate", "uniquelyAlignedCount", "uniquelyAlignedErrorRate",
                       "preferentiallyAlignedCount", "preferentiallyAlignedErrorRate", "assignedCount", "assignedErrorRate" })
@JsonPropertyOrder({ "referenceGenome", "alignedCount", "errorRate", "uniquelyAlignedCount", "uniquelyAlignedErrorRate",
                     "preferentiallyAlignedCount", "preferentiallyAlignedErrorRate", "assignedCount", "assignedErrorRate" })
public class MGAAlignmentSummary implements Serializable
{
    private static final long serialVersionUID = -1759073805638011218L;

    @XmlElement(name = "ReferenceGenome")
    @JsonProperty("ReferenceGenome")
    private ReferenceGenome referenceGenome;

    @XmlElement(name = "AlignedCount")
    @JsonProperty("AlignedCount")
    private long alignedCount;

    @XmlElement(name = "ErrorRate")
    @JsonProperty("ErrorRate")
    @JsonSerialize(using = LimitedPrecisionFloatSerializer.class)
    private float errorRate;

    @XmlElement(name = "UniquelyAlignedCount")
    @JsonProperty("UniquelyAlignedCount")
    private long uniquelyAlignedCount;

    @XmlElement(name = "UniquelyAlignedErrorRate")
    @JsonProperty("UniquelyAlignedErrorRate")
    @JsonSerialize(using = LimitedPrecisionFloatSerializer.class)
    private float uniquelyAlignedErrorRate;

    @XmlElement(name = "PreferentiallyAlignedCount")
    @JsonProperty("PreferentiallyAlignedCount")
    private long preferentiallyAlignedCount;

    @XmlElement(name = "PreferentiallyAlignedErrorRate")
    @JsonProperty("PreferentiallyAlignedErrorRate")
    @JsonSerialize(using = LimitedPrecisionFloatSerializer.class)
    private float preferentiallyAlignedErrorRate;

    @XmlElement(name = "AssignedCount")
    @JsonProperty("AssignedCount")
    private long assignedCount;

    @XmlElement(name = "AssignedErrorRate")
    @JsonProperty("AssignedErrorRate")
    @JsonSerialize(using = LimitedPrecisionFloatSerializer.class)
    private float assignedErrorRate;

    public MGAAlignmentSummary()
    {
    }

    public MGAAlignmentSummary(AlignmentSummary alignmentSummary)
    {
        alignedCount = alignmentSummary.getAlignedCount();
        errorRate = alignmentSummary.getErrorRate();
        uniquelyAlignedCount = alignmentSummary.getUniquelyAlignedCount();
        uniquelyAlignedErrorRate = alignmentSummary.getUniquelyAlignedErrorRate();
        preferentiallyAlignedCount = alignmentSummary.getPreferentiallyAlignedCount();
        preferentiallyAlignedErrorRate = alignmentSummary.getPreferentiallyAlignedErrorRate();
        assignedCount = alignmentSummary.getAssignedCount();
        assignedErrorRate = alignmentSummary.getAssignedErrorRate();
    }

    public ReferenceGenome getReferenceGenome()
    {
        return referenceGenome;
    }

    public void setReferenceGenome(ReferenceGenome referenceGenome)
    {
        this.referenceGenome = referenceGenome;
    }

    public void setReferenceGenome(String id, String name)
    {
        referenceGenome = new ReferenceGenome(id, name);
    }

    public long getAlignedCount()
    {
        return alignedCount;
    }

    public void setAlignedCount(long alignedCount)
    {
        this.alignedCount = alignedCount;
    }

    public float getErrorRate()
    {
        return errorRate;
    }

    public void setErrorRate(float errorRate)
    {
        this.errorRate = errorRate;
    }

    public long getUniquelyAlignedCount()
    {
        return uniquelyAlignedCount;
    }

    public void setUniquelyAlignedCount(long uniquelyAlignedCount)
    {
        this.uniquelyAlignedCount = uniquelyAlignedCount;
    }

    public float getUniquelyAlignedErrorRate()
    {
        return uniquelyAlignedErrorRate;
    }

    public void setUniquelyAlignedErrorRate(float uniquelyAlignedErrorRate)
    {
        this.uniquelyAlignedErrorRate = uniquelyAlignedErrorRate;
    }

    public long getPreferentiallyAlignedCount()
    {
        return preferentiallyAlignedCount;
    }

    public void setPreferentiallyAlignedCount(long preferentiallyAlignedCount)
    {
        this.preferentiallyAlignedCount = preferentiallyAlignedCount;
    }

    public float getPreferentiallyAlignedErrorRate()
    {
        return preferentiallyAlignedErrorRate;
    }

    public void setPreferentiallyAlignedErrorRate(float preferentiallyAlignedErrorRate)
    {
        this.preferentiallyAlignedErrorRate = preferentiallyAlignedErrorRate;
    }

    public long getAssignedCount()
    {
        return assignedCount;
    }

    public void setAssignedCount(long assignedCount)
    {
        this.assignedCount = assignedCount;
    }

    public float getAssignedErrorRate()
    {
        return assignedErrorRate;
    }

    public void setAssignedErrorRate(float assignedErrorRate)
    {
        this.assignedErrorRate = assignedErrorRate;
    }

}
