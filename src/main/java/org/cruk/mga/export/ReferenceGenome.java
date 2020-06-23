package org.cruk.mga.export;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
@JsonPropertyOrder({ "id", "name" })
public class ReferenceGenome implements Serializable
{
    private static final long serialVersionUID = -8623385309678136927L;

    @XmlAttribute(required = true)
    @JsonProperty(required = true)
    private String id;

    @XmlAttribute
    @JsonProperty
    private String name;

    public ReferenceGenome()
    {
    }

    public ReferenceGenome(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public String getId()
    {
        return id;
    }

    public void setId(String id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

}
