package name.vbraun.view.write;

import java.util.LinkedList;
import java.util.ListIterator;

import name.vbraun.view.write.Graphics.Tool;

import android.util.Log;


public class ToolHistory {
	private static final String TAG = "ToolHistory";
	
	private static final ToolHistory instance = new ToolHistory();
	protected static final int MAX_SIZE = 10;
	protected LinkedList<HistoryItem> history = new LinkedList<HistoryItem>(); 
	
	
	private ToolHistory() {}
	
	public static ToolHistory getPenHistory() {
		return instance;
	}
	
	public int size() {
		return history.size();
	}
	
	public int getColor(int historyIndex) {
		return history.get(historyIndex).color;
	}

	public Tool getTool(int historyIndex) {
		return history.get(historyIndex).tool;
	}

	public int getThickness(int historyIndex) {
		return history.get(historyIndex).thickness;
	}
	
	public int getPreviousColor() {
		if (history.size() >= 2)
			return history.get(1).color;
		else
			return getNewestColor();
	}
	
	public Tool getPreviousTool() {
		if (history.size() >= 2)
			return history.get(1).tool;
		else
			return getNewestTool();
	}

	public int getPreviousThickness() {
		if (history.size() >= 2)
			return history.get(1).thickness;
		else
			return getNewestThickness();
	}

	public int getNewestColor() {
		return history.getFirst().color;
	}
	
	public Tool getNewestTool() {
		return history.getFirst().tool;
	}
	
	public int getNewestThickness() {
		return history.getFirst().thickness;
	}
	
	public class HistoryItem {
	
		public HistoryItem(Tool new_pen_type, int new_pen_thickness, int new_pen_color) {
			tool = new_pen_type;
			thickness = new_pen_thickness;
			color = new_pen_color;
		}
		
		protected Tool tool;
		protected int thickness;
		protected int color;
		
		public boolean equals(HistoryItem other) {
			return tool == other.tool && 
				thickness == other.thickness && 
				color == other.color;
		}
	}
	
	public static boolean add(Tool penType, int penThickness, int penColor) {
		historyItem = 0;
		ToolHistory instance = ToolHistory.getPenHistory();
		LinkedList<HistoryItem> history = instance.history;
		ListIterator<HistoryItem> iter = history.listIterator();
		while (iter.hasNext()) {
			HistoryItem h = iter.next();
			if (h.tool == penType && 
				h.thickness == penThickness && 
				h.color == penColor) {
				history.remove(h);
				history.addFirst(h);
				return false;
			}
		}
		HistoryItem item = ToolHistory.instance.new HistoryItem(penType, penThickness, penColor);
		history.addFirst(item);
		if (history.size() > ToolHistory.MAX_SIZE)
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

