package com.write.Quill;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.Page;

public class CommandCreateGraphics extends Command {

	protected final Graphics graphics;
	
	public CommandCreateGraphics(Page page, Graphics toAdd) {
		super(page);
		graphics = toAdd;
	}

	@Override
	public void execute() {
		getPage().add(graphics);
	}

	@Override
	public void revert() {
		getPage().remove(graphics);
	}
	
}
