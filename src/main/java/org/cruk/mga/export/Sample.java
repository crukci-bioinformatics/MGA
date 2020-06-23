package org.cruk.mga.export;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlElement;

import org.cruk.util.OrderedProperties;

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
