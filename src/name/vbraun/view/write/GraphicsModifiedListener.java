package name.vbraun.view.write;

public interface GraphicsModifiedListener {
	public void onGraphicsCreateListener(Page page, Graphics toAdd);
	public void onGraphicsModifyListener(Page page, Graphics toRemove, Graphics toReplaceWith);
	public void onGraphicsEraseListener(Page page, Graphics toErase);
	
	public void onPageClearListener(Page page);
}
