package com.write.Quill.bookshelf;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.write.Quill.R;
import com.write.Quill.R.drawable;
import com.write.Quill.R.id;
import com.write.Quill.R.layout;
import com.write.Quill.R.string;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;

public class BookshelfAdapter extends ArrayAdapter<BookPreview> {
	private final static String TAG = "BookshelfAdapter";
	
	private Context context;
	
	public BookshelfAdapter(Context c) {
		super(c, R.layout.bookshelf_item, Bookshelf.getBookPreviewList());
		context = c;
	}
	
	@Override
	public int getCount() {
		return super.getCount()+1;
	}
	
	@Override
	public boolean hasStableIds() {
		return false;
	}
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View layout;
        if (convertView == null) {
            layout = LayoutInflater.from(context).inflate(
            		R.layout.bookshelf_item, parent, false);
        } else {
            layout = convertView;
        }
        TextView title = (TextView) layout.findViewById(R.id.bookshelf_title);
        TextView summary = (TextView) layout.findViewById(R.id.bookshelf_summary);
        ImageView thumb = (ImageView) layout.findViewById(R.id.bookshelf_image);
        
        if (position == Bookshelf.getBookPreviewList().size()) {
            title.setText(context.getString(R.string.edit_notebook_title_new));
            summary.setText("");
            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.icon_150);
            thumb.setImageBitmap(icon);
            return layout;
        }
        
        Bookshelf.BookPreview nb = Bookshelf.getBookPreviewList().get(position);
        title.setText(nb.getTitle());
        summary.setText(nb.getSummary());
        thumb.setImageBitmap(nb.getThumbnail(150, 150));
        return layout;
    }
	

}
