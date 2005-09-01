package edu.mit.broad.modules.heatmap;
import java.awt.*;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import javax.swing.JComponent;
import javax.swing.JPanel;
import org.genepattern.data.matrix.*;


/**
 *  This class is used to render header of an experiment.
 *
 * @author     Aleksey D.Rezantsev
 * @author     Joshua Gould
 * @created    March 10, 2004
 * @version    1.0
 */
public class HeatMapHeader extends JPanel implements ClassVectorListener {


	HeatMap heatMap;

	private final static int IMAGE_HEIGHT = 15;
	/**  The space between the heatmap and the sample names */
	private final static int COLOR_BAR_HEIGHT = 10;
	private Insets insets = new Insets(0, 10, 0, 0);
	private boolean drawColorBar = false;

	boolean drawSampleNames = true;

	private MouseListener mouseListener = new MouseListener();
	Font font;
	
	int height = 0;
	
	/**  used for drawing */
	private int fHeight = 0;


	public HeatMapHeader(HeatMap heatMap) {
		this.heatMap = heatMap;
		setBackground(Color.white);
		addMouseListener(mouseListener);
		addMouseMotionListener(mouseListener);
	}


	public void classVectorChanged(java.util.EventObject event) {
		updateSize(heatMap.contentWidth, heatMap.elementSize.width);
		repaint();
	}


	public void updateSize(int contentWidth, int elementWidth) {
		Graphics2D g2 = (Graphics2D) getGraphics();
		if(g2!=null) {
			updateSize(contentWidth, elementWidth, g2);
			g2.dispose();
		}
	}
	
	/**
	 *  Updates size of this header.
	 *
	 * @param  contentWidth  Description of the Parameter
	 * @param  elementWidth  Description of the Parameter
	 */
	public void updateSize(int contentWidth, int elementWidth, Graphics2D g) {
		setElementWidth(elementWidth);
		if(heatMap.antiAliasing) {
			g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		FontMetrics hfm = g.getFontMetrics();
		int maxHeight = 0;

		final int size = heatMap.dataset.getColumnCount();
		if(drawSampleNames) {
			for(int feature = 0; feature < size; feature++) {
				String name = heatMap.dataset.getColumnName(heatMap.getColumn(feature));
				maxHeight = Math.max(maxHeight, hfm.stringWidth(name));
			}
		}
		maxHeight += IMAGE_HEIGHT + hfm.getHeight() + 10;
		maxHeight += getColorBarHeight();
		setSize(contentWidth, maxHeight);
		setPreferredSize(new Dimension(contentWidth, maxHeight));
		height = maxHeight;
	}


	/**
	 *  Paints the header into specified graphics.
	 *
	 * @param  g  Description of the Parameter
	 */
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		Graphics2D g2 = (Graphics2D) g;
		draw(g2);

	}


	/**
	 *  Sets the left margin for the header
	 *
	 * @param  leftMargin  The new leftInset value
	 */
	public void setLeftInset(int leftMargin) {
		insets.left = leftMargin;
	}


	void draw(Graphics2D g2) {
		if(heatMap.antiAliasing) {
			g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
			g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
		}
		drawHeader(g2);
	}


	void setDrawColorBar(boolean b) {
		drawColorBar = b;
	}


	int getSelectedColumnCount() {
		return mouseListener.lastIndex - mouseListener.firstIndex;
	}

	void setShowSampleNames(boolean b) {
		drawSampleNames = b;
	}
	
	boolean isShowingSampleNames() {
		return drawSampleNames;	
	}

	int getSelectedColumn() {

		return mouseListener.firstIndex;
	}


