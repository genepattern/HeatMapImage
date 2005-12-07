package org.genepattern.modules.heatmap;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.genepattern.data.expr.ExpressionData;
import org.genepattern.heatmap.image.DisplaySettings;
import org.genepattern.heatmap.image.FeatureAnnotator;
import org.genepattern.heatmap.image.HeatMap;
import org.genepattern.io.expr.IExpressionDataReader;
import org.genepattern.ioutil.ImageUtil;
import org.genepattern.module.AnalysisUtil;

public class ExampleUsage {

	/**
	 * @param args
	 * @throws Exception
	 */
	public static void main(String[] args) throws Exception {
		String inputFileName = args[0];
		IExpressionDataReader reader = AnalysisUtil
				.getExpressionReader(inputFileName);

		ExpressionData data = AnalysisUtil.readExpressionData(reader,
				inputFileName);

		final Map featureName2Colors = new HashMap();
		featureName2Colors.put("M31303_rna1_at", Arrays
				.asList(new Color[] { Color.BLUE }));

		DisplaySettings ds = new DisplaySettings();

		FeatureAnnotator fa = new FeatureAnnotator() {

			public String getAnnotation(String feature, int j) {
				return "test";
			}

			public int getColumnCount() {
				return 1;
			}

			public List getColors(String featureName) {
				return (List) featureName2Colors.get(featureName);
			}

		};
		HeatMap.saveImage(data, ds, fa, null, "out1", "png");

		HeatMap.HeatMapImage result = HeatMap.createImage2(data, ds, null, fa);

		String htmlMap = HeatMap.createHtmlImageMap(data.getRowCount(), data
				.getColumnCount(), result, ds);

		ImageUtil.saveImage(result.image, "out2", "png");

		BufferedImage bi = HeatMap.createImage(data, ds, null, fa);
		ImageUtil.saveImage(bi, "out3", "png");

	}
}
