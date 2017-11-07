/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2017 Cancer Research UK Cambridge Institute
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.cruk.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple class for holding name/value properties and which retains the order
 * in which these are inserted.
 */
public class OrderedProperties
{
    private List<String> names = new ArrayList<String>();
    private Map<String, String> values = new HashMap<String, String>();

    public String[] getPropertyNames()
    {
        return names.toArray(new String[0]);
    }

    public void setProperty(String name, String value)
    {
        if (names.contains(name))
        {
            names.remove(name);
        }
        names.add(name);
        values.put(name, value);
    }

    public String getProperty(String name)
    {
        return values.get(name);
    }

    /**
     * Returns the value of the property matching the first of the given
     * set of names.
     *
     * @param names the possible names for the property.
     * @return the property value.
     */
    public String getProperty(String[] names)
    {
        for (String name : names)
        {
            String value = values.get(name);
            if (value != null) return value;
        }
        return null;
    }

    public String removeProperty(String name)
    {
        names.remove(name);
        return values.remove(name);
    }
}
