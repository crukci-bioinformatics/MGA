package org.cruk.mga.export;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;

import org.cruk.util.OrderedProperties;


@XmlAccessorType(XmlAccessType.FIELD)
@XmlType
public class Sample implements Serializable
{
    private static final long serialVersionUID = -8710245839002445380L;

    @XmlElement(name = "Properties")
    private Properties properties;


    public Sample()
    {
    }

    public Sample(OrderedProperties props)
    {
        properties = new Properties(props);
    }
}
