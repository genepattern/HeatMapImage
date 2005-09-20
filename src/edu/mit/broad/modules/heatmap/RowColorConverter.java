package edu.mit.broad.modules.heatmap;
import java.awt.*;
import java.awt.geom.*;
import java.text.NumberFormat;
import javax.swing.*;
import org.genepattern.data.matrix.*;

/**
 *  converts double values to a color from a color map. Performs normalization by
 *  row.
 *
 * @author     Keith Ohm
 * @author     jgould
 * @created    August 12, 2003
 */
public final class RowColorConverter {

	/**  use log scale */
	 static int COLOR_RESPONSE_LOG = HeatMap.COLOR_RESPONSE_LOG;
	 static int COLOR_RESPONSE_LINEAR = HeatMap.COLOR_RESPONSE_LINEAR;
	int response = COLOR_RESPONSE_LINEAR;
	DoubleMatrix2D dataset;
	/**  the last row for which max, min, and mean were computed */
	int lastRow = -1;
	static Color missingColor = new Color(128, 128, 128);

	/**  the minimum value */
	private double min = 0;
	/**  the maximum value */
	private double max = 8000;
	/**  the mean value */
	private double mean = Double.NEGATIVE_INFINITY;

	/**  the color table */
	private final Color[] colors;
	/**  the boundry values used to determine which color to associate a value */
	private final double[] slots;

	private static int[] defaultColorMap = {0x4500ad, 0x2700d1, 0x6b58ef, 0x8888ff, 0xc7c1ff,
			0xd5d5ff, 0xffc0e5, 0xff8989, 0xff7080, 0xff5a5a, 0xef4040, 0xd60c00};

	private boolean globalScale;
	
	/**
	 *  constructs a new ColorConverter
	 *
	 * @param  colormap  array of rgb values. Each element contains an RGB value
	 *      consisting of the red component in bits 16-23, the green component in
	 *      bits 8-15, and the blue component in bits 0-7.
	 * @param  dataset   Description of the Parameter
	 * @param  response  Description of the Parameter
	 */
	public RowColorConverter(int[] colormap, int response, DoubleMatrix2D dataset) {
		this(getColorMap(colormap), response, dataset);
	}
	
	public static Color[] getDefaultColorMap() {
		return getColorMap(defaultColorMap);
	}
	
	private static Color[] getColorMap(int[] colormap) {
		Color[] colors = new Color[colormap.length];
		for(int i = 0; i < colormap.length; ++i) {
			colors[i] = new Color(colormap[i]);
		}
		return colors;
	}
	
	public RowColorConverter(Color[] colors, int response, DoubleMatrix2D dataset) {
		this.colors = colors;
		this.slots = new double[colors.length];
		if(response != COLOR_RESPONSE_LINEAR && response != COLOR_RESPONSE_LOG) {
			throw new IllegalArgumentException("Unkown color response");
		}
		this.response = response;
		this.dataset = dataset;
	}
	
	public RowColorConverter(int response, DoubleMatrix2D dataset) {
		this(defaultColorMap, response, dataset);
	}

 /** sets the params for Global (absolute) color scheme */
	public final void setGlobalScale (boolean b) {
		if(b) {
			max = -Double.MAX_VALUE;
			min = Double.MAX_VALUE;
			mean = 0;
			for(int i = 0, rows = dataset.getRowCount(); i < rows; i++) {
				for(int j = 0, columns = dataset.getColumnCount(); j < columns; j++) {
					double d = dataset.get(i, j);
					max = d > max ? d : max;
					min = d < min ? d : min;
					mean +=d;
				}
			}  
			int num = dataset.getColumnCount() * dataset.getRowCount();
			mean /= num;
			calculateSlots (min, max, mean, slots);
		} 
		this.globalScale = b;
		  
    }
  
    
	public void setColorResponse(int _response) {
		if(_response != COLOR_RESPONSE_LINEAR && _response != COLOR_RESPONSE_LOG) {
			throw new IllegalArgumentException("Unkown color response");
		}
		if(response != _response) {
			this.response = _response;
		}
		
		
	}
	

	/**
	 *  gets the color for the specified entry in the dataset. Getting colors in a
	 *  loop row-by-row is quicker than column-by-column.
	 *
	 * @param  row     row index of dataset
	 * @param  column  column index of dataset
	 * @return         The color value
	 */
	public Color getColor(int row, int column) {
		if(!globalScale && lastRow != row) {
			calculateRowStats(row);
			lastRow = row;
		}
		double val = dataset.get(row, column);
		if(Double.isNaN(val)) {
			return missingColor;
		}
		final int num = slots.length - 1;
		if(val >= slots[num]) {
			return colors[num];
		}
		for(int i = num; i > 0; i--) {// rev loop
			if(slots[i] > val && val > slots[i - 1]) {//assumes slots[i] > slots[i - 1]
				return colors[i];
			}
		}
		return colors[0];// all the rest
	}


