package edu.mit.broad.modules.heatmap;
import java.awt.*;

import java.awt.event.*;
import java.awt.image.*;
import java.util.*;

import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.*;
import edu.mit.broad.dataobj.*;
import edu.mit.broad.dataobj.microarray.*;
import edu.mit.broad.gp.*;
import edu.mit.broad.io.*;
import edu.mit.broad.io.microarray.*;

import com.sun.media.jai.codec.JPEGEncodeParam;
import com.sun.media.jai.codec.ImageEncodeParam;
import javax.media.jai.JAI;

/**
 *  This class is used to draw a heat map.
 *
 *@author     Aleksey D.Rezantsev
 *@author     Joshua Gould
 *@created    March 10, 2004
 *@version    1.0
 */
public class HeatMap extends JPanel {

	public static int COLOR_RESPONSE_LOG = 0;
	public static int COLOR_RESPONSE_LINEAR = 1;

	public static int NORMALIZATION_ROW = 0;
	public static int NORMALIZATION_NONE = 1;

	/**  The url to go to when the user double clicks the gene name */
	public final static String GENE_QUERY = "<query>";
	Dataset dataset;
	ExpressionData data;
	MyClassVector geneClassVector;
	MyClassVector sampleClassVector;
	/**  max value in the dataset */
	double maxValue;
	/**  min value in the dataset */
	double minValue;

	/**  width and height of one 'cell' in the heatmap */
	Dimension elementSize = new Dimension(10, 10);

	/**  max color for absolute color scheme */
	Color maxColor = Color.red;
	/**  min color for absolute color scheme */
	Color minColor = Color.green;

	/**  width of 'cells', gene names, left inset, gene class label area */
	int contentWidth = 0;
	/**  height of this heatmap */
	int height = 0;
	int[] genesOrder;
	int[] samplesOrder;
	boolean showToolTipText = true;
	boolean antiAliasing = true;

