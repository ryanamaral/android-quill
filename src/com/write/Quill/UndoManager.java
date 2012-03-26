package com.write.Quill;

import java.util.LinkedList;

import android.content.Context;

import junit.framework.Assert;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.GraphicsModifiedListener;
import name.vbraun.view.write.Page;

/**
 * The command manager. Every change to the notebook has to go through 
 * the command manager in the form of a Command class that knows how to
 * undo/redo itself. 
 * 
 * The undo manager is a singlet and currently operates only within a 
 * single notebook. If you change the notebook you must call 
 * {@link #clearHistory()}.  
 * 
 * @author vbraun
 *
 */
public class UndoManager 
	implements GraphicsModifiedListener, BookModifiedListener {
	private final static String TAG = "UndoManager";
	
	private UndoManager() {};
	private final static UndoManager instance = new UndoManager();
	public static UndoManager getUndoManager() { return instance; }
	
	protected LinkedList<Command> undoStack = new LinkedList<Command>();
	protected LinkedList<Command> redoStack = new LinkedList<Command>();
	
	private QuillWriterActivity main;
	
	public static void setApplication(QuillWriterActivity app) {
		getUndoManager().main = app;
	}
	
	protected static QuillWriterActivity getApplication() {
		QuillWriterActivity result = getUndoManager().main;
		Assert.assertNotNull(result);
		return result;
	}
	
	@Override
	public void onGraphicsCreateListener(Page page, Graphics toAdd) {
		Command cmd = new CommandCreateGraphics(page, toAdd);
		undoStack.addFirst(cmd);
		redoStack.clear();
		limitStackSize();
		cmd.execute();
	}

	@Override
	public void onGraphicsModifyListener(Page page, Graphics toRemove,
			Graphics toReplaceWith) {
		Command cmd = new CommandModifyGraphics(page, toRemove, toReplaceWith);
		undoStack.addFirst(cmd);
		redoStack.clear();
		limitStackSize();
		cmd.execute();
	}

	@Override
	public void onGraphicsEraseListener(Page page, Graphics toErase) {
		Command cmd = new CommandEraseGraphics(page, toErase);
		undoStack.addFirst(cmd);
		redoStack.clear();
		limitStackSize();
		cmd.execute();
	}
	
	public void onPageClearListener(Page page) {
		Command cmd = new CommandClearPage(page);
		undoStack.addFirst(cmd);
		redoStack.clear();
		limitStackSize();
		cmd.execute();	
	}

	public void onPageInsertListener(Page page, int position) {
		Command cmd = new CommandPage(page, position, true);
		undoStack.addFirst(cmd);
		redoStack.clear();
		limitStackSize();
		cmd.execute();	
	}
	
	public void onPageDeleteListener(Page page, int position) {
		Command cmd = new CommandPage(page, position, false);
		undoStack.addFirst(cmd);
		redoStack.clear();
		limitStackSize();
		cmd.execute();	
	}



	private static final int MAX_STACK_SIZE = 50;
	
	private void limitStackSize() {
		while (undoStack.size()>MAX_STACK_SIZE)
			undoStack.removeLast();
		while (redoStack.size()>MAX_STACK_SIZE)
			redoStack.removeLast();				
	}
	
	
	public boolean undo() {
		Command cmd = undoStack.pollFirst();
		if (cmd == null)
			return false;
		QuillWriterActivity activity = getApplication();
		activity.toast(activity.getString(R.string.quill_undo) + ": "+cmd.toString());
		cmd.revert();
		redoStack.addFirst(cmd);
		return true;
	}
	
	public boolean redo() {
		Command cmd = redoStack.pollFirst();
		if (cmd == null)
			return false;
		cmd.execute();
		QuillWriterActivity activity = getApplication();
		activity.toast(activity.getString(R.string.quill_redo) + ": "+cmd.toString());
		undoStack.addFirst(cmd);
		return true;	
	}
	
	public boolean haveUndo() {
		return !undoStack.isEmpty();
	}
	
	public boolean haveRedo() {
		return !redoStack.isEmpty();
	}
	
	public void clearHistory() {
		undoStack.clear();
		redoStack.clear();
	}
}
