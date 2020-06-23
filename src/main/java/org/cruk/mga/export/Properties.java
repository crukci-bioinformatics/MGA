package org.cruk.mga.export;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.cruk.util.OrderedProperties;

import com.fasterxml.jackson.annotation.JsonProperty;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Properties implements Serializable
{
    private static final long serialVersionUID = -8315790482372848478L;

    @XmlElement(name = "Property")
    @JsonProperty("Property")
    private List<Property> properties;

    public Properties()
    {
    }

    public Properties(OrderedProperties props)
    {
        properties = new ArrayList<>(props.size());
        for (Map.Entry<String, String> entry : props.entrySet())
        {
            properties.add(new Property(entry));
        }
    }

    public List<Property> getProperties()
    {
        if (properties == null)
        {
            properties = new ArrayList<>();
        }
        return properties;
    }

}
