package org.cruk.mga.report;

import static org.cruk.mga.MGAConfig.*;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.imageio.ImageIO;

import org.cruk.mga.AlignmentSummary;
import org.cruk.mga.MGAConfig;
import org.cruk.mga.MultiGenomeAlignmentSummary;
import org.cruk.mga.ReferenceGenomeSpeciesMapping;
import org.cruk.util.OrderedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SummaryPlotter
{
    private static final int[] INTERVALS = new int[] {5, 10, 25};
    private static final int OPTIMUM_NO_INTERVALS = 6;
    private static final float ROW_HEIGHT_SCALING_FACTOR = 1.5f;
    private static final float ROW_GAP_SCALING_FACTOR = 2.0f;
    private static final int DEFAULT_FONT_SIZE = 12;
    private static final int DEFAULT_AXIS_FONT_SIZE = 10;
    private static final int DEFAULT_GAP_SIZE = 10;
    private static final Color ADAPTER_COLOR = new Color(255, 102, 255);
    private static final float MAX_ALPHA = 1.0f;
    private static final float MIN_ALPHA = 0.1f;
    private static final float MIN_ERROR = 0.0025f;
    private static final float MAX_ERROR = 0.01f;

    private static final String[] SPECIES_PROPERTY_NAMES = new String[] { "Species", "species" };
    private static final String[] CONTROL_PROPERTY_NAMES = new String[] { "Control", "control" };

    protected Logger log = LoggerFactory.getLogger(SummaryPlotter.class);

    public SummaryPlotter()
    {
    }

    /**
     * Creates a summary plot for the given set of multi-genome alignment summaries.
     *
     * @param multiGenomeAlignmentSummaries
     * @param the name of the image file
     * @throws IOException
     */
    public void createSummaryPlot(MGAConfig mgaConfig,
                                  ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping,
                                  Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries,
                                  Map<String, String> datasetDisplayLabels)
    throws IOException
    {
        final int n = multiGenomeAlignmentSummaries.size();

        PlotConfig config = new PlotConfig(mgaConfig.getPlotWidth(), n);

        BufferedImage image = new BufferedImage(config.width, config.height, BufferedImage.TYPE_INT_ARGB);

        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setStroke(new BasicStroke(Math.max(1.0f, 0.65f * config.scaleFactor)));

        g2.setFont(config.font);

        g2.setColor(Color.WHITE);
        g2.fillRect(0, 0, config.width, config.height);
        g2.setColor(Color.BLACK);

        int x0 = drawLabels(g2, config, datasetDisplayLabels, multiGenomeAlignmentSummaries);

        long maxSequenceCount = getMaximumSequenceCount(multiGenomeAlignmentSummaries);
        log.debug("Maximum sequence count: " + maxSequenceCount);

        maxSequenceCount = Math.max(maxSequenceCount, mgaConfig.getMinimumSequenceCount());

        long tickInterval = (int)getTickInterval(maxSequenceCount);
        log.debug("Tick interval: " + tickInterval);
        int tickIntervals = (int)(Math.max(1, maxSequenceCount) / tickInterval);
        if (maxSequenceCount % tickInterval != 0)
        {
            tickIntervals += 1;
        }
        maxSequenceCount = tickIntervals * tickInterval;
        log.debug("No. tick intervals: " + tickIntervals);
        log.debug("Maximum sequence count: " + maxSequenceCount);

        int y = config.rowGap + n * config.rowSeparation;
        int x1 = drawAxisAndLegend(g2, config, x0, y, tickIntervals, maxSequenceCount);

        drawAlignmentBars(g2, config, x0, x1, maxSequenceCount, referenceGenomeSpeciesMapping, multiGenomeAlignmentSummaries);

        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(mgaConfig.getImageFile())))
        {
            ImageIO.write(image, "png", out);
        }
    }

    /**
     * Returns the maximum sequence count for the given alignment summaries.
     *
     * @param multiGenomeAlignmentSummaries
     * @return
     */
    protected long getMaximumSequenceCount(Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
    {
        long maxSequenceCount = 0;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            maxSequenceCount = Math.max(maxSequenceCount, multiGenomeAlignmentSummary.getSequenceCount());
        }
        log.debug("Maximum sequence read count: " + maxSequenceCount);
        return maxSequenceCount;
    }

    /**
     * Returns a reasonable choice for the interval on the x-axis
     * corresponding to the number of sequences given a maximum value.
     *
     * @param max the maximum number of sequences.
     * @return
     */
    private long getTickInterval(long max)
    {
        if (max <= 10) return 1l;
        long scaleFactor = 1l;
        while (true)
        {
            for (int i : INTERVALS)
            {
                long interval = i * scaleFactor;
                if (max / interval <= OPTIMUM_NO_INTERVALS)
                {
                    return interval;
                }
            }
            scaleFactor *= 10;
        }
    }

    /**
     * Draws the labels for each dataset ID, returning the x coordinate for
     * subsequent drawing for each row.
     *
     * @param g2
     * @param offset
     * @param separation
     * @param multiGenomeAlignmentSummaries
     * @return
     */
    private int drawLabels(Graphics2D g2, PlotConfig config,
                           Map<String, String> datasetDisplayLabels,
                           Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
    {
        int n = multiGenomeAlignmentSummaries.size();
        boolean drawNumbers = false;
        if (n > 1)
        {
            int i = 0;
            for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
            {
                i++;
                String datasetId = multiGenomeAlignmentSummary.getDatasetId();
                String datasetDisplayLabel = datasetDisplayLabels.get(datasetId);
                if (!Integer.toString(i).equals(datasetDisplayLabel))
                {
                    drawNumbers = true;
                    break;
                }
            }
        }
        int x = config.gapSize;
        int y = config.offset;
        int maxWidth = 0;
        if (drawNumbers)
        {
            for (int i = 1; i <= n; i++)
            {
                String s = Integer.toString(i) + ".";
                g2.drawString(s, x, y);
                maxWidth = Math.max(maxWidth, g2.getFontMetrics().stringWidth(s));
                y += config.rowSeparation;
            }
            x += maxWidth + config.gapSize / 2;
        }
        y = config.offset;
        maxWidth = 0;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            String datasetId = multiGenomeAlignmentSummary.getDatasetId();
            String datasetDisplayLabel = datasetDisplayLabels.get(datasetId);
            g2.drawString(datasetDisplayLabel, x, y);
            maxWidth = Math.max(maxWidth, g2.getFontMetrics().stringWidth(datasetDisplayLabel));
            y += config.rowSeparation;
        }
        int acceptableWidth = (int)(0.15 * config.width);
        if (maxWidth > acceptableWidth)
        {
            Composite origComposite = g2.getComposite();
            y = config.offset - g2.getFontMetrics().getHeight() - config.rowSeparation / 4;
            for (int i = 0; i < n; i++)
            {
                g2.setColor(Color.WHITE);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.8f));
                g2.fillRect(x + acceptableWidth, y, config.gapSize, config.rowSeparation);
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
                g2.fillRect(x + acceptableWidth + config.gapSize, y, config.width - x - acceptableWidth - config.gapSize, config.rowSeparation);
                y += config.rowSeparation;
            }
            maxWidth = acceptableWidth;
            g2.setComposite(origComposite);
        }
        return x + maxWidth + config.gapSize;
    }

    /**
     * Draws the x-axis for the number of sequences and the legend.
     *
     * @param g2
     * @param x0
     * @param y
     * @param tickIntervals
     * @param maxSequenceCount
     * @return
     */
    private int drawAxisAndLegend(Graphics2D g2, PlotConfig config, int x0, int y, int tickIntervals, long maxSequenceCount)
    {
        g2.setColor(Color.BLACK);
        g2.setFont(config.axisFont);

        boolean millions = maxSequenceCount / tickIntervals >= 1000000;
        long largestTickValue = maxSequenceCount;
        if (millions) largestTickValue /= 1000000;
        int w = g2.getFontMetrics().stringWidth(Long.toString(largestTickValue));
        int x1 = config.width - (w / 2) - config.gapSize;
        g2.drawLine(x0, y, x1, y);

        int tickFontHeight = g2.getFontMetrics().getAscent();
        int tickHeight = tickFontHeight / 2;
        for (int i = 0; i <= tickIntervals; i++)
        {
            int x = x0 + i * (x1 - x0) / tickIntervals;
            g2.drawLine(x, y, x, y + tickHeight);
            long tickValue = i * maxSequenceCount / tickIntervals;
            if (millions) tickValue /= 1000000;
            String s = Long.toString(tickValue);
            int xs = x - g2.getFontMetrics().stringWidth(s) / 2 + 1;
            int ys = y + tickHeight + tickFontHeight + 1;
            g2.drawString(s, xs, ys);
        }

        g2.setFont(config.font);
        int fontHeight = g2.getFontMetrics().getAscent();
        String s = "Number of sequences";
        if (millions) s += " (millions)";
        int xs = x0 + (x1 - x0 - g2.getFontMetrics().stringWidth(s)) / 2;
        int ys = y + tickHeight + tickFontHeight + fontHeight + fontHeight / 3;
        g2.drawString(s, xs, ys);

        int yl = ys + fontHeight * 2;
        int xl = x0;

        int barHeight = (int)(fontHeight * 0.7f);
        int barWidth = 3 * barHeight;
        int yb = yl + (int)(fontHeight * 0.3f);
        int gap = (int)(fontHeight * 0.4f);

        g2.setColor(Color.GREEN);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        g2.setFont(config.axisFont);
        String label = "Sequenced species/genome";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.ORANGE);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Control";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.RED);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Contaminant";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(ADAPTER_COLOR);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Adapter";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Unmapped";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);
        xl += g2.getFontMetrics().stringWidth(label) + gap * 3;

        g2.setColor(Color.GRAY);
        g2.fillRect(xl, yb, barWidth,  barHeight);
        g2.setColor(Color.BLACK);
        g2.drawRect(xl, yb, barWidth,  barHeight);
        label = "Unknown";
        xl += barWidth + gap;
        g2.drawString(label, xl, yl + fontHeight);

        return x1;
    }

    /**
     * Draws bars representing the total number of sequences for each dataset
     * and the assigned subsets for each species/reference genome to which
     * these have been aligned.
     *
     * @param g2
     * @param offset
     * @param height
     * @param separation
     * @param x0
     * @param x1
     * @param maxSequenceCount
     * @param multiGenomeAlignmentSummaries
     */
    private void drawAlignmentBars(Graphics2D g2, PlotConfig config,
                                   int x0, int x1, long maxSequenceCount,
                                   ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping,
                                   Collection<MultiGenomeAlignmentSummary> multiGenomeAlignmentSummaries)
    {
        AlignmentSummaryComparator alignmentSummaryComparator = new AlignmentSummaryComparator();

        g2.setColor(Color.BLACK);

        int y = config.rowGap;
        for (MultiGenomeAlignmentSummary multiGenomeAlignmentSummary : multiGenomeAlignmentSummaries)
        {
            int sampledCount = multiGenomeAlignmentSummary.getSampledCount();
            long sequenceCount = multiGenomeAlignmentSummary.getSequenceCount();
            log.debug(multiGenomeAlignmentSummary.getDatasetId() + " " + sequenceCount);

            Set<String> species = new HashSet<String>();
            Set<String> controls = new HashSet<String>();
            for (OrderedProperties sampleProperties : multiGenomeAlignmentSummary.getSampleProperties())
            {
                String value = sampleProperties.getProperty(SPECIES_PROPERTY_NAMES);
                if (value != null) species.add(value);
                String control = sampleProperties.getProperty(CONTROL_PROPERTY_NAMES);
                if ("Yes".equals(control)) controls.add(value);
            }

            double width = (double)sequenceCount * (x1 - x0) / maxSequenceCount;

            int total = 0;
            int x = x0;

            // iterate over alignments for various reference genomes drawing bar for each
            List<AlignmentSummary> alignmentSummaryList = Arrays.asList(multiGenomeAlignmentSummary.getAlignmentSummaries());
            Collections.sort(alignmentSummaryList, alignmentSummaryComparator);
            for (AlignmentSummary alignmentSummary : alignmentSummaryList)
            {
                total += alignmentSummary.getAssignedCount();
                int w = (int)(width * total / sampledCount) - x + x0;

                String referenceGenomeId = alignmentSummary.getReferenceGenomeId();
                String referenceGenomeName = getReferenceGenomeName(referenceGenomeSpeciesMapping, referenceGenomeId);
                Color color = Color.RED;
                if (controls.contains(referenceGenomeName))
                {
                    color = Color.ORANGE;
                }
                else if (species.contains(referenceGenomeName))
                {
                    color = Color.GREEN;
                }
                else if (species.isEmpty() || species.contains("Other") || species.contains("other"))
                {
                    color = Color.GRAY;
                }

                float alpha = MAX_ALPHA - (MAX_ALPHA - MIN_ALPHA) * (alignmentSummary.getAssignedErrorRate() - MIN_ERROR) / (MAX_ERROR - MIN_ERROR);
                alpha = Math.max(alpha, MIN_ALPHA);
                alpha = Math.min(alpha, MAX_ALPHA);
                if (alignmentSummary.getAssignedCount() >= 100)
                    log.debug(alignmentSummary.getReferenceGenomeId() + "\t" + alignmentSummary.getAssignedCount() + "\t" + alignmentSummary.getErrorRate() * 100.0f + "\t" + alpha);

                Composite origComposite = g2.getComposite();
                g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
                g2.setColor(color);
                g2.fillRect(x, y, w, config.height);
                g2.setComposite(origComposite);

                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, w, config.height);
                x += w;
            }

            // bar for all sequences
            g2.drawRect(x0, y, (int)width, config.height);

            // bar for adapter sequences
            int adapterCount = multiGenomeAlignmentSummary.getAdapterCount();
            log.debug("Adapter count: " + adapterCount + " / " + sampledCount);
            int ya = y + config.height + config.height / 5;
            double wa = width * adapterCount / sampledCount;
            if (wa > 2)
            {
                int ha = config.height / 3;
                g2.setColor(ADAPTER_COLOR);
                g2.fillRect(x0, ya, (int)wa, ha);
                g2.setColor(Color.BLACK);
                g2.drawRect(x0, ya, (int)wa, ha);
            }

            y += config.rowSeparation;
        }
    }

    /**
     * Look up the name (species) for the given reference genome ID.
     *
     * @param referenceGenomeId
     * @return
     */
    protected String getReferenceGenomeName(ReferenceGenomeSpeciesMapping referenceGenomeSpeciesMapping, String referenceGenomeId)
    {
        String name = referenceGenomeSpeciesMapping.getSpecies(referenceGenomeId);
        return name == null ? referenceGenomeId : name;
    }

    private class PlotConfig
    {
        Font font = new Font("SansSerif", Font.PLAIN, DEFAULT_FONT_SIZE);
        Font axisFont = new Font("SansSerif", Font.PLAIN, DEFAULT_AXIS_FONT_SIZE);
        int gapSize = DEFAULT_GAP_SIZE;
        float scaleFactor = 1.0f;

        int width;
        int height;
        int offset;

        int fontHeight;
        int rowHeight;
        int labelOffset;
        int rowGap;
        int rowSeparation;

        PlotConfig(int plotWidth, int numberOfSummaries)
        {
            this.width = plotWidth;

            scaleForPlotWidth();
            getFontHeight();

            rowHeight = (int)(fontHeight * ROW_HEIGHT_SCALING_FACTOR);
            labelOffset = (rowHeight - fontHeight) / 2;
            rowGap = (int)(fontHeight * ROW_GAP_SCALING_FACTOR);
            height = (rowHeight + rowGap) * (numberOfSummaries + 3);
            rowSeparation = rowHeight + rowGap;

            offset = rowGap + rowHeight - labelOffset;
        }

        /**
         * Scale the font and gap sizes for the size of the plot.
         */
        private void scaleForPlotWidth()
        {
            if (width > DEFAULT_PLOT_WIDTH)
            {
                scaleFactor = ((float)width) / DEFAULT_PLOT_WIDTH;

                int fontSize = (int)(scaleFactor * DEFAULT_FONT_SIZE);
                font = new Font("SansSerif", Font.PLAIN, fontSize);

                int axisFontSize = (int)(scaleFactor * DEFAULT_AXIS_FONT_SIZE);
                axisFont = new Font("SansSerif", Font.PLAIN, axisFontSize);

                gapSize = (int)(scaleFactor * DEFAULT_GAP_SIZE);
            }
        }

        private void getFontHeight()
        {
            BufferedImage image = new BufferedImage(width, width, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = image.createGraphics();
            g2.setFont(font);
            fontHeight = g2.getFontMetrics().getAscent();
            g2.dispose();
        }
    }
}
