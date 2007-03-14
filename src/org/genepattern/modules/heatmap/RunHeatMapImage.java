package org.genepattern.modules.heatmap;

import java.awt.Color;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.heatmap.RowColorScheme;
import org.genepattern.heatmap.image.DisplaySettings;
import org.genepattern.heatmap.image.HeatMapImageDrawer;
import org.genepattern.io.DatasetParser;
import org.genepattern.matrix.Dataset;
import org.genepattern.module.AnalysisUtil;

public class RunHeatMapImage {

    /**
     * Create a heatmap image from the command line <input.filename>
     * <output.filename> <output.format> -cw <column.size> -rw <row.size> -norm
     * <normalization> -grid <grid> -ra <show.row.descriptions> -p
     * <show.row.ids>
     *
     * @param args
     *            The command line arguments
     */
    public static void main(String[] args) {
        String inputFileName = args[0];
        String outputFileName = args[1];
        String outputFileFormat = args[2];

        DatasetParser reader = AnalysisUtil.getDatasetParser(inputFileName);

        Dataset data = AnalysisUtil.readDataset(reader, inputFileName);

        int columnWidth = 10;
        int rowWidth = 10;
        int normalization = HeatMapImageDrawer.COLOR_RESPONSE_ROW;
        Color gridLinesColor = Color.black;
        boolean showGridLines = false;
        boolean showGeneAnnotations = false;
        boolean showGeneNames = true;
        java.util.List featureList = null;
        Color highlightColor = Color.red;
        Color[] colorMap = null;
        for (int i = 3; i < args.length; i++) { // 0th arg is input file name,
            // 1st arg is output file name,
            // 2nd arg is format
            String arg = args[i].substring(0, 2);
            String value = args[i].substring(2, args[i].length());
            if (value.equals("")) {
                continue;
            }

            if (arg.equals("-c")) {
                columnWidth = Integer.parseInt(value);
            } else if (arg.equals("-r")) {
                rowWidth = Integer.parseInt(value);
            } else if (arg.equals("-n")) {
                if (value.equals("global")) {
                    normalization = HeatMapImageDrawer.COLOR_RESPONSE_GLOBAL;
                } else if (value.equals("row normalized")) {
                    normalization = HeatMapImageDrawer.COLOR_RESPONSE_ROW;
                }

            } else if (arg.equals("-g")) {
                showGridLines = "yes".equalsIgnoreCase(value);
            } else if (arg.equals("-l")) {
                // r:g:b triplet
                gridLinesColor = HeatMapImageDrawer.createColor(value);
            } else if (arg.equals("-a")) {
                showGeneAnnotations = "yes".equalsIgnoreCase(value);
            } else if (arg.equals("-s")) {
                showGeneNames = "yes".equalsIgnoreCase(value);
            } else if (arg.equals("-f")) {
                featureList = AnalysisUtil.readFeatureList(value);
            } else if (arg.equals("-h")) {
                highlightColor = HeatMapImageDrawer.createColor(value);
            } else if (arg.equals("-m")) {
                colorMap = HeatMapImageDrawer.parseColorMap(value);
            } else {
                AnalysisUtil.exit("unknown option " + arg);
            }
        }
        Color[] _colorMap = colorMap != null ? colorMap : RowColorScheme.getDefaultColorMap();
        try {
            DisplaySettings ds = new DisplaySettings();
            ds.columnSize = columnWidth;
            ds.rowSize = rowWidth;
            if (normalization == HeatMapImageDrawer.COLOR_RESPONSE_ROW) {
                ds.colorConverter = RowColorScheme.getRowInstance(_colorMap);
            } else {
                ds.colorConverter = RowColorScheme.getGlobalInstance(_colorMap);
            }
            ds.drawGrid = showGridLines;
            ds.drawRowNames = showGeneNames;
            ds.drawRowDescriptions = showGeneAnnotations;
            ds.gridLinesColor = gridLinesColor;

            Map featureNames2Colors = null;
            if (featureList != null) {
                featureNames2Colors = new HashMap();
                for (int i = 0; i < featureList.size(); i++) {
                    String name = (String) featureList.get(i);
                    featureNames2Colors.put(name, highlightColor);
                }

            }
            HeatMapImageDrawer.saveImage(data, ds, null, featureNames2Colors, outputFileName, outputFileFormat);
        } catch (Exception e) {
            if (e instanceof IOException || e instanceof RuntimeException) {
                AnalysisUtil.exit(e.getMessage());
            } else {
                AnalysisUtil.exit("An error occurred while saving the image.");
            }
        } catch (OutOfMemoryError ome) {
            AnalysisUtil.exit("Not enough memory available to save the image.");
        }

    }

}
