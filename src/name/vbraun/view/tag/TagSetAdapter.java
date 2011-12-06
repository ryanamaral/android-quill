package name.vbraun.view.tag;



import com.write.Quill.R;
import com.write.Quill.R.layout;
import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class TagSetAdapter extends ArrayAdapter {
	
	private static final String TAG = "TagSetAdapter";
	
	private TagSet tags;
	private Context context;
	private int highlight = Color.YELLOW;

	public TagSetAdapter(Context mContext, TagSet active_tags) {
		super(mContext, R.layout.tag_item, 
					active_tags.allTags());
		tags = active_tags;
		context = mContext;
		
	}

	public void setHighlightColor(int color) {
		highlight = color;
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
        tv.setText(t.toString());
        if (tags.contains(t)) {
        	tv.setShadowLayer(20, 0, 0, highlight);
        	tv.setTypeface(Typeface.DEFAULT_BOLD);
        } else {
        	tv.setShadowLayer(0, 0, 0, highlight);
        	tv.setTypeface(Typeface.DEFAULT);
	
        }
        return tv;
    }
	
}

