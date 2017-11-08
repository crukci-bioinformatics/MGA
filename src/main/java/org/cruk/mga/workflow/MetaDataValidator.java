/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Cancer Research UK Cambridge Institute
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

import org.cruk.workflow.PipelineConfig;
import org.cruk.workflow.assembly.MetaDataException;

/**
 * Class for validating the pipeline metadata.
 *
 * @author eldrid01
 */
public class MetaDataValidator implements org.cruk.workflow.assembly.MetaDataValidator
{
    public static final String RUN_ID = "runId";
    public static final String SAMPLE_SHEET_FILE = "sampleSheetFile";

    @Override
    public void validateMetaData(PipelineConfig pipelineConfig, Object origin) throws MetaDataException
    {
        MetaDataException metadataException = new MetaDataException();

        String runId = (String)pipelineConfig.getVariable(RUN_ID);
        if (runId == null || runId.trim().isEmpty())
            metadataException.addVariable(RUN_ID, "Missing '" + RUN_ID + "' variable used in job naming, output file names, etc.");
        runId = runId.trim().replaceAll("\\s+", "_");
        pipelineConfig.getCurrentMetaDefinition().setTransientVariable(RUN_ID, runId);

        String sampleSheetFile = (String)pipelineConfig.getVariable(SAMPLE_SHEET_FILE);
        if (sampleSheetFile == null)
            metadataException.addVariable(SAMPLE_SHEET_FILE, "Missing '" + SAMPLE_SHEET_FILE + "' variable.");

        if (metadataException.hasErrors())
        {
            throw metadataException;
        }
    }
}
