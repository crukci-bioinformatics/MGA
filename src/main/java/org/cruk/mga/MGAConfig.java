package org.cruk.mga;

import static org.apache.commons.lang3.StringUtils.isEmpty;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MGAConfig
{
    public static final long MINIMUM_SEQUENCE_COUNT = 10;

    public static final int DEFAULT_PLOT_WIDTH = 800;
    public static final int MINIMUM_PLOT_WIDTH = 600;

    private static Logger log = LoggerFactory.getLogger(MGAConfig.class);

    private String runId;
    private Number trimStart;
    private Number trimLength;
    private String outputPrefix;
    private String sampleSheetFilename;
    private String referenceGenomeMappingFilename;
    private String xslStyleSheetFilename;
    private boolean separateDatasetReports;
    private String datasetReportFilenamePrefix;
    private int plotWidth;
    private long minimumSequenceCount;

    public MGAConfig()
    {
    }

    public String getRunId()
    {
        return runId;
    }

    public void setRunId(String runId)
    {
        this.runId = runId;
    }

    public Number getTrimStart()
    {
        return trimStart;
    }

    public void setTrimStart(Number trimStart)
    {
        this.trimStart = trimStart;
    }

    public Number getTrimLength()
    {
        return trimLength;
    }

    public void setTrimLength(Number trimLength)
    {
        this.trimLength = trimLength;
    }

    public String getOutputPrefix()
    {
        return outputPrefix;
    }

    public void setOutputPrefix(String outputPrefix)
    {
        this.outputPrefix = outputPrefix;
    }

    public File getXmlFile()
    {
        return isEmpty(outputPrefix) ? null : new File(outputPrefix + ".xml");
    }

    public File getYamlFile()
    {
        return isEmpty(outputPrefix) ? null : new File(outputPrefix + ".yml");
    }

    public File getHtmlFile()
    {
        return isEmpty(outputPrefix) ? null : new File(outputPrefix + ".html");
    }

    public File getImageFile()
    {
        return isEmpty(outputPrefix) ? null : new File(outputPrefix + ".png");
    }

    public boolean hasSampleSheet()
    {
        return isNotEmpty(sampleSheetFilename);
    }

    public File getSampleSheetFile()
    {
        return isEmpty(sampleSheetFilename) ? null : new File(sampleSheetFilename);
    }

    public void setSampleSheetFilename(String sampleSheetFilename)
    {
        this.sampleSheetFilename = sampleSheetFilename;
    }

    public boolean hasReferenceGenomeMapping()
    {
        return isNotEmpty(referenceGenomeMappingFilename);
    }

    public File getReferenceGenomeMappingFile()
    {
        return isEmpty(referenceGenomeMappingFilename) ? null : new File(referenceGenomeMappingFilename);
    }

    public void setReferenceGenomeMappingFilename(String referenceGenomeMappingFilename)
    {
        this.referenceGenomeMappingFilename = referenceGenomeMappingFilename;
    }

    public boolean hasXSLStyleSheet()
    {
        return isNotEmpty(xslStyleSheetFilename);
    }

    public File getXSLStyleSheetFile()
    {
        return isEmpty(xslStyleSheetFilename) ? null : new File(xslStyleSheetFilename);
    }

    public void setXSLStyleSheetFilename(String xslStyleSheetFilename)
    {
        this.xslStyleSheetFilename = xslStyleSheetFilename;
    }

    public boolean isSeparateDatasetReports()
    {
        return separateDatasetReports;
    }

    public void setSeparateDatasetReports(boolean separateDatasetReports)
    {
        this.separateDatasetReports = separateDatasetReports;
    }

    public String getDatasetReportFilenamePrefix()
    {
        return datasetReportFilenamePrefix;
    }

    public void setDatasetReportFilenamePrefix(String datasetReportFilenamePrefix)
    {
        this.datasetReportFilenamePrefix = datasetReportFilenamePrefix;
    }

    public int getPlotWidth()
    {
        return plotWidth;
    }

    public void setPlotWidth(Number plotWidthN)
    {
        plotWidth = plotWidthN == null ? DEFAULT_PLOT_WIDTH : plotWidthN.intValue();
        if (plotWidth < MINIMUM_PLOT_WIDTH)
        {
            log.warn("Minimum width of plot is {} pixels.", MINIMUM_PLOT_WIDTH);
            plotWidth = MINIMUM_PLOT_WIDTH;
        }
    }

    public long getMinimumSequenceCount()
    {
        return minimumSequenceCount;
    }

    public void setMinimumSequenceCount(Number minimumSequenceCountN)
    {
        minimumSequenceCount = minimumSequenceCountN == null ? MINIMUM_SEQUENCE_COUNT : minimumSequenceCountN.longValue();
    }
}
