/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2013 Cancer Research UK Cambridge Institute
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

package org.cruk.mga.workflow;

import static org.cruk.mga.workflow.Variables.ADAPTER_FASTA_FILE;
import static org.cruk.mga.workflow.Variables.BOWTIE_EXECUTABLE;
import static org.cruk.mga.workflow.Variables.BOWTIE_INDEX_DIR;
import static org.cruk.mga.workflow.Variables.DATA_DIR;
import static org.cruk.mga.workflow.Variables.EXONERATE_EXECUTABLE;
import static org.cruk.mga.workflow.Variables.OUTPUT_DIR;
import static org.cruk.mga.workflow.Variables.REFERENCE_GENOME_MAPPING_FILE;
import static org.cruk.mga.workflow.Variables.RUN_ID;
import static org.cruk.mga.workflow.Variables.SAMPLE_SHEET_FILE;
import static org.cruk.mga.workflow.Variables.SAMPLE_SIZE;
import static org.cruk.mga.workflow.Variables.TRIM_LENGTH;
import static org.cruk.mga.workflow.Variables.XSL_STYLE_SHEET_FILE;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.cruk.workflow.PipelineConfig;
import org.cruk.workflow.assembly.MetaDataException;
import org.cruk.workflow.xml2.metadata.MetaData;
import org.cruk.workflow.xml2.pipeline.Pipeline;

public class MetaDataValidator implements org.cruk.workflow.assembly.MetaDataValidator
{
    protected Log logger = LogFactory.getLog(MetaDataValidator.class);

    public void validateMetaData(PipelineConfig pipelineConfig, Object origin) throws MetaDataException
    {
        Pipeline pipeline = pipelineConfig.getCurrentPipelineDefinition();
        MetaData metadata = pipelineConfig.getMetaData();

        MetaDataException metadataException = new MetaDataException("Problems with metadata ");

        getVariable(RUN_ID, pipeline, metadata, "run id.", metadataException);
        getVariable(SAMPLE_SHEET_FILE, pipeline, metadata, "sample sheet file", metadataException);
        getIntegerVariable(SAMPLE_SIZE, pipeline, metadata, "sample size", metadataException);
        getIntegerVariable(TRIM_LENGTH, pipeline, metadata, "trim length", metadataException);
        getVariable(DATA_DIR, pipeline, metadata, "data directory", metadataException);
        getVariable(OUTPUT_DIR, pipeline, metadata, "output directory", metadataException);
        getVariable(BOWTIE_INDEX_DIR, pipeline, metadata, "bowtie index dir", metadataException);
        getVariable(ADAPTER_FASTA_FILE, pipeline, metadata, "adapter fasta file", metadataException);
        getVariable(REFERENCE_GENOME_MAPPING_FILE, pipeline, metadata, "reference genome mapping file", metadataException);
        getVariable(BOWTIE_EXECUTABLE, pipeline, metadata, "bowtie executable", metadataException);
        getVariable(EXONERATE_EXECUTABLE, pipeline, metadata, "exonerate executable", metadataException);
        getVariable(XSL_STYLE_SHEET_FILE, pipeline, metadata, "XSL style sheet file", metadataException);

        if (metadataException.hasErrors())
        {
            throw metadataException;
        }
    }

    /**
     * Returns the value for a named numerical variable from the pipeline definition
     * or metadata (metadata value takes precedence).
     *
     * Adds an error to the exception if it doesn't exist.
     *
     * @param name
     * @param pipeline
     * @param metadata
     * @param displayName
     * @param metadataException
     * @return
     */
    public Integer getIntegerVariable(String name, Pipeline pipeline, MetaData metadata, String displayName, MetaDataException metadataException)
    {
        String value = getVariable(name, pipeline, metadata, displayName, metadataException);
        Integer integerValue = null;
        if (value != null)
        {
            try
            {
                int intValue = Integer.parseInt(value);
                integerValue = new Integer(intValue);
            }
            catch (NumberFormatException e)
            {
                metadataException.addVariable(name, "The " + displayName + " is expected to be an integer.");
            }
        }
        return integerValue;
    }

    /**
     * Returns the value of the named variable from the pipeline definition or
     * metadata where the value defined in the metadata takes precedence.
     *
     * Adds an error to the exception if it doesn't exist.
     *
     * @param name
     * @param pipeline
     * @param metadata
     * @param displayName
     * @param metadataException
     * @return
     */
    private String getVariable(String name, Pipeline pipeline, MetaData metadata, String displayName, MetaDataException metadataException)
    {
        String value = metadata.getVariable(name);
        if (value == null)
        {
            value = pipeline.getVariable(name);
            if (value == null)
            {
                metadataException.addVariable(name, "The " + displayName + " is required.");
            }
        }
        return value;
    }
}
