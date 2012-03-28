package com.write.Quill;

import java.util.Collection;
import java.util.LinkedList;

import com.write.Quill.data.Bookshelf;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.GraphicsControlpoint;
import name.vbraun.view.write.Page;
import name.vbraun.view.write.Stroke;

public class CommandClearPage extends Command {

	protected final LinkedList<Stroke> strokes = new LinkedList<Stroke>();
	protected final LinkedList<GraphicsControlpoint> lineArt = new LinkedList<GraphicsControlpoint>();

	public CommandClearPage(Page page) {
		super(page);
		strokes.addAll(page.strokes);
		lineArt.addAll(page.lineArt);
	}

	@Override
	public void execute() {
		UndoManager.getApplication().remove(getPage(), strokes);
		for (GraphicsControlpoint line : lineArt)
			UndoManager.getApplication().remove(getPage(), line);
	}

	@Override
	public void revert() {
		UndoManager.getApplication().add(getPage(), strokes);
		for (GraphicsControlpoint line : lineArt)
			UndoManager.getApplication().add(getPage(), line);
	}
	
	@Override
	public String toString() {
		int n = Bookshelf.getCurrentBook().getPageNumber(getPage());
		QuillWriterActivity app = UndoManager.getApplication();
		return app.getString(R.string.command_clear_page, n);
	}

}
