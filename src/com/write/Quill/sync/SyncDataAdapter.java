package com.write.Quill.sync;

import java.util.List;
import java.util.UUID;

import com.write.Quill.R;
import com.write.Quill.data.Bookshelf;
import com.write.Quill.data.Bookshelf.BookPreview;
import com.write.Quill.data.Storage;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.format.Time;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SyncDataAdapter extends ArrayAdapter<SyncData.SyncItem> {
	private final static String TAG = "SyncDataAdapter";
	
	private final SyncData data;
	
	public SyncDataAdapter(Context context, int textViewResourceId, SyncData syncData) {
		super(context, textViewResourceId, syncData.data);
		data = syncData;
	}
	
	public static class ViewHolder {
		RelativeLayout layout;
		TextView title, subtitle;
		ImageView android, status, earth;

		public ViewHolder(View v) {
			layout = (RelativeLayout) v;
			title = (TextView) v.findViewById(R.id.sync_item_title);
			subtitle = (TextView) v.findViewById(R.id.sync_item_subtitle);
			android = (ImageView) v.findViewById(R.id.sync_icon_android);
			status = (ImageView) v.findViewById(R.id.sync_icon_status);
			earth = (ImageView) v.findViewById(R.id.sync_icon_earth);
		}
		
		public void show(SyncData.SyncItem item) {
			title.setText(item.getTitle());
			String s = new String();
			Storage storage = Storage.getInstance();
			if (item.local) {
				 s += "Local copy last edited ";
				 s += storage.formatDateTime(item.localTime.toMillis(false)) + "\n";
			}
			if (item.remote) {
				s += "Most recent remote backup on ";
				s += storage.formatDateTime(item.remoteTime.toMillis(false)) + "\n";
			}
			subtitle.setText(s);
			switch (item.getState()) {
			case CONFLICT:
				layout.setBackgroundColor(Color.argb(0x7f, 0xff, 0x0, 0x0));
				status.setImageResource(R.drawable.ic_sync_lightning);
				status.setVisibility(View.VISIBLE);
				earth.setVisibility(View.VISIBLE);
				break;
			case LOCAL_IS_NEWER:
				layout.setBackgroundColor(Color.argb(0x7f, 0x0, 0xff, 0x0));
				status.setImageResource(R.drawable.ic_sync_arrow_left);
				status.setVisibility(View.VISIBLE);
				earth.setVisibility(View.VISIBLE);
				break;
			case REMOTE_IS_NEWER:
				layout.setBackgroundColor(Color.argb(0x7f, 0x0, 0xff, 0x0));
				status.setImageResource(R.drawable.ic_sync_arrow_right);
				status.setVisibility(View.VISIBLE);
				earth.setVisibility(View.VISIBLE);
				break;
			case IN_SYNC:
				layout.setBackgroundColor(Color.TRANSPARENT);
				status.setVisibility(View.INVISIBLE);
				earth.setVisibility(View.VISIBLE);
				break;
			case LOCAL_ONLY:
				layout.setBackgroundColor(Color.TRANSPARENT);
				status.setVisibility(View.INVISIBLE);
				earth.setVisibility(View.INVISIBLE);
				break;
			}
		}
	}
	
	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		ViewHolder holder;
		if (v == null) {
			LayoutInflater inflater =
					(LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			v = inflater.inflate(R.layout.sync_item, null);
			holder = new ViewHolder(v);
			v.setTag(holder);
		} else {
			holder = (ViewHolder)v.getTag();
		}
		holder.show(data.get(position));
		return v;	
	}
	
	public void reset() {
		data.reset();
		notifyDataSetChanged();
	}
		
	public void addRemote(UUID uuid, String title, Time mtime) {
		data.addRemote(uuid, title, mtime);
		notifyDataSetChanged();
	}


}
