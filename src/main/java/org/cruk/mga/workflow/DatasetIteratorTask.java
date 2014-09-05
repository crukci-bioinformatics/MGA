package org.cruk.mga.workflow;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.cruk.workflow.execution.MonitorSet;
import org.cruk.workflow.execution.TaskMonitor;
import org.cruk.workflow.tasks.AbstractJavaTask;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

public class DatasetIteratorTask extends AbstractJavaTask
{
    /**
     * Factory for monitor sets.
     */
    @Autowired
    @Qualifier("monitorSet")
    protected ObjectFactory<MonitorSet> monitorSetFactory;

    private File sampleSheetFile;

    private List<String> datasetIds = new ArrayList<String>();
    private Map<String, Set<String>> files = new HashMap<String, Set<String>>();

    public File getSampleSheetFile()
    {
        return sampleSheetFile;
    }

    public void setSampleSheetFile(File sampleSheetFile)
    {
        this.sampleSheetFile = sampleSheetFile;
    }

    @Override
    public void execute(TaskMonitor taskMonitor) throws Throwable
    {
        if (sampleSheetFile == null)
        {
            throw new IllegalStateException("Sample sheet file not set in dataset iterator task.");
        }

        readSampleSheet();

        List<MonitorSet> monitorSets = new ArrayList<MonitorSet>();

        for (String datasetId : datasetIds)
        {
            String datasetFiles = StringUtils.join(files.get(datasetId), "|");
            System.err.println(datasetId + "\t" + datasetFiles);

            configuration.setVariable("datasetId", datasetId);
            configuration.setVariable("datasetFiles", datasetFiles);

            MonitorSet monitorSet = monitorSetFactory.getObject();
            monitorSet.afterCreation(configuration, task);
            monitorSets.add(monitorSet);
            monitorSet.run();
        }

        waitForMonitorsAndThrow("One or more subtasks of the " + getClass().getName() + " task failed", monitorSets);
    }

    /**
     * Reads the files for each dataset specified in the sample sheet.
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
}