	/**
	 *  Draws the header into specified graphics.
	 *
	 * @param  g  Description of the Parameter
	 */
	private void drawHeader(Graphics2D g) {
		final int samples = heatMap.dataset.getColumnCount();
		if(samples == 0) {
			return;
		}
		int width = samples * heatMap.elementSize.width;
		if(drawColorBar) {
		//	g.drawImage(heatMap.negColorImage, insets.left, 0, (int) (width / 2f), IMAGE_HEIGHT, null);
		//	g.drawImage(heatMap.posColorImage, (int) ((width) / 2f + insets.left), 0, (int) (width / 2.0), IMAGE_HEIGHT, null);
		}
		FontMetrics hfm = g.getFontMetrics();
		int descent = hfm.getDescent();
		fHeight = hfm.getHeight();

		g.setColor(Color.black);

		int textWidth;
		if(drawColorBar) {
			g.drawString(String.valueOf(heatMap.minValue), insets.left, IMAGE_HEIGHT + fHeight);
			textWidth = hfm.stringWidth("1:1");
			g.drawString("1:1", (int) (width / 2f) - textWidth / 2 + insets.left, IMAGE_HEIGHT + fHeight);
			textWidth = hfm.stringWidth(String.valueOf(heatMap.maxValue));
			g.drawString(String.valueOf(heatMap.maxValue), width - textWidth + insets.left, IMAGE_HEIGHT + fHeight);
		}

		int h = -getSize().height + 8;
		if(this.getColorBarHeight() > 0) {
			h += COLOR_BAR_HEIGHT;
		}

		if(drawSampleNames && mouseListener.firstIndex >= 0) {
			g.setColor(Color.yellow);
			Composite oldComposite = g.getComposite();
			g.setComposite(HeatMap.SRC_OVER_COMPOSITE);
			int xstart = heatMap.elementSize.width * mouseListener.firstIndex;
			int xend = heatMap.elementSize.width * mouseListener.lastIndex;

			int top = 0;
			if(drawColorBar) {
				top = top + IMAGE_HEIGHT + fHeight;
			}
			int bottom = getHeight();
			if(heatMap.sampleClassVector.hasNonDefaultLabels()) {
				bottom -= COLOR_BAR_HEIGHT;
			}

			g.fillRect(xstart + insets.left, top, xend - xstart, bottom - top);
			g.setColor(Color.black);
			g.setComposite(oldComposite);
		}

		Rectangle bounds = g.getClipBounds();
		int left = 0;
		int right = samples;
		if(bounds != null) {
			left = heatMap.getLeftIndex(bounds.x);
			right = heatMap.getRightIndex(bounds.x + bounds.width, samples);
		}

		Graphics2D g2 = (Graphics2D) g;

		if(drawSampleNames) {
			g.rotate(-Math.PI / 2);
			for(int sample = left; sample < right; sample++) {
				String name = heatMap.dataset.getColumnName(heatMap.samplesOrder[sample]);
				//g.drawString(name, h, hfm.getAscent() + heatMap.elementSize.width * sample + heatMap.elementSize.width / 2 + insets.left);
				g.drawString(name, h, hfm.getAscent() + heatMap.elementSize.width * sample + insets.left);
			}
			g.rotate(Math.PI / 2);
		}

		if(heatMap.sampleClassVector.hasNonDefaultLabels()) {
			for(int sample = left; sample < right; sample++) {
				Color c = heatMap.sampleClassVector.getColorForIndex(heatMap.samplesOrder[sample]);
				g.setColor(c);
				g.fillRect(sample * heatMap.elementSize.width + insets.left, getSize().height - COLOR_BAR_HEIGHT - 2, heatMap.elementSize.width, COLOR_BAR_HEIGHT);
			}
		}
	}


	/**
	 *  Sets an element width.
	 *
	 * @param  width  The new heatMap.elementSize.width value
	 */
	private void setElementWidth(int width) {
		width = Math.min(width, 14);
		font = new Font(heatMap.fontFamilyName, heatMap.fontStyle, width);
		setFont(font);
	}


	/**
	 *  Returns height of color bar for experiments
	 *
	 * @return    The colorBarHeight value
	 */
	private int getColorBarHeight() {
		/*
		    for(int sample = 0; sample < heatMap.samplesOrder.length; sample++) {
		    FIXME	//if(data.getExperimentColor(experiment.getSampleIndex(heatMap.samplesOrder[sample])) != null) {
		    return COLOR_BAR_HEIGHT;
		    }
		    }
		  */
		if(!heatMap.sampleClassVector.hasNonDefaultLabels()) {
			return 0;
		} else {
			return COLOR_BAR_HEIGHT;
		}
	}


	class MouseListener extends MouseAdapter implements MouseMotionListener {
		/**  left-most selected sample index */
		int firstIndex = -1;
		/**  right-most selected sample index */
		int lastIndex = -1;
		int lastMouseEvent = -1;


		public void mouseMoved(MouseEvent e) { }


		public void mousePressed(MouseEvent e) {// start of selection

			firstIndex = heatMap.findColumn(e.getX());

			// if click on 0th cell, left = 0 and right = 1
			lastIndex = firstIndex + 1;

			if(firstIndex < 0 || lastIndex > heatMap.dataset.getColumnCount()) {
				firstIndex = -1;
				lastIndex = -1;
			} else if(!yIsInRange(e)) {
				firstIndex = -1;
				lastIndex = -1;
			}

			lastMouseEvent = firstIndex;
			repaint();
		}


		public void mouseDragged(MouseEvent e) {
			int index = heatMap.findColumn(e.getX());
			if(index < 0) {
				index = 0;
			} else if(index > heatMap.dataset.getColumnCount()) {
				index = heatMap.dataset.getColumnCount();
			}

			if(index > lastMouseEvent) {// when moving right
				if(lastMouseEvent < lastIndex && index >= lastIndex) { //crossover
					firstIndex = lastIndex - 1;
					lastIndex = index;
					System.out.println("C");
				} else if(index >= lastIndex) {
					lastIndex = index;
					System.out.println("firstIndex " + firstIndex + " lastIndex " + lastIndex);
				} else {
					System.out.println("B");
					firstIndex = index;
				}
			} // when moving left, update first index if click < first index, else update last index
			else {
				if(lastMouseEvent > firstIndex && index <= firstIndex) {
					//crossover
					lastIndex = firstIndex + 1;
					firstIndex = index;
				} else if(index <= firstIndex) {
					firstIndex = index;
				} else {
					lastIndex = index;
				}
			}

			lastMouseEvent = index;
			repaint();
		}


		boolean yIsInRange(MouseEvent e) {
			int top = 0;
			if(drawColorBar) {
				top = top + IMAGE_HEIGHT + fHeight;
			}
			int bottom = getHeight();
			if(getColorBarHeight() > 0) {
				bottom -= COLOR_BAR_HEIGHT;
			}
			return (e.getY() <= bottom && e.getY() >= top);
		}

	}

}

