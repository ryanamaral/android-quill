package com.write.Quill.artist;

public class PaperType {
	private final static String TAG = "PaperType";
	
	public enum PageSize {
		 LETTER, LEGAL, A3, A4, A5, B4, B5, EXECUTIVE, US4x6, US4x8, US5x7, COMM10
	}
	public enum PageDirection {
		PORTRAIT, LANDSCAPE
	}

	protected final PageSize pageSize;
	protected final PageDirection pageDirection;
	
	public PaperType(PageSize pageSize) {
		this.pageSize = pageSize;
		this.pageDirection = PageDirection.PORTRAIT;
	}

	public PaperType(PageSize pageSize, PageDirection pageDirection) {
		this.pageSize = pageSize;
		this.pageDirection = pageDirection;
	}
	
	public PageSize getPageSize() {
		return pageSize;
	}
	
	public PageDirection getPageDirection() {
		return pageDirection;
	}
	
}
