package org.cruk.mga.export;

import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
@JsonPropertyOrder({ "name", "value" })
public class Property implements Serializable
{
    private static final long serialVersionUID = -480829562256350157L;

    @XmlAttribute(required = true)
    @JsonProperty(required = true)
    private String name;

    @XmlAttribute
    @JsonProperty
    private String value;

    public Property(Map.Entry<String, String> entry)
    {
        this(entry.getKey(), entry.getValue());
    }

    public Property(String n, String v)
    {
        name = n;
        value = v;
    }
}
