@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(type = float.class, value = LimitedPrecisionFloatAdapter.class),
    @XmlJavaTypeAdapter(type = Float.class, value = LimitedPrecisionFloatAdapter.class),
    @XmlJavaTypeAdapter(type = double.class, value = LimitedPrecisionFloatAdapter.class),
    @XmlJavaTypeAdapter(type = Double.class, value = LimitedPrecisionFloatAdapter.class)
})
package org.cruk.mga.export;


import javax.xml.bind.annotation.adapters.*;
