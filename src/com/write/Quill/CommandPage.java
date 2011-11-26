package com.write.Quill;

import name.vbraun.view.write.Page;

public class CommandPage extends Command {

	private int position;
	private boolean insertOrErase;
	
	public CommandPage(Page page, int pos, boolean insert) {
		super(page);
		position = pos;
		insertOrErase = insert;
	}
	
	@Override
	public void execute() {
		if (insertOrErase)
			UndoManager.getApplication().add(getPage(), position);
		else
			UndoManager.getApplication().remove(getPage(), position);
	}

	@Override
	public void revert() {
		if (insertOrErase)
			UndoManager.getApplication().remove(getPage(), position);
		else
			UndoManager.getApplication().add(getPage(), position);
	}

	@Override
	public String toString() {
		if (insertOrErase)
			return "Insert page number "+position;
		else
			return "Delete page number "+position;
	}
	
}
