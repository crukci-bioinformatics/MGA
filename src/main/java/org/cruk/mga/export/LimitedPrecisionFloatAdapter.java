package org.cruk.mga.export;

import static org.apache.commons.lang3.StringUtils.isEmpty;

import java.text.NumberFormat;

import javax.xml.bind.annotation.adapters.XmlAdapter;

public class LimitedPrecisionFloatAdapter extends XmlAdapter<String, Number>
{
    static final NumberFormat FIVEDP_FORMAT = NumberFormat.getNumberInstance();

    static
    {
        FIVEDP_FORMAT.setGroupingUsed(false);
        FIVEDP_FORMAT.setMinimumFractionDigits(1);
        FIVEDP_FORMAT.setMaximumFractionDigits(5);
    }

    public LimitedPrecisionFloatAdapter()
    {
    }

    @Override
    public Number unmarshal(String v) throws Exception
    {
        return isEmpty(v) ? null : Float.valueOf(v);
    }

    @Override
    public String marshal(Number v) throws Exception
    {
        return v == null ? null : FIVEDP_FORMAT.format(v.floatValue());
    }
}
