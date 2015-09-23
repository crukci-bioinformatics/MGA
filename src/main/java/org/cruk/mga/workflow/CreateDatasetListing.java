package org.cruk.mga.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cruk.workflow.execution.TaskMonitor;
import org.cruk.workflow.tasks.AbstractJavaTask;

/**
 * Java task for creating a dataset file listing that can be used by a table
 * loop in the pipeline definition.
 *
 * @author eldrid01
 */
public class CreateDatasetListing extends AbstractJavaTask
{
    private List<String> datasetIds = new ArrayList<String>();
    private Map<String, Set<String>> files = new HashMap<String, Set<String>>();

    private File sampleSheetFile;
    private File datasetListingFile;

    public File getSampleSheetFile()
    {
        return sampleSheetFile;
    }

    public void setSampleSheetFile(File sampleSheetFile)
    {
        this.sampleSheetFile = sampleSheetFile;
    }

    public File getDatasetListingFile()
    {
        return datasetListingFile;
    }

    public void setDatasetListingFile(File datasetListingFile)
    {
        this.datasetListingFile = datasetListingFile;
    }

    @Override
    public void execute(TaskMonitor arg0) throws Throwable
    {
        readSampleSheet();
        writeDatasetListing();
    }

    /**
     * Reads the files for each dataset specified in the sample sheet.
     *
     * @throws IOException
     */
    private void readSampleSheet() throws IOException
    {
        BufferedReader reader = new BufferedReader(new FileReader(sampleSheetFile));
        try
        {
            boolean inDatasetSection = false;
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                String[] fields = line.split("\\t");
                if (inDatasetSection)
                {
                    if (fields.length < 2) break;
                    String id = fields[0].trim();
                    String filename = fields[1].trim();
                    if (id.length() == 0) break;
                    if (filename.length() == 0) continue;
                    Set<String> datasetFiles = files.get(id);
                    if (datasetFiles == null)
                    {
                        datasetIds.add(id);
                        datasetFiles = new HashSet<String>();
                        files.put(id, datasetFiles);
                    }
                    System.err.println(id + "\t" + filename);
                    datasetFiles.add(filename);
                }
                else
                {
                    if (fields.length > 1 && fields[0].trim().equals("DatasetId") && fields[1].trim().equals("File"))
                    {
                        inDatasetSection = true;
                    }
                }
            }
        }
        finally
        {
            try
            {
                reader.close();
            }
            catch (IOException e)
            {
            }
        }
    }

    /**
     * Writes a tab-delimited file with a single row for each dataset specifying
     * the sequence file(s) for that dataset.
     *
     * @throws IOException
     */
    private void writeDatasetListing() throws IOException
    {
        PrintWriter writer = new PrintWriter(new FileWriter(datasetListingFile));
        writer.println("ID\tFiles");
        for (String datasetId : datasetIds)
        {
            writer.print(datasetId);
            writer.print("\t");
            writer.println(StringUtils.join(files.get(datasetId), "|"));
        }
        writer.close();
    }
}
