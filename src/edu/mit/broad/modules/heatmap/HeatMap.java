package edu.mit.broad.modules.heatmap;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Composite;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EventObject;
import java.util.List;

import javax.media.jai.JAI;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.EventListenerList;

import org.genepattern.data.expr.IExpressionData;
import org.jibble.epsgraphics.EpsGraphics2D;

import com.sun.media.jai.codec.ImageEncodeParam;
import com.sun.media.jai.codec.JPEGEncodeParam;

/**
 * This class is used to draw a heat map.
 * 
 * @author Joshua Gould
 */
public class HeatMap {

	public static int COLOR_RESPONSE_LOG = 0;

	public static int COLOR_RESPONSE_LINEAR = 1;

	public static int NORMALIZATION_ROW = 0;

	public static int NORMALIZATION_GLOBAL = 1;

	/** The url to go to when the user double clicks the gene name */
	public final static String GENE_QUERY = "<query>";

	String[] rowDescriptions;

	IExpressionData data;

	/** max value in the data */
	double maxValue = -Double.MAX_VALUE;

	/** min value in the data */
	double minValue = Double.MAX_VALUE;

	/** width and height of one 'cell' in the heatmap */
	Dimension elementSize = new Dimension(10, 10);

	/** width of 'cells', gene names, left inset, gene class label area */
	int contentWidth = 0;

	/** height of this heatmap */
	int height = 0;

	int[] genesOrder;

	int[] samplesOrder;

	boolean showToolTipText = true;

	boolean antiAliasing = true;

	String fontFamilyName = "monospaced";

	int fontStyle = Font.PLAIN;

	static AlphaComposite SRC_OVER_COMPOSITE = AlphaComposite.getInstance(
			AlphaComposite.SRC_OVER, 0.5f);

	private HeatMapHeader header;

	private boolean drawBorders = true;

	private Color borderColor = Color.black;

	private static Color missingColor = new Color(128, 128, 128);

	private int geneNameWidth;

	private Insets insets = new Insets(0, 10, 0, 0);

	private boolean showGeneNames = true;

	private RowColorConverter rowColorConverter;

	private int normalization = NORMALIZATION_ROW;

	private static Color maskColor = Color.yellow;

	private String geneURL = "http://www.google.com/search?q=" + GENE_QUERY;

	private EventListenerList eventListeners = new EventListenerList();

	private java.text.NumberFormat numberFormat = java.text.NumberFormat
			.getInstance();

	private Font font;

	private GeneMouseListener geneMouseListener = new GeneMouseListener();

	/** Whether to show gene annoations */
	boolean showGeneAnnotations = false;

	/** The max width of the gene annotations */
	int maxGeneAnnotationsWidth = 0;

	/** The number of pixels after the gene name and before the gene annotation */
	int spaceAfterGeneNames = 0;

	/** if not null, draw a filled square to the left of the row name */
	Color[] rowColorAnnotations;

	Color[] sampleColorAnnotations;

	public HeatMap(IExpressionData data2, Color[] colorMap) {
		this(data2, null, null, colorMap);
	}

	/**
	 * Constructs an <code>HeatMap</code> with specified data, genesOrder,
	 * samples order and draw annotations attribute.
	 * 
	 * @param genesOrder
	 *            the two dimensional array with spots indices.
	 * @param samplesOrder
	 *            the one dimensional array with samples indices.
	 * @param data
	 *            Description of the Parameter
	 */
	public HeatMap(IExpressionData data, int[] genesOrder, int[] samplesOrder,
			Color[] colorMap) {
		this.data = data;
		this.rowDescriptions = new String[data.getRowCount()];
		for (int i = 0, rows = data.getRowCount(); i < rows; i++) {
			rowDescriptions[i] = data.getRowDescription(i);
			if (rowDescriptions[i] == null) {
				rowDescriptions[i] = "";
			}
		}

		this.genesOrder = genesOrder == null ? createDefaultOrdering(data
				.getRowCount()) : genesOrder;
		this.samplesOrder = samplesOrder == null ? createDefaultOrdering(data
				.getColumnCount()) : samplesOrder;
		this.header = new HeatMapHeader(this);

		rowColorConverter = new RowColorConverter(colorMap,
				COLOR_RESPONSE_LINEAR, data);
		setNormalization(NORMALIZATION_ROW);
		// ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
		// toolTipManager.registerComponent(this);
		// addMouseListener(geneMouseListener);
		// addMouseMotionListener(geneMouseListener);
	}

