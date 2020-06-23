package org.cruk.mga.export;

import java.io.Serializable;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Property implements Serializable
{
    private static final long serialVersionUID = -480829562256350157L;

    @XmlAttribute
    private String name;

    @XmlAttribute
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
