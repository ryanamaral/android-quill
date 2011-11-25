package com.write.Quill;

import java.util.LinkedList;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.GraphicsModifiedListener;
import name.vbraun.view.write.Page;

public class UndoManager implements GraphicsModifiedListener {
	private final static String TAG = "UndoManager";
	
	protected LinkedList<Command> undoStack = new LinkedList<Command>();
	protected LinkedList<Command> redoStack = new LinkedList<Command>();
	
	
	
	@Override
	public void onGraphicsCreateListener(Page page, Graphics toAdd) {
		Command cmd = new CommandCreateGraphics(page, toAdd);
		cmd.execute();
		undoStack.addFirst(cmd);
	}

	@Override
	public void onGraphicsModifyListener(Page page, Graphics toRemove,
			Graphics toReplaceWith) {
		Command cmd = new CommandModifyGraphics(page, toRemove, toReplaceWith);
		cmd.execute();
		undoStack.addFirst(cmd);
	}

	@Override
	public void onGraphicsEraseListener(Page page, Graphics toErase) {
		Command cmd = new CommandEraseGraphics(page, toErase);
		cmd.execute();
		undoStack.addFirst(cmd);
	}

}
