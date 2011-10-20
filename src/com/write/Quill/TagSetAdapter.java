package com.write.Quill;

import com.write.Quill.TagManager.Tag;
import com.write.Quill.TagManager.TagSet;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TagSetAdapter extends ArrayAdapter 
	implements ListView.OnItemClickListener {
	
	private static final String TAG = "TagSetAdapter";
	
	private TagSet tags;
	private TagManager tagManager = TagManager.getTagManager();
	private Context context;
	
	public TagSetAdapter(Context mContext, TagSet active_tags) {
		super(mContext, R.layout.tag_item, 
					active_tags.allTags());
		tags = active_tags;
		context = mContext;
	}

	
	public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		Log.v(TAG, "onItemClick");
		Tag t = tagManager.get(position);
		if (tags.contains(t))
			tags.remove(t);
		else
			tags.add(t);
		Log.d(TAG, "Click: "+tags.size());
		notifyDataSetChanged();
	}
	
	
	@Override
    public View getView(int position, View convertView, ViewGroup parent) {
        TextView tv;
        if (convertView == null) {
            tv = (TextView) LayoutInflater.from(context).inflate(
                    R.layout.tag_item, parent, false);
        } else {
            tv = (TextView) convertView;
        }
        Tag t = tags.allTags().get(position);
        tv.setText(t.name);
        if (tags.contains(t)) {
        	tv.setShadowLayer(10, 0, 0, Color.BLUE);
        	tv.setTypeface(Typeface.DEFAULT_BOLD);
        }
        return tv;
    }
	
}

