package com.write.Quill.ImageEditor;

import android.app.Dialog;
import android.app.DialogFragment;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class DownloadImageFragment extends DialogFragment {
	private final static String TAG = "DownloadImageFragment";
	
	protected DownloadThread downloadThread;
    protected ProgressDialog progressDialog;
    
    protected Uri uri;

    private final static String ARGUMENT_SOURCE_URI = "sourceUri";
    
    public static DownloadImageFragment newInstance(Uri uri) {
        DownloadImageFragment frag = new DownloadImageFragment();
        Bundle args = new Bundle();
        args.putString(ARGUMENT_SOURCE_URI, uri.toString());
        frag.setArguments(args);
        return frag;
    }

    @Override
    public Dialog onCreateDialog(final Bundle savedInstanceState) {
    	String uriString = getArguments().getString(ARGUMENT_SOURCE_URI);
    	uri = Uri.parse(uriString);
        progressDialog = new ProgressDialog(getActivity());
        progressDialog.setTitle("Downloading...");
        progressDialog.setMessage("Image is stored in Picasa");
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(true);
      
        downloadThread = new DownloadThread(handler);
        downloadThread.start();

        return progressDialog;
    }

    // Define the Handler that receives messages from the thread and update the progress
    final Handler handler = new Handler() {
        public void handleMessage(Message msg) {
            int total = msg.arg1;
            progressDialog.setProgress(total);
            if (total >= 100){
                downloadThread.setState(DownloadThread.STATE_DONE);
            	dismiss();
            }
        }
    };

    
    /** Nested class that performs progress calculations (counting) */
    private class DownloadThread extends Thread {
        Handler handler;
        final static int STATE_DONE = 0;
        final static int STATE_RUNNING = 1;
        int mState;
        int total;
       
        DownloadThread(Handler handler) {
            this.handler = handler;
        }
       
        public void run() {
            mState = STATE_RUNNING;   
            total = 0;
            while (mState == STATE_RUNNING) {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    Log.e("ERROR", "Thread Interrupted");
                }
                Message msg = handler.obtainMessage();
                msg.arg1 = total;
                handler.sendMessage(msg);
                total++;
            }
        }
        
        public void setState(int state) {
            mState = state;
        }
        
        public void interrupt() {
        	setState(STATE_DONE);
        }
    }

}
