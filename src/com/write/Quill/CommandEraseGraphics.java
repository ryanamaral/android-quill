package com.write.Quill;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.Page;

public class CommandEraseGraphics extends Command {

	protected final Graphics graphics;
	
	public CommandEraseGraphics(Page page, Graphics toAdd) {
		super(page);
		graphics = toAdd;
	}

	@Override
	public void execute() {
		getPage().remove(graphics);
	}

	@Override
	public void revert() {
		getPage().add(graphics);
	}
	
}
