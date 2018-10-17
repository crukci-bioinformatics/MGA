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

package org.cruk.mga;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Class providing a mapping between reference genome IDs and species.
 *
 * @author eldrid01
 */
public class ReferenceGenomeSpeciesMapping
{
    private Map<String, String> referenceGenomeSpeciesLookup = new HashMap<String, String>();
    private Map<String, String> referenceGenomeIdLookup = new HashMap<String, String>();

    /**
     * Loads reference genome ID to species mappings from the given properties
     * file.
     *
     * @param referenceGenomeMappingFile the reference genome mapping file
     * @throws FileNotFoundException
     * @throws IOException
     */
    public void loadFromPropertiesFile(File referenceGenomeMappingFile) throws FileNotFoundException, IOException
    {
        try (InputStream fileStream = new FileInputStream(referenceGenomeMappingFile))
        {
            Properties properties = new Properties();
            properties.load(fileStream);

            Enumeration<?> referenceGenomeIds = properties.keys();
            while (referenceGenomeIds.hasMoreElements())
            {
                String referenceGenomeId = (String)referenceGenomeIds.nextElement();
                String[] names = properties.getProperty(referenceGenomeId).split("\\|");

                List<String> nameList = new ArrayList<String>();
                for (String name : names)
                {
                    name = name.trim();
                    if (name.length() > 0)
                    {
                        nameList.add(name);
                    }
                }

                if (nameList.isEmpty())
                {
                    referenceGenomeSpeciesLookup.put(referenceGenomeId, "Not specified");
                }
                else
                {
                    referenceGenomeSpeciesLookup.put(referenceGenomeId, nameList.get(0));
                    for (String name : nameList)
                    {
                        name = name.toLowerCase();
                        if (!referenceGenomeIdLookup.containsKey(name))
                        {
                            referenceGenomeIdLookup.put(name, referenceGenomeId);
                        }
                    }
                }
            }
        }
    }

    /**
     * Returns the reference genome ID for the given species name/synonym.
     *
     * @param species the name of the species.
     *
     * @return The reference genome ID for the given species, or null if it isn't mapped.
     */
    public String getReferenceGenomeId(String species)
    {
        return referenceGenomeIdLookup.get(species.toLowerCase());
    }

    /**
     * Returns the species display name for the given reference genome ID.
     *
     * @param referenceGenomeId the reference genome identifier
     *
     * @return The species of the given reference genome, or null if it isn't mapped.
     */
    public String getSpecies(String referenceGenomeId)
    {
        return referenceGenomeSpeciesLookup.get(referenceGenomeId);
    }

    /**
     * Get an unordered list of all the reference genome identifiers.
     *
     * @return A list of reference genome IDs.
     */
    public Collection<String> listReferenceGenomeIds()
    {
        return Collections.unmodifiableCollection(referenceGenomeIdLookup.keySet());
    }

    /**
     * Get an unordered list of all the species names.
     *
     * @return A list of species.
     */
    public Collection<String> listSpecies()
    {
        return Collections.unmodifiableCollection(referenceGenomeSpeciesLookup.keySet());
    }
}
