package org.genepattern.modules.heatmap;

import java.awt.Color;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.data.expr.IExpressionData;
import org.genepattern.heatmap.image.HeatMap;
import org.genepattern.io.expr.ExpressionDataCreator;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.module.AnalysisUtil;

public class ExampleUsage {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		String inputFileName = "all_aml_train.neighbors.gct";
		IExpressionDataReader reader = AnalysisUtil
				.getExpressionReader(inputFileName);

		ExpressionData data = (ExpressionData) AnalysisUtil.readExpressionData(
				reader, inputFileName, new ExpressionDataCreator());

		String outputFileName = "out";

		String outputFileFormat = "jpeg";
		int columnSize = 8;
		int rowSize = 8;

		int normalization = HeatMap.COLOR_RESPONSE_ROW;
		boolean drawGrid = true;
		Color gridLinesColor = Color.black;

		boolean drawRowNames = true;
		boolean drawRowDescriptions = false;
		List featureList = Arrays.asList(new String[] {"M31303_rna1_at", "Y08612_at", "L49229_f_at", "U20998_at", "U29175_at", "M91432_at", "X15949_at"});
		Color featureListColor = Color.YELLOW;

		try {
			HeatMap.createImage(data, outputFileName, outputFileFormat, columnSize,
					rowSize, normalization, drawGrid, gridLinesColor, drawRowNames,
					drawRowDescriptions, featureList, featureListColor);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
