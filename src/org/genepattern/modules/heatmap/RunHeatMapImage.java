package org.genepattern.modules.heatmap;

import java.awt.Color;
import java.io.IOException;

import org.genepattern.data.expr.IExpressionData;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.module.AnalysisUtil;
import org.genepattern.heatmap.image.*;

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

		IExpressionDataReader reader = AnalysisUtil
				.getExpressionReader(inputFileName);

		IExpressionData data = AnalysisUtil.readExpressionData(reader,
				inputFileName);

		int columnWidth = 10;
		int rowWidth = 10;
		int normalization = HeatMap.NORMALIZATION_ROW;
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
					normalization = HeatMap.NORMALIZATION_GLOBAL;
				} else if (value.equals("row normalized")) {
					normalization = HeatMap.NORMALIZATION_ROW;
				}

			} else if (arg.equals("-g")) {
				showGridLines = "yes".equalsIgnoreCase(value);
			} else if (arg.equals("-l")) {
				// r:g:b triplet
				gridLinesColor = HeatMap.createColor(value);
			} else if (arg.equals("-a")) {
				showGeneAnnotations = "yes".equalsIgnoreCase(value);
			} else if (arg.equals("-s")) {
				showGeneNames = "yes".equalsIgnoreCase(value);
			} else if (arg.equals("-f")) {
				featureList = AnalysisUtil.readFeatureList(value);
			} else if (arg.equals("-h")) {
				highlightColor = HeatMap.createColor(value);
			} else if (arg.equals("-m")) {
				colorMap = HeatMap.parseColorMap(value);
			} else {
				AnalysisUtil.exit("unknown option " + arg);
			}
		}
		Color[] _colorMap = colorMap != null ? colorMap : RowColorConverter
				.getDefaultColorMap();
		try {
			HeatMap.createImage(data, outputFileName, outputFileFormat,
					columnWidth, rowWidth, normalization, showGridLines,
					gridLinesColor, showGeneNames, showGeneAnnotations,
					featureList, highlightColor);
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
