package edu.mit.broad.modules.heatmap;
import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import org.genepattern.data.matrix.ClassVector;

/**
 *  Description of the Class
 *
 * @author    Joshua Gould
 */
public class ColoredClassVector extends ClassVector {
   protected Map className2ColorMap;
   final static Color[] DEFAULT_COLORS
          = {Color.red, Color.blue, Color.yellow, Color.lightGray,
         Color.magenta};


   public ColoredClassVector(int size) {
      super(new String[size]);
      className2ColorMap = new HashMap();
      className2ColorMap.put(getClassName(0), Color.white);
   }


 /*  public ColoredClassVector(ClassVector cv) {
      super(cv);
      className2ColorMap = new HashMap();
      int length = Math.min(cv.getClassCount(), DEFAULT_COLORS.length);
      for(int i = 0; i < length; i++) {
         className2ColorMap.put(cv.getClassName(cv.getLevel(i)),
               DEFAULT_COLORS[i]);
      }
      for(int i = length; i < cv.getClassCount(); i++) {
         className2ColorMap.put(cv.getClassName(cv.getLevel(i)),
               new Color((int) (255.0 * Math.random()),
               (int) (255.0 * Math.random()),
               (int) (255.0 * Math.random())));
      }
   }
*/

  /* public ClassVector slice(int[] indices) {
      return new ColoredClassVector(super.slice(indices));
   }*/


   public void setColor(String label, Color c) {
      className2ColorMap.put(label, c);
   }


   public Color getColorForClassName(String className) {
      return (Color) className2ColorMap.get(className);
   }


   public Color getColorForIndex(int index) {
      String className = getClassName(getAssignment(index));
      return (Color) className2ColorMap.get(className);
   }
}
