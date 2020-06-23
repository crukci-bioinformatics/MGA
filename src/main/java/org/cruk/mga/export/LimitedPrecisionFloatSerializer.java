package org.cruk.mga.export;

import static org.cruk.mga.export.LimitedPrecisionFloatAdapter.FIVEDP_FORMAT;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;

public class LimitedPrecisionFloatSerializer extends JsonSerializer<Number>
{
    public LimitedPrecisionFloatSerializer()
    {
    }

    @Override
    public void serialize(Number value, JsonGenerator gen, SerializerProvider serializers) throws IOException
    {
        if (value == null)
        {
            gen.writeNull();
        }
        else
        {
            String trimmed = FIVEDP_FORMAT.format(value.floatValue());
            try
            {
                gen.writeRawValue(trimmed);
            }
            catch (UnsupportedOperationException e)
            {
                gen.writeNumber(value.floatValue());
            }
        }
    }
}
