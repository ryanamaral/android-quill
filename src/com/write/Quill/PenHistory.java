package com.write.Quill;

import java.util.LinkedList;
import java.util.ListIterator;

import com.write.Quill.Stroke.PenType;


import android.util.Log;


public class PenHistory {
	private static final String TAG = "PenHistory";
	
	private static final PenHistory instance = new PenHistory();
	protected static final int MAX_SIZE = 10;
	protected LinkedList<HistoryItem> history = new LinkedList<HistoryItem>(); 
	
	
	private PenHistory() {}
	
	public static PenHistory getPenHistory() {
		return instance;
	}
	
	public int size() {
		return history.size();
	}
	
	public int getColor(int historyIndex) {
		return history.get(historyIndex).penColor;
	}

	public PenType getPenType(int historyIndex) {
		return history.get(historyIndex).penType;
	}

	public int getThickness(int historyIndex) {
		return history.get(historyIndex).penThickness;
	}
	
	public int getPreviousColor() {
		if (history.size() >= 2)
			return history.get(1).penColor;
		else
			return getNewestColor();
	}
	
	public PenType getPreviousPenType() {
		if (history.size() >= 2)
			return history.get(1).penType;
		else
			return getNewestPenType();
	}

	public int getPreviousThickness() {
		if (history.size() >= 2)
			return history.get(1).penThickness;
		else
			return getNewestThickness();
	}

	public int getNewestColor() {
		return history.getFirst().penColor;
	}
	
	public PenType getNewestPenType() {
		return history.getFirst().penType;
	}
	
	public int getNewestThickness() {
		return history.getFirst().penThickness;
	}
	
	public class HistoryItem {
	
		public HistoryItem(PenType new_pen_type, int new_pen_thickness, int new_pen_color) {
			penType = new_pen_type;
			penThickness = new_pen_thickness;
			penColor = new_pen_color;
		}
		
		protected PenType penType;
		protected int penThickness;
		protected int penColor;
		
		public boolean equals(HistoryItem other) {
			return penType == other.penType && 
				penThickness == other.penThickness && 
				penColor == other.penColor;
		}
	}
	
	public static boolean add(PenType penType, int penThickness, int penColor) {
		historyItem = 0;
		PenHistory instance = PenHistory.getPenHistory();
		LinkedList<HistoryItem> history = instance.history;
		ListIterator<HistoryItem> iter = history.listIterator();
		while (iter.hasNext()) {
			HistoryItem h = iter.next();
			if (h.penType == penType && 
				h.penThickness == penThickness && 
				h.penColor == penColor) {
				history.remove(h);
				history.addFirst(h);
				return false;
			}
		}
		HistoryItem item = PenHistory.instance.new HistoryItem(penType, penThickness, penColor);
		history.addFirst(item);
		if (history.size() > PenHistory.MAX_SIZE)
			history.removeLast();
		return true;
	}

	
    private static int historyItem = 0;
    
    public int nextHistoryItem() {
    	historyItem++;
    	if (historyItem >= size())
    		historyItem = 0;
    	return historyItem;
    }
	
	
}

