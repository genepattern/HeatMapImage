package org.genepattern.modules.heatmap;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;

import org.genepattern.annotation.DefaultAnnotation;
import org.genepattern.heatmap.HeatMap;
import org.genepattern.heatmap.HeatMapElementPanel;
import org.genepattern.heatmap.RowColorScheme;
import org.genepattern.io.DatasetParser;
import org.genepattern.io.DefaultDatasetCreator;
import org.genepattern.io.ImageUtil;
import org.genepattern.io.featureset.GrpIO;
import org.genepattern.matrix.Dataset;
import org.genepattern.matrix.FeatureSet;
import org.genepattern.module.AnalysisUtil;

public class RunHeatMapImage {

    protected Color[] colorMap = null;

    protected int columnSize = 10;

    protected Dataset data;

    protected FeatureSet featureList = null;

    protected Color featureListColor = Color.RED;

    protected Color gridLinesColor = Color.BLACK;

    protected String inputFileName;

    protected int normalization = HeatMapElementPanel.NORMALIZATION_ROW;

    protected String outputFileFormat;

    protected String outputFileName;

    protected int rowSize = 10;

    protected boolean showGridLines = true;

    protected boolean showRowDescriptions = true;

    protected boolean showRowNames = true;

    public RunHeatMapImage() {

    }

    protected void parse(String[] args) {
        String inputFileName = args[0];
        String outputFileName = args[1];
        String outputFileFormat = args[2];
        data = parseDataset(inputFileName);

        for (int i = 3; i < args.length; i++) { // 0th arg is input file name,
            // 1st arg is output file name,
            // 2nd arg is format
            String arg = args[i].substring(0, 2);
            String value = args[i].substring(2, args[i].length());
            if (value.equals("")) {
                continue;
            }

            if (arg.equals("-c")) {
                columnSize = Integer.parseInt(value);
            } else if (arg.equals("-r")) {
                rowSize = Integer.parseInt(value);
            } else if (arg.equals("-n")) {
                if (value.equals("global")) {
                    normalization = HeatMapElementPanel.NORMALIZATION_GLOBAL;
                } else if (value.equals("row normalized")) {
                    normalization = HeatMapElementPanel.NORMALIZATION_ROW;
                }

            } else if (arg.equals("-g")) {
                showGridLines = "yes".equalsIgnoreCase(value);
            } else if (arg.equals("-l")) {
                // r:g:b triplet
                gridLinesColor = createColor(value);
            } else if (arg.equals("-a")) {
                showRowDescriptions = "yes".equalsIgnoreCase(value);
            } else if (arg.equals("-s")) {
                showRowNames = "yes".equalsIgnoreCase(value);
            } else if (arg.equals("-f")) {
                FileInputStream is = null;
                try {
                    is = new FileInputStream(value);
                    FeatureSet[] s = new GrpIO().parse(is);
                    if (s.length == 1) {
                        featureList = s[0];
                    }
                } catch (IOException ioe) {
                    AnalysisUtil.exit("An error occurred while reading the file " + new File(value).getName() + ".");
                } finally {
                    if (is != null) {
                        try {
                            is.close();
                        } catch (IOException e) {

                        }
                    }
                }

            } else if (arg.equals("-h")) {
                featureListColor = createColor(value);
            } else if (arg.equals("-m")) {
                colorMap = parseColorMap(value);
            } else {
                parseArg(arg, value);
            }
        }

        HeatMap heatMap = createHeatMap();
        heatMap.setSquareAspect(false);
        heatMap.setColumnSize(columnSize);
        heatMap.setRowSize(rowSize);
        Color[] _colorMap = colorMap != null ? colorMap : RowColorScheme.getDefaultColorMap();
        if (normalization == HeatMapElementPanel.NORMALIZATION_ROW) {
            heatMap.setColorScheme(RowColorScheme.getRowInstance(_colorMap));
        } else {
            heatMap.setColorScheme(RowColorScheme.getGlobalInstance(_colorMap));
        }
        heatMap.setDrawGrid(showGridLines);
        heatMap.setGridColor(gridLinesColor);
        heatMap.setRowNamesVisible(showRowNames);
        heatMap.setRowDescriptionsVisible(showRowDescriptions);
        if (featureList != null) {
            DefaultAnnotation a = new DefaultAnnotation(featureList);
            heatMap.getRowAnnotatorModel().addAnnotation(0, a);
            heatMap.getRowAnnotatorColorModel().setColor(featureList.getName(), featureListColor);
        }
        try {
            ImageUtil.saveImage(heatMap, outputFileFormat, new File(outputFileName), true);
        } catch (OutOfMemoryError ome) {
            AnalysisUtil.exit("Not enough memory available to save the image.");
        } catch (IOException e) {
            AnalysisUtil.exit("An error occurred while saving the image.");
        }

    }

    protected HeatMap createHeatMap() {
        return new HeatMap(new JPanel(), data);
    }

    protected void parseArg(String arg, String value) {
        throw new IllegalArgumentException();
    }

    protected Dataset parseDataset(String inputFileName) {
        DatasetParser reader = AnalysisUtil.getDatasetParser(inputFileName);
        DefaultDatasetCreator c = new DefaultDatasetCreator(true);
        return (Dataset) AnalysisUtil.readDataset(reader, inputFileName, c);

    }

    public static Color createColor(String triplet) {
        String[] rgb = triplet.split(":");
        if (rgb.length != 3) {
            throw new IllegalArgumentException("Invalid rgb triplet " + triplet);
        }
        int r = 0, g = 0, b = 0;
        try {
            r = Integer.parseInt(rgb[0]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Red component is not an integer " + triplet);
        }
        try {
            g = Integer.parseInt(rgb[1]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Green component is not an integer " + triplet);
        }
        try {
            b = Integer.parseInt(rgb[2]);
        } catch (NumberFormatException nfe) {
            throw new IllegalArgumentException("Blue component is not an integer " + triplet);
        }
        return new Color(r, g, b);
    }

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
        new RunHeatMapImage().parse(args);
    }

    public static Color[] parseColorMap(String fileName) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(fileName));
            String s = null;
            List colors = new ArrayList();
            while ((s = br.readLine()) != null) {
                if (s.trim().equals("")) {
                    continue;
                }
                Color c = createColor(s);
                colors.add(c);
            }
            return (Color[]) colors.toArray(new Color[0]);
        } catch (IOException ioe) {
            return null;
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                }
            }
        }
    }

}
