package edu.mit.broad.modules.heatmap;
import java.awt.Color;
import java.util.EventObject;
import java.util.EventListener;
import javax.swing.event.EventListenerList;
import org.genepattern.data.matrix.*;

public class MyClassVector {
	public static int DEFAULT_CLASS_NUMBER = -1;
	EventListenerList eventListeners = new EventListenerList();
	boolean hasLabels = false;
	ColoredClassVector cv;


	public MyClassVector(ColoredClassVector cv) {
		this.cv = cv;
		hasLabels = true;
	}
	
	public MyClassVector(int size) {
		cv = new ColoredClassVector(size); // FIXME set default class label for everything
	}


	public boolean hasNonDefaultLabels() {
		return hasLabels;
	}


	public void removeClassVectorListener(ClassVectorListener listener) {
		eventListeners.remove(ClassVectorListener.class, listener);
	}


	public void addClassVectorListener(ClassVectorListener l) {
		eventListeners.add(ClassVectorListener.class, l);
	}


/*	public void addClass(int classNumber, String label) {
		cv.addClass(classNumber, label);
		notifyListeners();
	}*/

   /*
   public void setClass(int index, int classNumber) {
		if(classNumber != DEFAULT_CLASS_NUMBER) {
			hasLabels = true;
		}
		cv.setClass(index, classNumber);
		notifyListeners();
	}
   */

	public int getClassCount() {
		return cv.getClassCount();
	}


	public void setColor(String label, Color c) {
		cv.setColor(label, c);
		notifyListeners();
	}


	


	public Color getColorForIndex(int index) {
		return cv.getColorForIndex(index);
	}


	public String getClassName(int index) {
		return cv.getClassName(index);
	}

	public int[] getIndices(int classNumber) {
		return cv.getIndices(classNumber);
	}

	public ClassVectorListener[] getListeners() {
		EventListener[] temp = eventListeners.getListeners(ClassVectorListener.class);
		ClassVectorListener[] list = new ClassVectorListener[temp.length];
		for(int i = 0; i < temp.length; i++) {
			list[i] = (ClassVectorListener) temp[i];	
		}
		return list;
		
	}

	private void notifyListeners() {
		EventObject e = new EventObject(this);
		Object[] listeners = eventListeners.getListenerList();
		for(int i = listeners.length - 2; i >= 0; i -= 2) {
			if(listeners[i] == ClassVectorListener.class) {
				((ClassVectorListener) listeners[i + 1]).classVectorChanged(e);
			}
		}
	}
	
	public Color getColorForClassName(String className) {
		return cv.getColorForClassName(className);
	}
	
	public MyClassVector slice(int[] indices) {
		return new MyClassVector((ColoredClassVector)cv.slice(indices));
	}
	
	public void writeClsFile(String fileName) throws java.io.IOException {
		org.genepattern.io.expr.cls.ClsWriter writer = new org.genepattern.io.expr.cls.ClsWriter();
		java.io.FileOutputStream fos = null;
		try {
		fos = new java.io.FileOutputStream(fileName);
		writer.write(cv, fos);
		} finally {
			try {
				fos.close();	
			} catch(java.io.IOException ioe) {
				
			}
		}
	}
}

