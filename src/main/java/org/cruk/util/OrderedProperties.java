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

import org.apache.commons.collections4.map.LinkedMap;

/**
 * Simple class for holding name/value properties and which retains the order
 * in which these are inserted. Refactored in release 1.6 to use the {@link LinkedMap}
 * class as its implementation, which does the same job.
 */
public class OrderedProperties extends LinkedMap<String, String>
{
    private static final long serialVersionUID = 8837732667981161564L;

    /**
     * Get the key set as an array.
     *
     * @return The key set as an array.
     *
     * @deprecated Use {@link #keySet()} instead and work with the collection.
     */
    @Deprecated
    public String[] getPropertyNames()
    {
        return keySet().toArray(new String[0]);
    }

    /**
     * Set a key to a value.
     *
     * @param name The key.
     * @param value The value;
     *
     * @deprecated Use {@link #put(String, String)} instead.
     */
    @Deprecated
    public void setProperty(String name, String value)
    {
        put(name, value);
    }

    /**
     * Get a value by key.
     *
     * @param name The key of the value to get.
     *
     * @return The value matching the given key, or null if there is no match.
     *
     * @deprecated Use {@link #get(Object)} instead.
     */
    public String getProperty(String name)
    {
        return get(name);
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
            String value = get(name);
            if (value != null) return value;
        }
        return null;
    }

    /**
     * Remove a key and value from the map.
     *
     * @param name The key.
     * @return The value removed, or null.
     *
     * @deprecated Use {@link #remove(Object)} instead.
     */
    @Deprecated
    public String removeProperty(String name)
    {
        return remove(name);
    }
}
