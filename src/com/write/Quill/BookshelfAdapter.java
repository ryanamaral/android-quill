package com.write.Quill;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.Notebook;

public class BookshelfAdapter extends ArrayAdapter<Notebook> {

	Context context;
	
	public BookshelfAdapter(Context c) {
		super(c, R.layout.bookshelf_item, Bookshelf.getNotebookList());
		context = c;
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
        
        Bookshelf.Notebook nb = Bookshelf.getNotebookList().get(position);
        title.setText(nb.getTitle());
        summary.setText(nb.getSummary());
        thumb.setImageBitmap(nb.getThumbnail(150, 150));
        return layout;
    }
	

}
