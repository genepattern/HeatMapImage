package edu.mit.broad.modules.heatmap;
public class ElementSizeChangedEvent extends java.util.EventObject {
	int width, height;
	
	public ElementSizeChangedEvent(Object source, int width, int height) {
		super(source);	
		this.width = width;
		this.height = height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public int getHeight() {
		return height;
	}
}
