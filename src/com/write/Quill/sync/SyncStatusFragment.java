package com.write.Quill.sync;

import com.write.Quill.R;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

public class SyncStatusFragment extends Fragment {
	private final static String TAG = "SyncStatusFragment";
	
	private ProgressBar progress;
	private TextView syncStatusBig, syncStatus;
	private TextView nameTextView, emailTextView;
	
    public static SyncStatusFragment newInstance() {
        SyncStatusFragment frag = new SyncStatusFragment();
        Bundle args = new Bundle();
        frag.setArguments(args);
        return frag;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
    		Bundle savedInstanceState) {
    	View layout = inflater.inflate(R.layout.sync_status_fragment, container);
		syncStatusBig = (TextView) layout.findViewById(R.id.sync_status_big);
		syncStatus = (TextView) layout.findViewById(R.id.sync_status);
		nameTextView = (TextView) layout.findViewById(R.id.sync_name);
		emailTextView = (TextView) layout.findViewById(R.id.sync_email);
		progress = (ProgressBar) layout.findViewById(R.id.sync_progress);
		return layout;
    }

    public void setAccount(QuillAccount account) {
		nameTextView.setText(account.name());
		emailTextView.setText(account.email());
    }
    
    public void setStatus(String title, String status, boolean finished) {
    	if (title != null)
    		syncStatusBig.setText(title);
    	if (status != null)
    		syncStatus.setText(status);
    	progress.setVisibility(finished ? View.INVISIBLE : View.VISIBLE);
    }
    
}