	static AlphaComposite SRC_OVER_COMPOSITE = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f);

	/**  neutral color for absolute color scheme */
	private Color neutralColor = Color.black;

	private HeatMapHeader header;

	private boolean drawBorders = false;

	private static Color missingColor = new Color(128, 128, 128);
	private int geneNameWidth;
	private Insets insets = new Insets(0, 10, 0, 0);

	private boolean showGeneNames = true;
	private RowColorConverter rowColorConverter;
	private int normalization = NORMALIZATION_ROW;
	private static Color maskColor = Color.yellow;
	private String geneURL = "http://www.google.com/search?q=" + GENE_QUERY;

	private EventListenerList eventListeners = new EventListenerList();
	private java.text.NumberFormat numberFormat = java.text.NumberFormat.getInstance();
	private Font font;

	private GeneMouseListener geneMouseListener = new GeneMouseListener();
	BufferedImage posColorImage = createGradientImage(neutralColor, maxColor);
	BufferedImage negColorImage = createGradientImage(minColor, neutralColor);


	public HeatMap(ExpressionData data) {
		this(data, null, null);
	}


	/**
	 *  Constructs an <code>HeatMap</code> with specified dataset, genesOrder,
	 *  samples order and draw annotations attribute.
	 *
	 *@param  genesOrder    the two dimensional array with spots indices.
	 *@param  samplesOrder  the one dimensional array with samples indices.
	 *@param  data          Description of the Parameter
	 */
	public HeatMap(ExpressionData data, int[] genesOrder, int[] samplesOrder) {
		this.data = data;
		this.dataset = (Dataset) data.getExpressionMatrix();
		this.geneClassVector = new MyClassVector(dataset.getRowDimension());
		geneClassVector.addClassVectorListener(new GeneClassVectorListener());
		geneClassVector.setColor(geneClassVector.getClassName(0), Color.black);
		this.sampleClassVector = new MyClassVector(dataset.getColumnDimension());
		sampleClassVector.setColor(sampleClassVector.getClassName(0), Color.black);
		this.genesOrder = genesOrder == null ? createDefaultOrdering(dataset.getRowDimension()) : genesOrder;
		this.samplesOrder = samplesOrder == null ? createDefaultOrdering(dataset.getColumnDimension()) : samplesOrder;
		this.header = new HeatMapHeader(this);
		sampleClassVector.addClassVectorListener(header);
		setNormalization(NORMALIZATION_ROW);
		setBackground(Color.white);
		Listener listener = new Listener();
		addMouseListener(listener);
		addMouseMotionListener(listener);
		maxValue = -Double.MAX_VALUE;
		minValue = Double.MAX_VALUE;
		for(int i = 0, rows = dataset.getRowDimension(); i < rows; i++) {
			for(int j = 0, columns = dataset.getColumnDimension(); j < columns; j++) {
				double d = dataset.get(i, j);
				maxValue = d > maxValue ? d : maxValue;
				minValue = d < minValue ? d : minValue;
			}
		}

		rowColorConverter = new RowColorConverter(COLOR_RESPONSE_LINEAR, dataset);
		ToolTipManager toolTipManager = ToolTipManager.sharedInstance();
		toolTipManager.registerComponent(this);
		addMouseListener(geneMouseListener);
		addMouseMotionListener(geneMouseListener);
	}


	static void exit(String s) {
		System.err.println(s);
		System.exit(1);
	}


	/**
	 *  Create a heatmap image from the command line <input.filename>
	 *  <output.filename> <output.format> -cw <column.size> -rw <row.size> -norm
	 *  <normalization>
	 *
	 *@param  args  The command line arguments
	 */
	public static void main(String[] args) {
		String inputFileName = args[0];
		String outputFileName = args[1];
		String outputFileFormat = args[2];

		ExpressionDataReader reader = GPUtil.getExpressionReader(inputFileName);
		
		ExpressionData data = GPUtil.read(reader, inputFileName);
		
		HeatMap heatMap = new HeatMap(data);

		int columnWidth = 10;
		int rowWidth = 10;
		String normalization = "row";
		for(int i = 3; i < args.length; i++) { // 0th arg is input file name, 1st arg is output file name, 2nd arg is format
			if(args[i].equals("-cw")) {
				columnWidth = Integer.parseInt(args[++i]);
			} else if(args[i].equals("-rw")) {
				rowWidth = Integer.parseInt(args[++i]);
			} else if(args[i].equals("-norm")) {
				normalization = args[++i];
				if(!normalization.equals("none") && !normalization.equals("row")) {
					exit("Invalid normalization");
				}
			} else {
				exit("unknown option " + args[i]);
			}
		}

		heatMap.setElementSize(rowWidth, columnWidth);

		if(normalization.equals("none")) { // default is row
			heatMap.setNormalization(HeatMap.NORMALIZATION_NONE);
		}
		BufferedImage bi = new BufferedImage(100, 100, BufferedImage.TYPE_3BYTE_BGR);
		Graphics2D g2 = bi.createGraphics();
		heatMap.updateSize(g2);
		heatMap.header.updateSize(heatMap.contentWidth, heatMap.elementSize.width, g2);
		g2.dispose();

		if(outputFileFormat.equals("jpeg")) {
			if(!outputFileName.toLowerCase().endsWith(".jpg") && !outputFileName.toLowerCase().endsWith("jpeg")) {
				outputFileName += ".jpg";
			}
		} else if(outputFileFormat.equals("png")) {
			if(!outputFileName.toLowerCase().endsWith(".png")) {
				outputFileName += ".png";
			}
		} else if(outputFileFormat.equals("tiff")) {
			if(!outputFileName.toLowerCase().endsWith(".tiff")) {
				outputFileName += ".tiff";
			}
		} else if(outputFileFormat.equals("bmp")) {
			if(!outputFileName.toLowerCase().endsWith(".bmp")) {
				outputFileName += ".bmp";
			}
		}
		try {
			ImageEncodeParam fParam = null;
			if(outputFileFormat.equals("jpeg")) {
				JPEGEncodeParam jpegParam = new JPEGEncodeParam();
				jpegParam.setQuality(1.0f);
				fParam = jpegParam;
			} else if(outputFileFormat.equals("png")) {
				fParam = new com.sun.media.jai.codec.PNGEncodeParam.RGB();
			} else if(outputFileFormat.equals("tiff")) {
				com.sun.media.jai.codec.TIFFEncodeParam param = new com.sun.media.jai.codec.TIFFEncodeParam();
				param.setCompression(com.sun.media.jai.codec.TIFFEncodeParam.COMPRESSION_NONE);
				fParam = param;
			} else if(outputFileFormat.equals("bmp")) {
				fParam = new com.sun.media.jai.codec.BMPEncodeParam();
			}
			JAI.create("filestore", heatMap.snapshot(), outputFileName, outputFileFormat, fParam);
		} catch(Exception ioe) {
			exit("An error occurred while saving the image.\nCause: " + ioe.getMessage());
		}
	}


	/**
	 *  Adds a listener to be notified when the element size is changed.
	 *
	 *@param  l  The feature to be added to the ElementSizeChangedListener
	 *      attribute
	 */
	public void addElementSizeChangedListener(ElementSizeChangedListener l) {
		eventListeners.add(ElementSizeChangedListener.class, l);
	}


	/**
	 *  Paint component into specified graphics.
	 *
	 *@param  g  Description of the Parameter
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		draw((Graphics2D) g);
	}


	void draw(Graphics2D g) {
		final int samples = dataset.getColumnDimension();
		Rectangle bounds = g.getClipBounds();
		int left = 0;
		int right = samples;
		int top = 0;
		int bottom = dataset.getRowDimension();

		if(bounds != null) {
			top = getTopIndex(bounds.y);
			bottom = getBottomIndex(bounds.y + bounds.height, dataset.getRowDimension());
			left = getLeftIndex(bounds.x);
			right = getRightIndex(bounds.x + bounds.width, samples);
		}
		Graphics2D g2 = (Graphics2D) g;
		// draw rectangles
		for(int row = top; row < bottom; row++) {
			for(int column = left; column < right; column++) {
				fillRectAt(g2, row, column);
			}
		}
		Color initColor = g.getColor();

		int expWidth = samples * this.elementSize.width + 5;

		if(geneClassVector.hasNonDefaultLabels()) { // draw colors beside the gene names
			for(int row = top; row < bottom; row++) {
				drawGeneLabel(g, row, expWidth);
			}
		}
		if(showGeneNames && geneMouseListener.topSelectedGeneIndex >= 0 && geneMouseListener.bottomSelectedGeneIndex <= dataset.getRowDimension()) {
			g2.setColor(Color.yellow);
			Composite oldComposite = g2.getComposite();
			g2.setComposite(HeatMap.SRC_OVER_COMPOSITE);
			int uniqX = elementSize.width * samples + 10;
			if(geneClassVector.hasNonDefaultLabels()) {
				uniqX += this.elementSize.width;
			}
			int topSel = (geneMouseListener.topSelectedGeneIndex) * elementSize.height;
			int bottomSel = (geneMouseListener.bottomSelectedGeneIndex) * elementSize.height;
			int leftSel = uniqX + insets.left;
			g2.fillRect(leftSel, topSel, geneNameWidth, bottomSel - topSel);
			g2.setColor(Color.black);
			g2.setComposite(oldComposite);
		}
		// draw annotations
		if(this.showGeneNames) {
			if(this.antiAliasing) {
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
				((Graphics2D) g).setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
			}
			if(right >= samples) {
				String label = "";
				g.setColor(Color.black);
				int uniqX = elementSize.width * samples + 10;

				if(geneClassVector.hasNonDefaultLabels()) {
					uniqX += this.elementSize.width;
				}
				FontMetrics fm = g.getFontMetrics();
				int descent = fm.getDescent();
				for(int row = top; row < bottom; row++) {
					label = dataset.getRowName(row);
					int annY = (row + 1) * elementSize.height;
					g.drawString(label, uniqX + insets.left, annY - descent);
				}
			}
		}
	}



	/**
	 *  Calls the <code>configureEnclosingScrollPane</code> method.
	 *
	 *@see    #configureEnclosingScrollPane
	 */
	public void addNotify() {
		super.addNotify();
		configureEnclosingScrollPane();
	}


	/**
	 *  Calls the <code>unconfigureEnclosingScrollPane</code> method.
	 *
	 *@see    #unconfigureEnclosingScrollPane
	 */
	public void removeNotify() {
		unconfigureEnclosingScrollPane();
		super.removeNotify();
	}


	public void resetSamplesOrder() {
		samplesOrder = createDefaultOrdering(dataset.getColumnDimension());
	}


	public void resetGenesOrder() {
		genesOrder = createDefaultOrdering(dataset.getRowDimension());
	}


	public void sortSamplesByColor() {
		sortByClass(sampleClassVector, samplesOrder);
	}


	public void sortGenesByColor() {
		sortByClass(geneClassVector, genesOrder);

	}


	public BufferedImage snapshot() {
		int headerHeight = header.height;
		//BufferedImage bi = new BufferedImage(contentWidth, height + headerHeight, BufferedImage.TYPE_INT_RGB);
		BufferedImage bi = new BufferedImage(contentWidth, height + headerHeight, BufferedImage.TYPE_3BYTE_BGR);

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


	/**  Updates the size of this heatmap panel and the header */
	public void updateSize() {
		Graphics2D g = (Graphics2D) getGraphics();
		if(g != null) {
			updateSize(g);
			g.dispose();
		}
		header.updateSize(contentWidth, elementSize.width);

	}


	/**
	 *  If this <code>HeatMap</code> is the <code>viewportView</code> of an
	 *  enclosing <code>JScrollPane</code> (the usual situation), configure this
	 *  <code>ScrollPane</code> by, amongst other things, installing the heat map's
	 *  <code>header</code> as the <code>columnHeaderView</code> of the scroll
	 *  pane. When a <code>HeatMap</code> is added to a <code>JScrollPane</code> in
	 *  the usual way, using <code>new JScrollPane(myHeatMap)</code>, <code>addNotify</code>
	 *  is called in the <code>HeatMap</code> (when the heat map is added to the
	 *  viewport). <code>HeatMap</code>'s <code>addNotify</code> method in turn
	 *  calls this method, which is protected so that this default installation
	 *  procedure can be overridden by a subclass.
	 *
	 *@see    #addNotify
	 */
	protected void configureEnclosingScrollPane() {
		Container p = getParent();
		if(p instanceof JViewport) {
			Container gp = p.getParent();
			if(gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if(viewport == null || viewport.getView() != this) {
					return;
				}
				scrollPane.setColumnHeaderView(header);
				header.updateSize(contentWidth, elementSize.width);
				//  scrollPane.getViewport().setBackingStoreEnabled(true);
				Border border = scrollPane.getBorder();
				if(border == null || border instanceof UIResource) {
					scrollPane.setBorder(UIManager.getBorder("Table.scrollPaneBorder"));
				}
			}
		}
	}


	/**
	 *  Reverses the effect of <code>configureEnclosingScrollPane</code> by
	 *  replacing the <code>columnHeaderView</code> of the enclosing scroll pane
	 *  with <code>null</code>. <code>HeatMap</code>'s <code>removeNotify</code>
	 *  method calls this method, which is protected so that this default
	 *  uninstallation procedure can be overridden by a subclass.
	 *
	 *@see    #removeNotify
	 *@see    #configureEnclosingScrollPane
	 */
	protected void unconfigureEnclosingScrollPane() {
		Container p = getParent();
		if(p instanceof JViewport) {
			Container gp = p.getParent();
			if(gp instanceof JScrollPane) {
				JScrollPane scrollPane = (JScrollPane) gp;
				// Make certain we are the viewPort's view and not, for
				// example, the rowHeaderView of the scrollPane -
				// an implementor of fixed columns might do this.
				JViewport viewport = scrollPane.getViewport();
				if(viewport == null || viewport.getView() != this) {
					return;
				}
				scrollPane.setColumnHeaderView(null);
			}
		}
	}


	/**
	 *  Selects rows from start to end.
	 *
	 *@param  color1  Description of the Parameter
	 *@param  color2  Description of the Parameter
	 *@return         Description of the Return Value
	 */
	/*
	    void selectRows(int start, int end) {
	    firstSelectedRow = start;
	    lastSelectedRow = end;
	    repaint();
	    }
	  */
	/**
	 *  Selects columns from start to end.
	 *
	 *@param  color1  Description of the Parameter
	 *@param  color2  Description of the Parameter
	 *@return         Description of the Return Value
	 */
	/*
	    void selectColumns(int start, int end) {
	    firstSelectedColumn = start;
	    lastSelectedColumn = end;
	    repaint();
	    }
	  */
	/**
	 *  Creates a gradient image with specified initial colors.
	 *
	 *@param  color1  Description of the Parameter
	 *@param  color2  Description of the Parameter
	 *@return         Description of the Return Value
	 */
	BufferedImage createGradientImage(Color color1, Color color2) {
		//BufferedImage image = (BufferedImage)java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration().createCompatibleImage(256, 1);
		BufferedImage image = new BufferedImage(256, 1, BufferedImage.TYPE_INT_RGB);

		Graphics2D graphics = image.createGraphics();
		GradientPaint gp = new GradientPaint(0, 0, color1, 255, 0, color2);
		graphics.setPaint(gp);
		graphics.drawRect(0, 0, 255, 1);
		return image;
	}



	static int[] createDefaultOrdering(int size) {
		int[] order = new int[size];
		for(int i = 0; i < order.length; i++) {
			order[i] = i;
		}
		return order;
	}


	/**
	 *  Updates the size of this heatmap
	 *
	 *@param  g  Description of the Parameter
	 */
	void updateSize(Graphics2D g) {
		int fontHeight = Math.min(14, elementSize.height);
		font = new Font("monospaced", Font.PLAIN, fontHeight);
		g.setFont(font);
		setFont(font);
		int width = elementSize.width * dataset.getColumnDimension() + 1 + insets.left;
		if(showGeneNames) {
			this.geneNameWidth = getMaxWidth(g);
			width += 20 + this.geneNameWidth;
		}

		if(geneClassVector.hasNonDefaultLabels()) {
			width += this.elementSize.width + 10;
		}

		this.contentWidth = width;
		this.height = elementSize.height * dataset.getRowDimension() + 1;
		setSize(width, height);
		setPreferredSize(new Dimension(width, height));
	}


	/**
	 *  Fills rect with specified row and colunn. Used to draw one 'cell' in the
	 *  heatmap.
	 *
	 *@param  g       Description of the Parameter
	 *@param  row     Description of the Parameter
	 *@param  column  Description of the Parameter
	 */
	void fillRectAt(Graphics2D g, int row, int column) {
		int x = column * elementSize.width + insets.left;
		int y = row * elementSize.height;

//		boolean selected = (this.firstSelectedRow >= 0 && this.lastSelectedRow >= 0) && (row >= this.firstSelectedRow && row <= this.lastSelectedRow);

		//	selected = (selected || this.firstSelectedColumn >= 0 && this.lastSelectedColumn >= 0 && (column >= this.firstSelectedColumn && column <= this.lastSelectedColumn));

		if(normalization == NORMALIZATION_ROW) {
			g.setColor(rowColorConverter.getColor(getRow(row), getColumn(column)));
		} else { //normalization = ABSOLUTE
			g.setColor(getColor(this.dataset.get(getRow(row), getColumn(column))));
		}

		g.fillRect(x, y, elementSize.width, elementSize.height);
		/*
		    if(selected) {
		    g.setColor(maskColor);
		    Composite oldComposite = g.getComposite();
		    g.setComposite(SRC_OVER_COMPOSITE);
		    g.fillRect(x, y, elementSize.width, elementSize.height);
		    g.setComposite(oldComposite);
		    }
		  */
		if(this.drawBorders) {
			g.setColor(Color.black);
			g.drawRect(x, y, elementSize.width - 1, elementSize.height - 1);
		}
	}



	void drawGeneLabel(Graphics g, int row, int xLoc) {
		/*
		    if(classVectors.isEmpty()) {
		    return;
		    }
		    ClassVector cv = (ClassVector) classVectors.get(0);
		    int[] levels = cv.getLevels();
		  */
		Color c = geneClassVector.getColorForIndex(genesOrder[row]);
		g.setColor(c);
		g.fillRect(xLoc + insets.left, row * elementSize.height, elementSize.width - 1, elementSize.height);
	}


	/**
	 *  Draws rect with specified row, column and color.
	 *
	 *@param  g       Description of the Parameter
	 *@param  row     Description of the Parameter
	 *@param  column  Description of the Parameter
	 *@param  color   Description of the Parameter
	 */
	void drawRectAt(Graphics g, int row, int column, Color color) {
		g.setColor(color);
		g.drawRect(column * elementSize.width + insets.left, row * elementSize.height, elementSize.width - 1, elementSize.height - 1);
	}


	/**
	 *  Finds column for specified x coordinate.
	 *
	 *@param  targetx  Description of the Parameter
	 *@return          -1 if column was not found.
	 */
	int findColumn(int targetx) {
		int xSize = dataset.getColumnDimension() * elementSize.width;
		if(targetx < insets.left) {
			return -1;
		}
		/*
		    if(targetx >= (xSize + insets.left) || targetx < insets.left) {
		    return -1;
		    }
		  */
		return (targetx - insets.left) / elementSize.width;
	}


	/**
	 *  Finds row for specified y coordinate.
	 *
	 *@param  targety  Description of the Parameter
	 *@return          -1 if row was not found.
	 */
	int findRow(int targety) {
		int ySize = dataset.getRowDimension() * elementSize.height;
		//	if(targety >= ySize || targety < 0) {
		//		return -1;
		//	}

		if(targety < 0) {
			return -1;
		}

		return targety / elementSize.height;
	}


	private void updateSizeAndRepaint() {
		updateSize();
		repaint();
		header.repaint();
	}


	private void sortByClass(MyClassVector cv, int[] order) {
		int index = 0;
		for(int i = 0, length = cv.levels(); i < length; i++) {
			int[] indices = cv.getIndices(cv.getLevel(i));
			for(int j = 0; j < indices.length; j++) {
				order[index++] = indices[j];
			}
		}
	}



	private void notifyElementSizeChangedListeners(ElementSizeChangedEvent e) {
		Object[] listeners = eventListeners.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i -= 2) {
			if(listeners[i] == ElementSizeChangedListener.class) {
				((ElementSizeChangedListener) listeners[i + 1]).elementSizeChanged(e);
			}
		}
	}


	public void setNormalization(int _normalization) {
		if(_normalization != NORMALIZATION_ROW && _normalization != NORMALIZATION_NONE) {
			throw new IllegalArgumentException("Unknown normalization");
		}
		this.normalization = _normalization;
		if(normalization == NORMALIZATION_NONE) {
			this.header.setDrawColorBar(true);
		} else {
			this.header.setDrawColorBar(false);
		}

	}


	/**
	 *  Sets the color respose to use when normalizing by row. Choices are
	 *  COLOR_RESPONSE_LOG and COLOR_RESPONSE_LINEAR
	 *
	 *@param  respose  The new rowColorResponse value
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


	public void setSampleClassVector(MyClassVector cv) {
		ClassVectorListener[] listeners = sampleClassVector.getListeners();
		sampleClassVector = cv;
		EventObject e = new EventObject(sampleClassVector);
		for(int i = 0; i < listeners.length; i++) {
			listeners[i].classVectorChanged(e);
			sampleClassVector.addClassVectorListener(listeners[i]);
		}

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
		updateSize();
		notifyElementSizeChangedListeners(new ElementSizeChangedEvent(this, width, height));
	}


	public void setAbsoluteColorScheme(Color minColor, Color maxColor, Color neutralColor) {
		posColorImage = createGradientImage(neutralColor, maxColor);
		negColorImage = createGradientImage(minColor, neutralColor);
		this.minColor = minColor;
		this.maxColor = maxColor;
		this.neutralColor = neutralColor;
	}


	/**
	 *  Sets the left margin for the viewer
	 *
	 *@param  leftMargin  The new leftInset value
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
	 *  Returns the index of the first selected row, -1 if no row is selected.
	 *
	 *@return    the index of the first selected row
	 */
	public int getSelectedRow() {
		return geneMouseListener.topSelectedGeneIndex;
	}


	/**
	 *  Returns the number of selected rows.
	 *
	 *@return    the number of selected rows, 0 if no rows are selected
	 */
	public int getSelectedRowCount() {
		return geneMouseListener.bottomSelectedGeneIndex - geneMouseListener.topSelectedGeneIndex;
	}


	public boolean isShowingSampleNames() {
		return header.isShowingSampleNames();
	}


	/**
	 *  Returns the index of the first selected column, -1 if no column is
	 *  selected.
	 *
	 *@return    the index of the first selected column
	 */
	public int getSelectedColumn() {
		return header.getSelectedColumn();
	}


	/**
	 *  Returns the number of selected columns.
	 *
	 *@return    the number of selected columns, 0 if no columns are selected
	 */
	public int getSelectedColumnCount() {
		return header.getSelectedColumnCount();
	}


	public String getToolTipText(MouseEvent event) {
		if(!showToolTipText) {
			return null;
		}
		int column = findColumn(event.getX());
		int row = findRow(event.getY());
		if(isLegalPosition(row, column)) {
			return "Value: " + numberFormat.format(dataset.get(row, column));
		}
		return null;
	}


	public boolean isShowingToolTipText() {
		return showToolTipText;
	}


	public Dataset getDataset() {
		return dataset;
	}


	public ExpressionData getExpressionMatrix() {
		return data;
	}


	public MyClassVector getSampleClassVector() {
		return sampleClassVector;
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


	public Color getMaxColor() {
		return maxColor;
	}


	public Color getMinColor() {
		return minColor;
	}


	public Color getNeutralColor() {
		return neutralColor;
	}


	/**
	 *  Returns the row index in the dataset corresponding to the passed index to
	 *  the genesOrder array
	 *
	 *@param  row  Description of the Parameter
	 *@return      The datasetRow value
	 */
	int getRow(int row) {
		return this.genesOrder[row];
	}



	int getColumn(int column) {
		return samplesOrder[column];
	}


	/**
	 *  Returns max width of annotation strings.
	 *
	 *@param  g  Description of the Parameter
	 *@return    The maxWidth value
	 */
	int getMaxWidth(Graphics2D g) {
		if(g == null) {
			return 0;
		}
		if(antiAliasing) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		FontMetrics fm = g.getFontMetrics();
		int max = 0;
		String str;
		for(int i = 0; i < dataset.getRowDimension(); i++) {
			//str = data.getElementAttribute(getMultipleArrayDataRow(i), labelIndex);
			str = dataset.getRowName(i);
			// str = genename ? data.getGeneName(getMultipleArrayDataRow(i)) : data.getUniqueId(getMultipleArrayDataRow(i));
			max = Math.max(max, fm.stringWidth(str));
		}
		return max;
	}


	/**
	 *  Calculates color for passed value using an absolute scale.
	 *
	 *@param  value  Description of the Parameter
	 *@return        The color value
	 */
	Color getColor(double value) {
		if(Double.isNaN(value)) {
			return missingColor;
		}
		double maximum = value < 0 ? this.minValue : this.maxValue;
		int colorIndex = (int) (255 * value / maximum);
		colorIndex = colorIndex > 255 ? 255 : colorIndex;
		int rgb = value < 0 ? negColorImage.getRGB(255 - colorIndex, 0) : posColorImage.getRGB(colorIndex, 0);
		return new Color(rgb);
	}


	int getTopIndex(int top) {
		if(top < 0) {
			return 0;
		}
		return top / elementSize.height;
	}


	int getLeftIndex(int left) {
		if(left < insets.left) {
			return 0;
		}
		return (left - insets.left) / elementSize.width;
	}


	int getRightIndex(int right, int limit) {
		if(right < 0) {
			return 0;
		}
		int result = right / elementSize.width + 1;
		return result > limit ? limit : result;
	}


	int getBottomIndex(int bottom, int limit) {
		if(bottom < 0) {
			return 0;
		}
		int result = bottom / elementSize.height + 1;
		return result > limit ? limit : result;
	}


	boolean isLegalPosition(int row, int column) {
		return (isLegalRow(row) && isLegalColumn(column));
	}


	boolean isLegalColumn(int column) {
		if(column < 0 || column > dataset.getColumnDimension() - 1) {
			return false;
		}
		return true;
	}


	boolean isLegalRow(int row) {
		if(row < 0 || row >= dataset.getRowDimension()) {
			return false;
		}
		return true;
	}



	class GeneMouseListener extends MouseAdapter implements MouseMotionListener {
		int bottomSelectedGeneIndex = -1;
		int topSelectedGeneIndex = -1;
		int lastYIndex = -1;


		public void mouseMoved(MouseEvent e) { }


		public void mousePressed(MouseEvent e) {
			// start of selection

			bottomSelectedGeneIndex = findRow(e.getY());
			System.out.println("bottomSelectedGeneIndex " + bottomSelectedGeneIndex);
			// if click on 0th cell, top = 1 and bottom = 0
			bottomSelectedGeneIndex++;
			topSelectedGeneIndex = bottomSelectedGeneIndex - 1;
			lastYIndex = bottomSelectedGeneIndex;

			int uniqX = elementSize.width * dataset.getColumnDimension() + 10;
			if(geneClassVector.hasNonDefaultLabels()) {
				uniqX += elementSize.width;
			}
			int leftSel = uniqX + insets.left;
			if(e.getX() > leftSel + geneNameWidth || e.getX() < leftSel) {
				bottomSelectedGeneIndex = -1;
				topSelectedGeneIndex = -1;
			}

			repaint();
		}


		public void mouseDragged(MouseEvent e) {
			updateMouse(e);
			Rectangle r = null;
			if(e.getY() <= 0) {
				r = new Rectangle(e.getX(), e.getY(), elementSize.height, elementSize.height);
				// this rectangle becomes visible

			} else {
				r = new Rectangle(e.getX(), e.getY(), elementSize.height, elementSize.height);
				// this rectangle becomes visible
				((JPanel) e.getSource()).scrollRectToVisible(r);
			}
			repaint();
		}


		private void updateMouse(MouseEvent e) {

			int index = findRow(e.getY());
			//System.out.println("index " + index);
			if(index < 0) {
				index = 0;
			}
			if(index > dataset.getRowDimension()) {
				index = dataset.getRowDimension();
			}
			// when moving up, update top index if click > top, else update bottom index

			if(index > lastYIndex) {
				if(lastYIndex < bottomSelectedGeneIndex && index >= bottomSelectedGeneIndex) {
					//crossover
					topSelectedGeneIndex = bottomSelectedGeneIndex - 1;
					bottomSelectedGeneIndex = index;
				} else if(index >= bottomSelectedGeneIndex) {

					bottomSelectedGeneIndex = index;
				} else {

					topSelectedGeneIndex = index;
				}
			}  // when moving down, update bottom index if click < bottom, else update top index
			else {

				if(lastYIndex > topSelectedGeneIndex && index <= topSelectedGeneIndex) {
					//crossover
					bottomSelectedGeneIndex = topSelectedGeneIndex + 1;
					topSelectedGeneIndex = index;
				} else if(index <= topSelectedGeneIndex) {
					topSelectedGeneIndex = index;

				} else {
					bottomSelectedGeneIndex = index;

				}
			}

			lastYIndex = index;
		}

	}


	/**
	 *  The class to listen to mouse events.
	 *
	 *@author     jgould
	 *@created    March 10, 2004
	 */
	class Listener extends MouseAdapter implements MouseMotionListener {

		String oldStatusText;
		int oldRow = -1;
		int oldColumn = -1;


		public void mouseClicked(MouseEvent event) {
			if(SwingUtilities.isRightMouseButton(event)) {
				return;
			}
			int column = findColumn(event.getX());
			int row = findRow(event.getY());

			int clickCount = event.getClickCount();
			if(clickCount >= 2 && column >= dataset.getColumnDimension() && isLegalRow(row)) { // double click on gene name
				String geneName = dataset.getRowName(row);
				String url = geneURL.replaceAll(GENE_QUERY, geneName);
				try {
					edu.mit.broad.modules.heatmap.util.BrowserLauncher.openURL(url);
				} catch(java.io.IOException ioe) {
					ioe.printStackTrace();
				}
				return;
			}

			if(!isLegalPosition(row, column)) {
				return;
			}
			// TODO make plot of genes

		}


		public void mouseMoved(MouseEvent event) {
			if(dataset.getColumnDimension() == 0 || event.isShiftDown()) {
				return;
			}
			int column = findColumn(event.getX());
			int row = findRow(event.getY());
			if(isCurrentPosition(row, column)) {
				return;
			}
			Graphics g = null;
			if(isLegalPosition(row, column)) {
				g = getGraphics(); //FIXME
				drawRectAt(g, row, column, Color.white); // draw border
				//framework.setStatusText("Gene: " + data.getUniqueId(getMultipleArrayDataRow(row)) + " Sample: " + data.getSampleName(dataset.getSampleIndex(getColumn(column))) + " Value: " + dataset.get(getRow(row), getColumn(column)));
			} else {
				; //framework.setStatusText(oldStatusText);
			}
			if(isLegalPosition(oldRow, oldColumn)) {
				g = g != null ? g : getGraphics(); // FIXME
				fillRectAt((Graphics2D) g, oldRow, oldColumn);
			}
			setOldPosition(row, column);
			if(g != null) {
				g.dispose();
			}
		}



		public void mouseEntered(MouseEvent event) {
			//	oldStatusText = framework.getStatusText();
		}


		public void mouseExited(MouseEvent event) {
			if(isLegalPosition(oldRow, oldColumn)) {
				Graphics g = getGraphics(); // FIXME
				fillRectAt((Graphics2D) g, oldRow, oldColumn);
				g.dispose();
			}
			setOldPosition(-1, -1);
			//framework.setStatusText(oldStatusText);
		}


		public void mouseDragged(MouseEvent event) { }


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

