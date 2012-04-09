package com.write.Quill.thumbnail;

import java.util.Hashtable;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;

import com.write.Quill.TagOverlay;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;

import name.vbraun.view.write.Page;

import junit.framework.Assert;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;


public class ThumbnailAdapter extends BaseAdapter {
	private static final String TAG = "ThumbnailAdapter";

	protected static final int MIN_THUMBNAIL_WIDTH = 200;
	protected int thumbnail_width = MIN_THUMBNAIL_WIDTH;

	private Context context;

	
	public ThumbnailAdapter(Context c) {
		context = c;
    	computeItemHeights();
	}

    @Override
    public int getCount() {
        return Bookshelf.getCurrentBook().filteredPagesSize();
    }

    @Override
    public Object getItem(int position) {
        Book book = Bookshelf.getCurrentBook();
        Page page = book.getFilteredPage(book.filteredPagesSize() - 1 - position);
        return page;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
    	return false;
    }
    
	protected Paint paint = new Paint();
	protected int[] heightOfItem = null;

    protected class Thumbnail extends View {
    	private static final String TAG = "Thumbnail";
    	protected int position;
    	protected Bitmap bitmap;
    	protected Page page;
    	protected TagOverlay tagOverlay = null;
    	protected boolean checked = false;
    	
		public Thumbnail(Context context) {
			super(context);	
		}

		public int heightOfRow() {
			int row = position / numColumns;
			int maxHeight = heightOfItem[row*numColumns];
			for (int i=row*numColumns; i<(row+1)*numColumns && i<heightOfItem.length; i++)
				maxHeight = Math.max(maxHeight, heightOfItem[i]);
			return maxHeight;
		}
		
		@Override
    	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
			// Log.d(TAG, "onMeasure "+position+" "+heightOfRow());
    		super.onMeasure(widthMeasureSpec, heightMeasureSpec);
			setMeasuredDimension(thumbnail_width, heightOfRow());
    	}
		
		@Override
		protected void onDraw(Canvas canvas) {
			if (bitmap == null) {
				canvas.drawColor(Color.DKGRAY);
	        	tagOverlay.draw(canvas);
				return;
			}
			// Log.d(TAG, "Thumb "+position+" "+getHeight()+" "+bitmap.getHeight());
			float y = (getHeight()-bitmap.getHeight())/2;
			canvas.drawBitmap(bitmap, 0, y, paint);
	        Boolean checked = selectedPages.get(page);
	        tagOverlay.draw(canvas);
	        if (checked != null && checked == true)
	        	canvas.drawARGB(0x50, 0, 0xff, 0);
		}
		
    }
    
    private int numColumns = 1;
    
    public void setNumColumns(int n) {
    	numColumns = n;
    }
    
    protected void computeItemHeights() {
    	Bookshelf.getCurrentBook().filterChanged();
    	LinkedList<Page> pages = Bookshelf.getCurrentBook().getFilteredPages();
    	heightOfItem = new int[pages.size()];
    	ListIterator<Page> iter = pages.listIterator();
    	int pos = pages.size()-1;
    	while (iter.hasNext()) 
    		heightOfItem[pos--] = (int)(thumbnail_width / iter.next().getAspectRatio());
    }
    	
    
    IdentityHashMap<Page, Boolean> selectedPages = new IdentityHashMap<Page, Boolean>();
    LinkedList<Thumbnail> unfinishedThumbnails = new LinkedList<Thumbnail>();
    
    protected boolean renderThumbnail() {
    	if (unfinishedThumbnails.isEmpty()) return false;
    	Thumbnail thumb = unfinishedThumbnails.pop();
    	Page page = thumb.page;
		thumb.bitmap = page.renderBitmap(thumbnail_width, 2*thumbnail_width, true);
		Assert.assertTrue(thumb.bitmap != null);
		thumb.invalidate();
		return true;
    }
    
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
		Thumbnail thumb;
        if (convertView == null) {
            thumb = new Thumbnail(context);
        } else {
            thumb = (Thumbnail) convertView;
            if (thumb.position == position)
            	return thumb;
            if (thumb.bitmap != null)
            	thumb.bitmap.recycle();
        }
        Book book = Bookshelf.getCurrentBook();
        // Log.d(TAG, "getView "+position+" "+book.filteredPagesSize());
        Page page = book.getFilteredPage(book.filteredPagesSize() - 1 - position);
        thumb.page = page;
        thumb.position = position;
        thumb.bitmap = null;
        thumb.tagOverlay = new TagOverlay(context, page.tags, true);
        thumb.requestLayout();     
        unfinishedThumbnails.add(thumb);
        ThumbnailView grid = (ThumbnailView)parent;
        grid.postIncrementalDraw();
		return thumb;
	}
	
    public void checkedStateChanged(int position, boolean checked) {
        Book book = Bookshelf.getCurrentBook();
    	Page page = book.getFilteredPage(book.filteredPagesSize() - 1 - position);
    	selectedPages.put(page, checked);
    }
	
    public void uncheckAll() {
    	selectedPages.clear();
    }

}
