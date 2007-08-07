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
import org.genepattern.heatmap.DefaultColorScheme;
import org.genepattern.io.DatasetParser;
import org.genepattern.io.ImageUtil;
import org.genepattern.io.featureset.GrpIO;
import org.genepattern.matrix.Dataset;
import org.genepattern.matrix.FeatureSet;
import org.genepattern.module.AnalysisUtil;

public class RunHeatMapImage {

    protected Color[] colorMap;

    protected int columnSize = 10;

    protected Dataset data;

    protected FeatureSet featureSet;

    protected Color featureSetColor = Color.RED;

    protected Color gridLinesColor = Color.BLACK;

    protected String inputFileName;

    protected boolean globalScale = false;

    protected String outputFileFormat;

    protected String outputFileName;

    protected int rowSize = 10;

    protected boolean showGridLines = true;

    protected boolean showRowDescriptions = true;

    protected boolean showRowNames = true;

    protected void parse(String[] args) {
	inputFileName = args[0];
	outputFileName = args[1];
	outputFileFormat = args[2];
	data = parseDataset();

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
		    globalScale = true;
		} else if (value.equals("row normalized")) {
		    globalScale = false;
		}

	    } else if (arg.equals("-g")) {
		showGridLines = "yes".equalsIgnoreCase(value);
	    } else if (arg.equals("-l")) {
		// r:g:b triplet
		gridLinesColor = ImageUtil.decodeRGBTriplet(value);
	    } else if (arg.equals("-a")) {
		showRowDescriptions = "yes".equalsIgnoreCase(value);
	    } else if (arg.equals("-s")) {
		showRowNames = "yes".equalsIgnoreCase(value);
	    } else if (arg.equals("-f")) {
		FileInputStream is = null;
		try {
		    is = new FileInputStream(value);
		    GrpIO grp = new GrpIO();
		    grp.setName(value);
		    FeatureSet[] s = grp.parse(is);
		    if (s.length == 1) {
			featureSet = s[0];
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
		featureSetColor = ImageUtil.decodeColor(value);
	    } else if (arg.equals("-m")) {
		try {
		    colorMap = parseColorMap(value);
		} catch (NumberFormatException nfe) {
		    AnalysisUtil.exit("An error occurred while parsing the color palette.");
		}
	    } else {
		parseArg(arg, value);
	    }
	}

	HeatMap heatMap = createHeatMap();
	heatMap.setSquareAspect(false);
	heatMap.setColumnSize(columnSize);
	heatMap.setRowSize(rowSize);
	Color[] _colorMap = colorMap != null ? colorMap : DefaultColorScheme.getDefaultColorMap();
	if (!globalScale) {
	    heatMap.setColorScheme(DefaultColorScheme.getRowInstance(_colorMap));
	} else {
	    heatMap.setColorScheme(DefaultColorScheme.getGlobalInstance(_colorMap));
	}
	heatMap.setDrawGrid(showGridLines);
	heatMap.setGridColor(gridLinesColor);
	heatMap.setRowNamesVisible(showRowNames);
	heatMap.setRowDescriptionsVisible(showRowDescriptions);
	if (featureSet != null) {
	    DefaultAnnotation a = new DefaultAnnotation(featureSet);
	    heatMap.getRowAnnotatorModel().addAnnotation(0, a);
	    String[] categories = a.getCategories();
	    if (categories != null) {
		for (int i = 0; i < categories.length; i++) {
		    heatMap.getRowAnnotatorColorModel().setColor(categories[i], featureSetColor);
		}
	    }

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

    protected Dataset parseDataset() {
	DatasetParser reader = AnalysisUtil.getDatasetParser(inputFileName);
	return AnalysisUtil.readDataset(reader, inputFileName, true);
    }

    /**
     * Create a heatmap image from the command line <input.filename> <output.filename> <output.format> -cw <column.size>
     * -rw <row.size> -norm <normalization> -grid <grid> -ra <show.row.descriptions> -p <show.row.ids>
     * 
     * @param args
     *                The command line arguments
     */
    public static void main(String[] args) {
	new RunHeatMapImage().parse(args);
    }

    private static Color[] parseColorMap(String fileName) {
	BufferedReader br = null;
	try {
	    br = new BufferedReader(new FileReader(fileName));
	    String s = null;
	    List<Color> colors = new ArrayList<Color>();
	    while ((s = br.readLine()) != null) {
		if (s.trim().equals("")) {
		    continue;
		}
		Color c = ImageUtil.decodeColor(s);
		colors.add(c);
	    }
	    return colors.toArray(new Color[0]);
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