	/**
	 *  gets the color at the index
	 *
	 * @param  i  Description of the Parameter
	 * @return    The colorAt value
	 */
	public Color getColorAt(int i) {
		return colors[i];
	}


	/**
	 *  returns the number of colors
	 *
	 * @return    The colorCount value
	 */
	public int getColorCount() {
		return colors.length;
	}


	/**
	 *  gets a copy of the slots array including the first element
	 *
	 * @return    The slots value
	 */
	public double[] getSlots() {
		double[] new_slots = new double[slots.length + 1];
		new_slots[0] = min;
		System.arraycopy(slots, 0, new_slots, 1, slots.length);
		return new_slots;
	}


	/**
	 *  helper method to calculate the slots considering the color response
	 *
	 * @param  min     Description of the Parameter
	 * @param  max     Description of the Parameter
	 * @param  mean    Description of the Parameter
	 * @param  values  Description of the Parameter
	 */
	private void calculateSlots(final double min, final double max, final double mean, final double[] values) {
		if(response == COLOR_RESPONSE_LOG) {
			computeLogScaleSlots(min, max, mean, values);
		} else {
			computeLinearSlots(min, max, mean, values);
		}

	}


	/**
	 *  Calculates the min, max, and mean for the specified row in the dataset.
	 *
	 * @param  rowNumber  Description of the Parameter
	 */
	private void calculateRowStats(int rowNumber) {
		double theMin;
		double theMax;
		double theMean;
		int num = dataset.getColumnCount();
		theMin = Double.POSITIVE_INFINITY;
		theMax = Double.NEGATIVE_INFINITY;
		theMean = 0;
		int numDataPoints = 0;// some arrays might not have data for all genes
		for(int i = 0; i < num; i++) {
			double tmpVal = (double) dataset.get(rowNumber, i);
			if(Double.isNaN(tmpVal)) {
				continue;
			}
			if(tmpVal < theMin) {
				theMin = tmpVal;
			}
			if(tmpVal > theMax) {
				theMax = tmpVal;
			}
			theMean += tmpVal;
			numDataPoints++;
		}
		theMean /= numDataPoints;

		this.min = theMin;
		this.max = theMax;
		this.mean = theMean;
		calculateSlots(min, max, mean, slots);
	}


	/**
	 *  conputes the log scaled slot values
	 *
	 * @param  real_min   Description of the Parameter
	 * @param  real_max   Description of the Parameter
	 * @param  real_mean  Description of the Parameter
	 * @param  slots      Description of the Parameter
	 */
	private void computeLogScaleSlots(final double real_min, final double real_max, final double real_mean, final double[] slots) {
		//cannot take log of 0 or negatives
		final double min = 1f;
		//cannot take log of 0 or negatives
		final double max = real_max - real_min + min;
		//cannot take log of 0 or negatives
		final double mean = real_mean - real_min + min;
		final double range = (double) (Math.log(max) - Math.log(min));
		final int num = slots.length;
		final double inc = range / (double) num;
		double log_val = inc;

		final double adjustment = real_min - min;
		for(int i = 0; i < num; i++) {
			slots[i] = (double) Math.exp(log_val) + adjustment;
			log_val += inc;
		}
		//System.out.println("final log_val="+(log_val - inc));
	}


	/**
	 *  computes the linear (almost) slot values
	 *
	 * @param  min    Description of the Parameter
	 * @param  max    Description of the Parameter
	 * @param  mean   Description of the Parameter
	 * @param  slots  Description of the Parameter
	 */
	private void computeLinearSlots(final double min, final double max, final double mean, final double[] slots) {
		//	final boolean min_to_max = true;
		final double ave = (mean == Double.NEGATIVE_INFINITY ? (max - min) / 2 : mean);
		final int num = slots.length;
		final int halfway = slots.length / 2;

		//	if(min_to_max) {
		// calc the range min -> ave
		final double inc2 = (ave - min) / halfway;
		double lin_val = min;
		for(int i = 0; i < halfway; i++) {
			lin_val += inc2;
			slots[i] = lin_val;
		}

		// ave -> max
		final double inc = (max - ave) / (num - halfway);
		lin_val = ave;
		for(int i = halfway; i < num; i++) {
			lin_val += inc;
			slots[i] = lin_val;
		}
//		}
		/*
		    } else {// max -> min
		    / max -> ave
		    final double inc = (ave - max) / (num - halfway);
		    lin_val = max;
		    for(int i = 0; i < halfway; i++) {
		    slots[i] = lin_val;
		    lin_val += inc;// actually is adding a negative
		    }
		    / calc the range ave -> min
		    final double inc2 = (min - ave) / halfway;
		    lin_val = ave;
		    for(int i = halfway; i < num; i++) {
		    slots[i] = lin_val;
		    lin_val += inc2;
		    }
		    }
		 */
	}


}