	static Color createColor(String triplet) {
		String[] rgb = triplet.split(":");
		if (rgb.length != 3) {
			throw new IllegalArgumentException("Invalid rgb triplet " + triplet);
		}
		int r = 0, g = 0, b = 0;
		try {
			r = Integer.parseInt(rgb[0]);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(
					"Red component is not an integer " + triplet);
		}
		try {
			g = Integer.parseInt(rgb[1]);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(
					"Green component is not an integer " + triplet);
		}
		try {
			b = Integer.parseInt(rgb[2]);
		} catch (NumberFormatException nfe) {
			throw new IllegalArgumentException(
					"Blue component is not an integer " + triplet);
		}
		return new Color(r, g, b);
	}

	static Color[] parseColorMap(String fileName) {
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

	/**
	 * 
	 * @param data
	 *            The data to draw the heatmap for
	 * @param outputFileName
	 *            The name of the output file. The correct file extension will
	 *            be added if it does not exist
	 * @param outputFileFormat
	 *            The output file format. One of jpeg, png, tiff, bmp, eps.
	 * @param columnSize
	 *            The size in pixels of an element along the x axis
	 * @param rowSize
	 *            The size in pixels of an element along the y axis
	 * @param normalization
	 *            The normalization method to use. One of
	 *            <tt>NORMALIZATION_ROW</tt> or <tt>NORMALIZATION_GLOBAL</tt>
	 * @param drawGrid
	 *            Whether to draw a grid between elements
	 * @param gridLinesColor
	 *            The grid color when <tt>drawGrid</tt> is <tt>true</tt>
	 * @param drawRowNames
	 *            Whether to draw row names
	 * @param drawRowDescriptions
	 *            Whether to draw row annotations
	 * @param featureList
	 *            List of row names to highlight in the heat map or
	 *            <tt>null</tt>
	 * @param featureListColor
	 *            The color to highlight the features in the feature list when
	 *            <tt>featureList</tt> is not <tt>null</tt>
	 * @throws IllegalArgumentException
	 * @throws IOException
	 */
	public static void createImage(IExpressionData data, String outputFileName,
			String outputFileFormat, int columnSize, int rowSize,
			int normalization, boolean drawGrid, Color gridLinesColor,
			boolean drawRowNames, boolean drawRowDescriptions,
			List featureList, Color featureListColor) throws IOException {
		if (featureList != null) {
			createImage(data, outputFileName, outputFileFormat, columnSize,
					rowSize, normalization, drawGrid, gridLinesColor,
					drawRowNames, drawRowDescriptions,
					new List[] { featureList },
					new Color[] { featureListColor }, null, null);
		} else {
			createImage(data, outputFileName, outputFileFormat, columnSize,
					rowSize, normalization, drawGrid, gridLinesColor,
					drawRowNames, drawRowDescriptions, null, null, null, null);
		}
	}

	public static void createImage(IExpressionData data, String outputFileName,
			String outputFileFormat, int columnSize, int rowSize,
			int normalization, boolean drawGrid, Color gridLinesColor,
			boolean drawRowNames, boolean drawRowDescriptions,
			List[] featureLists, Color[] featureListColors, List[] sampleLists,
			Color[] sampleListColors) throws IOException {
		HeatMap heatMap = new HeatMap(data, RowColorConverter
				.getDefaultColorMap());

		if (featureLists != null) {
			heatMap.rowColorAnnotations = new Color[data.getRowCount()];
			for (int j = 0; j < featureLists.length; j++) {
				List featureList = featureLists[j];
				Color featureListColor = featureListColors[j];
				for (int i = 0; i < featureList.size(); i++) {
					String feature = (String) featureList.get(i);
					int index = data.getRowIndex(feature);
					if (index < 0) {
						System.out.println(feature
								+ " not found in feature list.");
					} else {
						heatMap.rowColorAnnotations[index] = featureListColor;
					}
				}
			}
		}

		if (sampleLists != null) {
			heatMap.sampleColorAnnotations = new Color[data.getColumnCount()];
			for (int j = 0; j < sampleLists.length; j++) {
				List sampleList = sampleLists[j];
				Color sampleListColor = sampleListColors[j];
				for (int i = 0; i < sampleList.size(); i++) {
					String sample = (String) sampleList.get(i);
					int index = data.getColumnIndex(sample);
					if (index < 0) {
						System.out.println(sample
								+ " not found in sample list.");
					} else {
						heatMap.sampleColorAnnotations[index] = sampleListColor;
					}
				}
			}
		}

		heatMap.showGeneAnnotations = drawRowDescriptions;
		heatMap.showGeneNames = drawRowNames;
		heatMap.setShowGridLines(drawGrid);
		heatMap.setGridLinesColor(gridLinesColor);

		heatMap.setElementSize(rowSize, columnSize);

		if (normalization == NORMALIZATION_GLOBAL) { // default is row
			heatMap.setNormalization(HeatMap.NORMALIZATION_GLOBAL);
		}
		if (outputFileFormat.equals("jpeg")) {
			if (!outputFileName.toLowerCase().endsWith(".jpg")
					&& !outputFileName.toLowerCase().endsWith(".jpeg")) {
				outputFileName += ".jpg";
			}
		} else if (outputFileFormat.equals("png")) {
			if (!outputFileName.toLowerCase().endsWith(".png")) {
				outputFileName += ".png";
			}
		} else if (outputFileFormat.equals("tiff")) {
			if (!outputFileName.toLowerCase().endsWith(".tiff")) {
				outputFileName += ".tiff";
			}
		} else if (outputFileFormat.equals("bmp")) {
			if (!outputFileName.toLowerCase().endsWith(".bmp")) {
				outputFileName += ".bmp";
			}
		} else if (outputFileFormat.equals("eps")) {
			if (!outputFileName.toLowerCase().endsWith(".eps")
					&& !outputFileName.toLowerCase().endsWith(".ps")) {
				outputFileName += ".eps";
			}
		}

		Graphics2D epsGraphics = null;
		if (outputFileFormat.equals("eps")) {
			EpsGraphics2D temp = new EpsGraphics2D();
			heatMap.updateSize(temp);
			heatMap.header.updateSize(heatMap.contentWidth,
					heatMap.elementSize.width, temp);
			temp.dispose();
			epsGraphics = new EpsGraphics2D();
			// epsGraphics = new EpsGraphics2D("", new BufferedOutputStream(
			// new FileOutputStream(outputFileName)), 0, 0,
			// heatMap.contentWidth, heatMap.height
			// + heatMap.header.height);
			// epsGraphics.scale(0.24, 0.24); // Set resolution to 300 dpi (0.24
			// = 72/300)
		} else {
			BufferedImage bi = new BufferedImage(100, 100,
					BufferedImage.TYPE_3BYTE_BGR);
			Graphics2D g2 = bi.createGraphics();
			heatMap.updateSize(g2);
			heatMap.header.updateSize(heatMap.contentWidth,
					heatMap.elementSize.width, g2);
			g2.dispose();
		}

		if (!outputFileFormat.equals("eps")) {

			ImageEncodeParam fParam = null;
			if (outputFileFormat.equals("jpeg")) {
				JPEGEncodeParam jpegParam = new JPEGEncodeParam();
				jpegParam.setQuality(1.0f);
				fParam = jpegParam;
			} else if (outputFileFormat.equals("png")) {
				fParam = new com.sun.media.jai.codec.PNGEncodeParam.RGB();
			} else if (outputFileFormat.equals("tiff")) {
				com.sun.media.jai.codec.TIFFEncodeParam param = new com.sun.media.jai.codec.TIFFEncodeParam();
				param
						.setCompression(com.sun.media.jai.codec.TIFFEncodeParam.COMPRESSION_NONE);
				fParam = param;
			} else if (outputFileFormat.equals("bmp")) {
				fParam = new com.sun.media.jai.codec.BMPEncodeParam();
			} else {
				throw new IllegalArgumentException("Unknown output file format");
			}
			// ImageIO.write(heatMap.snapshot(), "jpeg", new
			// File(outputFileName));
			JAI.create("filestore", heatMap.snapshot(), outputFileName,
					outputFileFormat, fParam);

		} else {
			epsGraphics.setFont(heatMap.header.font);
			heatMap.header.draw(epsGraphics);
			epsGraphics.translate(0, heatMap.header.height);
			epsGraphics.setFont(heatMap.font);
			heatMap.draw(epsGraphics);
			// ((EpsGraphics2D) epsGraphics).flush();
			// ((EpsGraphics2D) epsGraphics).close();

			String output = epsGraphics.toString();
			try {
				java.io.PrintWriter pw = new java.io.PrintWriter(
						new java.io.FileWriter(outputFileName));
				pw.print(output);
				pw.close();
			} catch (IOException ioe) {
				throw new IOException(
						"An error occurred while saving the postscript file.\nCause: "
								+ ioe.getMessage());
			}
			epsGraphics.dispose();

		}
	}

	/**
	 * Adds a listener to be notified when the element size is changed.
	 * 
	 * @param l
	 *            The feature to be added to the ElementSizeChangedListener
	 *            attribute
	 */
	public void addElementSizeChangedListener(ElementSizeChangedListener l) {
		eventListeners.add(ElementSizeChangedListener.class, l);
	}

	/**
	 * Paint component into specified graphics.
	 * 
	 * @param g
	 *            Description of the Parameter
	 */
	// public void paintComponent(Graphics g) {
	// super.paintComponent(g);
	// draw((Graphics2D) g);
	// }
	private String getRowDescription(int row) {
		return rowDescriptions[row];
	}

	void draw(Graphics2D g) {
		final int samples = data.getColumnCount();
		Rectangle bounds = g.getClipBounds();
		int left = 0;
		int right = samples;
		int top = 0;
		int bottom = data.getRowCount();

		if (bounds != null) {
			top = getTopIndex(bounds.y);
			bottom = getBottomIndex(bounds.y + bounds.height, data
					.getRowCount());
			left = getLeftIndex(bounds.x);
			right = getRightIndex(bounds.x + bounds.width, samples);
		}
		Graphics2D g2 = (Graphics2D) g;

		// draw rectangles
		for (int row = top; row < bottom; row++) {
			for (int column = left; column < right; column++) {
				fillRectAt(g2, row, column);
			}
		}
		Color initColor = g.getColor();

		int expWidth = samples * this.elementSize.width + 5;

		if (rowColorAnnotations != null) { // geneClassVector.hasNonDefaultLabels())
			// { // draw colors beside the gene
			// names
			for (int row = top; row < bottom; row++) {
				drawGeneLabel(g, row, expWidth);
			}
		}
		if (showGeneNames
				&& geneMouseListener.topSelectedGeneIndex >= 0
				&& geneMouseListener.bottomSelectedGeneIndex <= data
						.getRowCount()) {
			g2.setColor(Color.yellow);
			Composite oldComposite = g2.getComposite();
			g2.setComposite(HeatMap.SRC_OVER_COMPOSITE);
			int uniqX = elementSize.width * samples + 10;
			if (rowColorAnnotations != null) { // geneClassVector.hasNonDefaultLabels())
				// {
				uniqX += this.elementSize.width;
			}
			int topSel = (geneMouseListener.topSelectedGeneIndex)
					* elementSize.height;
			int bottomSel = (geneMouseListener.bottomSelectedGeneIndex)
					* elementSize.height;
			int leftSel = uniqX + insets.left;
			g2.fillRect(leftSel, topSel, geneNameWidth, bottomSel - topSel);
			g2.setColor(Color.black);
			g2.setComposite(oldComposite);
		}
		// draw gene ids
		if (this.showGeneNames || this.showGeneAnnotations) {
			if (this.antiAliasing) {
				((Graphics2D) g).setRenderingHint(
						RenderingHints.KEY_ANTIALIASING,
						RenderingHints.VALUE_ANTIALIAS_OFF);
				((Graphics2D) g).setRenderingHint(
						RenderingHints.KEY_TEXT_ANTIALIASING,
						RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			}
			if (right >= samples) {

				g.setColor(Color.black);
				int uniqX = elementSize.width * samples + 10;

				if (rowColorAnnotations != null) { // geneClassVector.hasNonDefaultLabels())
					// {
					uniqX += this.elementSize.width;
				}
				FontMetrics fm = g.getFontMetrics();
				int descent = fm.getDescent();

				for (int row = top; row < bottom; row++) {
					int annY = (row + 1) * elementSize.height;
					// int annY = (row ) * elementSize.height;

					if (this.showGeneNames) {
						String label = data.getRowName(row);
						g
								.drawString(label, uniqX + insets.left, annY
										- descent);
						// g.drawString(label, uniqX + insets.left, annY +
						// fm.getAscent());
					}
					if (showGeneAnnotations) {
						int geneAnnotationX = uniqX + insets.left
								+ geneNameWidth;
						if (showGeneNames) {
							geneAnnotationX += spaceAfterGeneNames;
						}
						String annot = (String) getRowDescription(row);
						if (annot != null) {
							g
									.drawString(annot, geneAnnotationX, annY
											- descent);
						}
					}
				}
			}
		}

		if (drawBorders) {
			g.setColor(borderColor);
			int leftx = left * elementSize.width + insets.left;
			int rightx = right * elementSize.width + insets.left; // increase
			// if
			// drawing
			// border to
			// row name
			for (int row = top; row <= bottom; row++) {
				int y = row * elementSize.height;
				g.drawLine(leftx, y, rightx, y);
			}

			int y = 0; // if drawing border to column name need to change
			// header
			int bottomy = bottom * elementSize.height;
			for (int column = left; column <= right; column++) {
				int x = column * elementSize.width + insets.left;
				g.drawLine(x, y, x, bottomy);
			}
		}
	}

	public void resetSamplesOrder() {
		samplesOrder = createDefaultOrdering(data.getColumnCount());
	}

	public void resetGenesOrder() {
		genesOrder = createDefaultOrdering(data.getRowCount());
	}

	public void sortGenesByColor() {
		// sortByClass(geneClassVector, genesOrder);
		// FIXME
	}

	public BufferedImage snapshot() {
		int headerHeight = header.height;
		// BufferedImage bi = new BufferedImage(contentWidth, height +
		// headerHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage bi = new BufferedImage(contentWidth, height
				+ headerHeight, BufferedImage.TYPE_3BYTE_BGR);

		Graphics2D g2 = bi.createGraphics();
		g2.setColor(Color.white);
		g2.fillRect(0, 0, bi.getWidth(), bi.getHeight());
		g2.setColor(Color.black);
		g2.setFont(header.font);
		header.draw(g2);
		g2.translate(0, headerHeight);
		g2.setFont(this.font);
		this.draw(g2);
		g2.dispose();
		return bi;
	}

	/** Updates the size of this heatmap panel and the header */
	// public void updateSize() {
	// Graphics2D g = (Graphics2D) getGraphics();
	// if (g != null) {
	// updateSize(g);
	// g.dispose();
	// }
	// header.updateSize(contentWidth, elementSize.width);
	// }
	static int[] createDefaultOrdering(int size) {
		int[] order = new int[size];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		return order;
	}

	/**
	 * Updates the size of this heatmap
	 * 
	 * @param g
	 *            Description of the Parameter
	 */
	void updateSize(Graphics2D g) {
		int fontHeight = Math.min(14, elementSize.height);
		font = new Font(fontFamilyName, fontStyle, fontHeight);
		g.setFont(font);
		// setFont(font);
		int width = elementSize.width * data.getColumnCount() + 1 + insets.left;
		if (showGeneNames) {
			this.geneNameWidth = getMaxGeneNamesWidth(g);
			width += 20 + this.geneNameWidth;
		}
		if (g != null) {
			FontMetrics fm = g.getFontMetrics();
			spaceAfterGeneNames = fm.stringWidth("xxxx");
		}

		if (showGeneAnnotations) {
			this.maxGeneAnnotationsWidth = getMaxGeneDescriptionsWidth(g);
			if (showGeneNames) {
				width += spaceAfterGeneNames;
			}
			width += this.maxGeneAnnotationsWidth;
		}

		if (rowColorAnnotations != null) {
			width += this.elementSize.width + 10;
		}

		this.contentWidth = width;
		this.height = elementSize.height * data.getRowCount() + 1;
		// setSize(width, height);
		// setPreferredSize(new Dimension(width, height));
	}

	/**
	 * Fills rect with specified row and colunn. Used to draw one 'cell' in the
	 * heatmap.
	 * 
	 * @param g
	 *            Description of the Parameter
	 * @param row
	 *            Description of the Parameter
	 * @param column
	 *            Description of the Parameter
	 */
	void fillRectAt(Graphics2D g, int row, int column) {
		int x = column * elementSize.width + insets.left;
		int y = row * elementSize.height;

		// boolean selected = (this.firstSelectedRow >= 0 &&
		// this.lastSelectedRow >= 0) && (row >= this.firstSelectedRow && row <=
		// this.lastSelectedRow);

		// selected = (selected || this.firstSelectedColumn >= 0 &&
		// this.lastSelectedColumn >= 0 && (column >= this.firstSelectedColumn
		// && column <= this.lastSelectedColumn));

		g.setColor(rowColorConverter.getColor(getRow(row), getColumn(column)));
		g.fillRect(x, y, elementSize.width, elementSize.height);
		/*
		 * if(selected) { g.setColor(maskColor); Composite oldComposite =
		 * g.getComposite(); g.setComposite(SRC_OVER_COMPOSITE); g.fillRect(x,
		 * y, elementSize.width, elementSize.height);
		 * g.setComposite(oldComposite); }
		 */
		/*
		 * if(this.drawBorders) { g.setColor(borderColor); g.drawRect(x, y,
		 * elementSize.width - 1, elementSize.height - 1); }
		 */
	}

	void drawGeneLabel(Graphics g, int row, int xLoc) {
		/*
		 * if(classVectors.isEmpty()) { return; } ClassVector cv = (ClassVector)
		 * classVectors.get(0); int[] levels = cv.getLevels();
		 */
		Color c = rowColorAnnotations[genesOrder[row]];
		if (c == null) {
			return;
		}
		g.setColor(c);
		g.fillRect(xLoc + insets.left, row * elementSize.height,
				elementSize.width - 1, elementSize.height);
	}

	/**
	 * Draws rect with specified row, column and color.
	 * 
	 * @param g
	 *            Description of the Parameter
	 * @param row
	 *            Description of the Parameter
	 * @param column
	 *            Description of the Parameter
	 * @param color
	 *            Description of the Parameter
	 */
	void drawRectAt(Graphics g, int row, int column, Color color) {
		g.setColor(color);
		g.drawRect(column * elementSize.width + insets.left, row
				* elementSize.height, elementSize.width - 1,
				elementSize.height - 1);
	}

	/**
	 * Finds column for specified x coordinate.
	 * 
	 * @param targetx
	 *            Description of the Parameter
	 * @return -1 if column was not found.
	 */
	int findColumn(int targetx) {
		int xSize = data.getColumnCount() * elementSize.width;
		if (targetx < insets.left) {
			return -1;
		}
		/*
		 * if(targetx >= (xSize + insets.left) || targetx < insets.left) {
		 * return -1; }
		 */
		return (targetx - insets.left) / elementSize.width;
	}

	/**
	 * Finds row for specified y coordinate.
	 * 
	 * @param targety
	 *            Description of the Parameter
	 * @return -1 if row was not found.
	 */
	int findRow(int targety) {
		int ySize = data.getRowCount() * elementSize.height;
		// if(targety >= ySize || targety < 0) {
		// return -1;
		// }

		if (targety < 0) {
			return -1;
		}

		return targety / elementSize.height;
	}

	private void updateSizeAndRepaint() {
		// updateSize();
		// repaint();
		// header.repaint();
	}

	private void sortByClass(MyClassVector cv, int[] order) {
		int index = 0;
		for (int i = 0, length = cv.getClassCount(); i < length; i++) {
			int[] indices = cv.getIndices(i);
			for (int j = 0; j < indices.length; j++) {
				order[index++] = indices[j];
			}
		}
	}

	private void notifyElementSizeChangedListeners(ElementSizeChangedEvent e) {
		Object[] listeners = eventListeners.getListenerList();
		for (int i = listeners.length - 2; i >= 0; i -= 2) {
			if (listeners[i] == ElementSizeChangedListener.class) {
				((ElementSizeChangedListener) listeners[i + 1])
						.elementSizeChanged(e);
			}
		}
	}

	public void setNormalization(int _normalization) {
		if (_normalization != NORMALIZATION_ROW
				&& _normalization != NORMALIZATION_GLOBAL) {
			throw new IllegalArgumentException("Unknown normalization");
		}
		if (_normalization == NORMALIZATION_ROW) {
			rowColorConverter.setGlobalScale(false);
		} else {
			rowColorConverter.setGlobalScale(true);
		}
		this.normalization = _normalization;
		this.header.setDrawColorBar(false);
	}

	/**
	 * Sets the color respose to use when normalizing by row. Choices are
	 * COLOR_RESPONSE_LOG and COLOR_RESPONSE_LINEAR
	 * 
	 * @param respose
	 *            The new rowColorResponse value
	 */
	public void setRowColorResponse(int respose) {
		rowColorConverter.setColorResponse(respose);
	}

	public void setShowSampleNames(boolean b) {
		header.setShowSampleNames(b);
		updateSizeAndRepaint();
	}

	public void setShowGeneNames(boolean b) {
		showGeneNames = b;
		updateSizeAndRepaint();
	}

	public void setGeneURL(String s) {
		geneURL = s;
	}

	public void setShowToolTipText(boolean b) {
		showToolTipText = b;
	}

	public void setElementSize(int width, int height) {
		elementSize.width = width;
		elementSize.height = height;
		// updateSize();
		notifyElementSizeChangedListeners(new ElementSizeChangedEvent(this,
				width, height));
	}

	public void setShowGridLines(boolean b) {
		drawBorders = b;
	}

	public void setGridLinesColor(Color c) {
		if (c == null) {
			throw new IllegalArgumentException(
					"Grid lines color can not be null.");
		}
		borderColor = c;
	}

	/**
	 * Sets the left margin for the viewer
	 * 
	 * @param leftMargin
	 *            The new leftInset value
	 */
	void setLeftInset(int leftMargin) {
		insets.left = leftMargin;
		this.header.setLeftInset(leftMargin);
	}

	public HeatMapHeader getHeader() {
		return header;
	}

	public String getGeneURL() {
		return geneURL;
	}

	/**
	 * Returns the index of the first selected row, -1 if no row is selected.
	 * 
	 * @return the index of the first selected row
	 */
	public int getSelectedRow() {
		return geneMouseListener.topSelectedGeneIndex;
	}

	/**
	 * Returns the number of selected rows.
	 * 
	 * @return the number of selected rows, 0 if no rows are selected
	 */
	public int getSelectedRowCount() {
		return geneMouseListener.bottomSelectedGeneIndex
				- geneMouseListener.topSelectedGeneIndex;
	}

	public boolean isShowingSampleNames() {
		return header.isShowingSampleNames();
	}

	/**
	 * Returns the index of the first selected column, -1 if no column is
	 * selected.
	 * 
	 * @return the index of the first selected column
	 */
	public int getSelectedColumn() {
		return header.getSelectedColumn();
	}

	/**
	 * Returns the number of selected columns.
	 * 
	 * @return the number of selected columns, 0 if no columns are selected
	 */
	public int getSelectedColumnCount() {
		return header.getSelectedColumnCount();
	}

	public String getToolTipText(MouseEvent event) {
		if (!showToolTipText) {
			return null;
		}
		int column = findColumn(event.getX());
		int row = findRow(event.getY());
		if (isLegalPosition(row, column)) {
			return "Value: " + numberFormat.format(data.getValue(row, column));
		}
		return null;
	}

	public boolean isShowingToolTipText() {
		return showToolTipText;
	}

	public boolean isShowingGeneNames() {
		return showGeneNames;
	}

	public int[] getGenesOrder() {
		return genesOrder;
	}

	public int[] getSamplesOrder() {
		return samplesOrder;
	}

	public Dimension getElementSize() {
		return elementSize;
	}

	/**
	 * Returns the row index in the data corresponding to the passed index to
	 * the genesOrder array
	 * 
	 * @param row
	 *            Description of the Parameter
	 * @return The datasetRow value
	 */
	int getRow(int row) {
		return this.genesOrder[row];
	}

	int getColumn(int column) {
		return samplesOrder[column];
	}

	int getMaxGeneNamesWidth(Graphics2D g) {
		return getMaxWidth(g, true);
	}

	int getMaxGeneDescriptionsWidth(Graphics2D g) {
		return getMaxWidth(g, false);
	}

	/**
	 * Returns max width of annotation strings.
	 * 
	 * @param g
	 *            Description of the Parameter
	 * @return The maxWidth value
	 */
	int getMaxWidth(Graphics2D g, boolean geneNames) {
		if (g == null) {
			return 0;
		}
		if (antiAliasing) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
					RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
					RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		FontMetrics fm = g.getFontMetrics();
		int max = 0;
		String str;
		for (int i = 0; i < data.getRowCount(); i++) {
			if (geneNames) {
				str = data.getRowName(i);
			} else {
				str = (String) getRowDescription(i);
			}
			if (str != null) {
				max = Math.max(max, fm.stringWidth(str));
			}
		}
		return max;
	}

	int getTopIndex(int top) {
		if (top < 0) {
			return 0;
		}
		return top / elementSize.height;
	}

	int getLeftIndex(int left) {
		if (left < insets.left) {
			return 0;
		}
		return (left - insets.left) / elementSize.width;
	}

	int getRightIndex(int right, int limit) {
		if (right < 0) {
			return 0;
		}
		int result = right / elementSize.width + 1;
		return result > limit ? limit : result;
	}

	int getBottomIndex(int bottom, int limit) {
		if (bottom < 0) {
			return 0;
		}
		int result = bottom / elementSize.height + 1;
		return result > limit ? limit : result;
	}

	boolean isLegalPosition(int row, int column) {
		return (isLegalRow(row) && isLegalColumn(column));
	}

	boolean isLegalColumn(int column) {
		if (column < 0 || column > data.getColumnCount() - 1) {
			return false;
		}
		return true;
	}

	boolean isLegalRow(int row) {
		if (row < 0 || row >= data.getRowCount()) {
			return false;
		}
		return true;
	}

	class GeneMouseListener extends MouseAdapter implements MouseMotionListener {
		int bottomSelectedGeneIndex = -1;

		int topSelectedGeneIndex = -1;

		int lastYIndex = -1;

		public void mouseMoved(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			// start of selection

			bottomSelectedGeneIndex = findRow(e.getY());
			System.out.println("bottomSelectedGeneIndex "
					+ bottomSelectedGeneIndex);
			// if click on 0th cell, top = 1 and bottom = 0
			bottomSelectedGeneIndex++;
			topSelectedGeneIndex = bottomSelectedGeneIndex - 1;
			lastYIndex = bottomSelectedGeneIndex;

			int uniqX = elementSize.width * data.getColumnCount() + 10;
			if (rowColorAnnotations != null) {
				uniqX += elementSize.width;
			}
			int leftSel = uniqX + insets.left;
			if (e.getX() > leftSel + geneNameWidth || e.getX() < leftSel) {
				bottomSelectedGeneIndex = -1;
				topSelectedGeneIndex = -1;
			}

			// repaint();
		}

		public void mouseDragged(MouseEvent e) {
			updateMouse(e);
			Rectangle r = null;
			if (e.getY() <= 0) {
				r = new Rectangle(e.getX(), e.getY(), elementSize.height,
						elementSize.height);
				// this rectangle becomes visible

			} else {
				r = new Rectangle(e.getX(), e.getY(), elementSize.height,
						elementSize.height);
				// this rectangle becomes visible
				((JPanel) e.getSource()).scrollRectToVisible(r);
			}
			// repaint();
		}

		private void updateMouse(MouseEvent e) {

			int index = findRow(e.getY());
			// System.out.println("index " + index);
			if (index < 0) {
				index = 0;
			}
			if (index > data.getRowCount()) {
				index = data.getRowCount();
			}
			// when moving up, update top index if click > top, else update
			// bottom index

			if (index > lastYIndex) {
				if (lastYIndex < bottomSelectedGeneIndex
						&& index >= bottomSelectedGeneIndex) {
					// crossover
					topSelectedGeneIndex = bottomSelectedGeneIndex - 1;
					bottomSelectedGeneIndex = index;
				} else if (index >= bottomSelectedGeneIndex) {

					bottomSelectedGeneIndex = index;
				} else {

					topSelectedGeneIndex = index;
				}
			} // when moving down, update bottom index if click < bottom, else
			// update top index
			else {

				if (lastYIndex > topSelectedGeneIndex
						&& index <= topSelectedGeneIndex) {
					// crossover
					bottomSelectedGeneIndex = topSelectedGeneIndex + 1;
					topSelectedGeneIndex = index;
				} else if (index <= topSelectedGeneIndex) {
					topSelectedGeneIndex = index;

				} else {
					bottomSelectedGeneIndex = index;

				}
			}

			lastYIndex = index;
		}

	}

	/**
	 * The class to listen to mouse events.
	 * 
	 * @author jgould
	 * @created March 10, 2004
	 */
	class Listener extends MouseAdapter implements MouseMotionListener {

		String oldStatusText;

		int oldRow = -1;

		int oldColumn = -1;

		public void mouseClicked(MouseEvent event) {
			if (SwingUtilities.isRightMouseButton(event)) {
				return;
			}
			int column = findColumn(event.getX());
			int row = findRow(event.getY());

			int clickCount = event.getClickCount();
			if (clickCount >= 2 && column >= data.getColumnCount()
					&& isLegalRow(row)) { // double click on gene name
				String geneName = data.getRowName(row);
				String url = geneURL.replaceAll(GENE_QUERY, geneName);
				try {
					edu.mit.broad.modules.heatmap.util.BrowserLauncher
							.openURL(url);
				} catch (java.io.IOException ioe) {
					ioe.printStackTrace();
				}
				return;
			}

			if (!isLegalPosition(row, column)) {
				return;
			}
			// TODO make plot of genes

		}

		public void mouseMoved(MouseEvent event) {
			if (data.getColumnCount() == 0 || event.isShiftDown()) {
				return;
			}
			int column = findColumn(event.getX());
			int row = findRow(event.getY());
			if (isCurrentPosition(row, column)) {
				return;
			}
			Graphics g = null;
			if (isLegalPosition(row, column)) {
				// g = getGraphics(); // FIXME
				drawRectAt(g, row, column, Color.white); // draw border
				// framework.setStatusText("Gene: " +
				// data.getUniqueId(getMultipleArrayDataRow(row)) + " Sample: "
				// +
				// data.getSampleName(data.getSampleIndex(getColumn(column)))
				// + " Value: " + data.get(getRow(row), getColumn(column)));
			} else {
				; // framework.setStatusText(oldStatusText);
			}
			if (isLegalPosition(oldRow, oldColumn)) {
				// g = g != null ? g : getGraphics(); // FIXME
				fillRectAt((Graphics2D) g, oldRow, oldColumn);
			}
			setOldPosition(row, column);
			if (g != null) {
				g.dispose();
			}
		}

		public void mouseEntered(MouseEvent event) {
			// oldStatusText = framework.getStatusText();
		}

		public void mouseExited(MouseEvent event) {
			if (isLegalPosition(oldRow, oldColumn)) {
				Graphics g = null; // getGraphics(); // FIXME
				fillRectAt((Graphics2D) g, oldRow, oldColumn);
				g.dispose();
			}
			setOldPosition(-1, -1);
			// framework.setStatusText(oldStatusText);
		}

		public void mouseDragged(MouseEvent event) {
		}

		void setOldPosition(int row, int column) {
			oldColumn = column;
			oldRow = row;
		}

		boolean isCurrentPosition(int row, int column) {
			return (row == oldRow && column == oldColumn);
		}
	}

	private class SampleClassVectorListener implements ClassVectorListener {
		public void classVectorChanged(EventObject e) {

		}
	}

	private class GeneClassVectorListener implements ClassVectorListener {
		public void classVectorChanged(EventObject e) {

		}
	}

}
