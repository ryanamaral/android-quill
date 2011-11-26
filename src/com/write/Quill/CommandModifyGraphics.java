package com.write.Quill;

import name.vbraun.view.write.Graphics;
import name.vbraun.view.write.Page;

public class CommandModifyGraphics extends Command {

	protected final Graphics graphicsOld, graphicsNew;
	
	public CommandModifyGraphics(Page page, Graphics toErase, Graphics toReCreate) {
		super(page);
		graphicsOld = toErase;
		graphicsNew = toReCreate;
	}

	@Override
	public void execute() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void revert() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String toString() {
		// TODO Auto-generated method stub
		return null;
	}
	
}
