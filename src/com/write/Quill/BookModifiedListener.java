package com.write.Quill;

import name.vbraun.view.write.Page;

public interface BookModifiedListener {
	public void onPageInsertListener(Page page, int position);
	public void onPageDeleteListener(Page page, int position);
}
