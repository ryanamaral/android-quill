package com.write.Quill.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import com.write.Quill.R;
import com.write.Quill.R.string;
import com.write.Quill.data.Book;
import com.write.Quill.data.TagManager.Tag;
import com.write.Quill.data.TagManager.TagSet;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import name.vbraun.view.write.Page;

public class EvernoteExportDialog 
	extends ProgressDialog 
	implements DialogInterface.OnCancelListener,
		DialogInterface.OnClickListener {
	private final static String TAG = "EvernoteExportDialog";

	public EvernoteExportDialog(Context c) {
		super(c);
		context = c;   
		setIndeterminate(false);
		setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
		setTitle(R.string.export_evernote_title);
		setMessage(c.getString(R.string.export_evernote_message));
		setCancelable(true);
		setCanceledOnTouchOutside(true);
		setOnCancelListener(this);
		setButton(BUTTON_NEGATIVE, c.getString(android.R.string.cancel), this);
	}
	
    // Names of Evernote-specific Intent actions and extras
    public static final String ACTION_NEW_NOTE             = "com.evernote.action.CREATE_NEW_NOTE";
    public static final String EXTRA_NOTE_GUID             = "NOTE_GUID";
    public static final String EXTRA_SOURCE_APP            = "SOURCE_APP";
    public static final String EXTRA_AUTHOR                = "AUTHOR";
    public static final String EXTRA_QUICK_SEND            = "QUICK_SEND";
    public static final String EXTRA_TAGS                  = "TAG_NAME_LIST";
    
    private Context context;
	private Thread exportThread;
	private boolean cancel;
	private int progress;

    private Handler handler = new Handler(new Handler.Callback() {
        @Override
        public boolean handleMessage(Message msg) {
            String s = (String)msg.obj;
        	Toast.makeText(context, s, Toast.LENGTH_LONG).show();
            return false;
        }
    });
	
    private Book book;
    private LinkedList<Page> pages;
    private UUID uuid;
    TagSet tagSet;

    private ArrayList<File> fileList = new ArrayList<File>();
	private ArrayList<Uri> uriList = new ArrayList<Uri>();
    
	@Override
	public void onCancel(DialogInterface dialog) {
		cancel = true;
	}

	@Override
	public void onClick(DialogInterface dialog, int which) {
		if (which == BUTTON_NEGATIVE)
			cancel = true;		
	}

	@Override
	public void onStart() {
		fileList.clear();
		uriList.clear();
		cancel = false;
		progress = 0;
		super.onStart();
		doExport();
        handler.post(mUpdateProgress);
	}
	
	@Override
	protected void onStop() {
		super.onStop();
		handler.removeCallbacks(mUpdateProgress);
	}
	
    public void setPages(Book exportBook, LinkedList<Page> exportPages) {
    	book = exportBook;
    	pages = exportPages;
    	tagSet = book.getTagManager().newTagSet();
    	ListIterator<Page> iter = pages.listIterator();
    	while (iter.hasNext()) {
    		Page p = iter.next();
    		tagSet.add(p.getTags());
    	}
    	setMax(pages.size()+1);
    }
    
    public void setUUID(UUID id) {
    	uuid = id;
    }

    private int width = 800;
    private int height = 1280;
   
    private void renderPages() {
    	File dir = context.getExternalCacheDir();
    	if (dir == null) {
        	toast(R.string.export_evernote_err_no_cache_dir);
    		return;
    	}
    	ListIterator<Page> iter = pages.listIterator();
    	FileOutputStream outStream;
    	Log.d(TAG, dir.getAbsolutePath());
    	progress = 0;
    	while (iter.hasNext()) {
    		if (cancel) return;
    		UUID id = UUID.randomUUID();
    		File file = new File(dir, id.toString()+".png");
    		progress ++;
			Log.e(TAG, "Writing file "+file.toString());
			try {
    			outStream = new FileOutputStream(file);
    		} catch (IOException e) {
    			Log.e(TAG, "Error writing file "+e.toString());
            	toast(context.getString(R.string.export_evernote_err_cannot_write, file.toString()));
            	return;
    		}
			Page page = iter.next();
			int w, h;
			if (page.getAspectRatio() <= 1) {
				w = width; h = height;
			} else {
				w = height; h = width;
			}
        	Bitmap bitmap = page.renderBitmap(w, h, false);
        	bitmap.compress(CompressFormat.PNG, 0, outStream);
        	try {
        		outStream.close();
        	} catch (IOException e) {
    			Log.e(TAG, "Error closing file "+e.toString());
            	toast(context.getString(R.string.export_evernote_err_cannot_close, file.toString()));
            	return;     		
        	}
        	file.deleteOnExit();
    		fileList.add(file);
    		uriList.add(Uri.fromFile(file));
    	}
    }
    
    private void doExport() {
        exportThread = new Thread(new Runnable() {
            public void run() {
            	renderPages();
            	if (cancel)
            		toast(R.string.export_evernote_cancel_message);
            	else
            		send();
            	dismiss();
            }});
        exportThread.start();
    }
    	
    private void send() {
        Intent intent = new Intent();
        intent.setAction(ACTION_NEW_NOTE);
        intent.putExtra(Intent.EXTRA_TITLE, book.getTitle());
        // intent.putExtra(Intent.EXTRA_TEXT, text);

        // Add tags, which will be created if they don't exist
        ArrayList<String> tags = new ArrayList<String>();
        ListIterator<Tag> iter = tagSet.tagIterator();
        while (iter.hasNext())
        	tags.add(iter.next().toString());
        Collections.sort(tags);
        intent.putExtra(EXTRA_TAGS, tags);
        if (uuid != null) {
        	String notebookGuid = uuid.toString();
        	intent.putExtra(EXTRA_NOTE_GUID, notebookGuid);
        }
        
        intent.putExtra(EXTRA_SOURCE_APP, "Quill");
        intent.putExtra(EXTRA_QUICK_SEND, true);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM , uriList);
        try {
        	context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
        	toast(context.getString(R.string.export_evernote_err_not_found)); 
        } 
    }


    private void toast(String s) {
        Message msg = handler.obtainMessage(0, s);
        handler.sendMessage(msg);
    }

    private void toast(int resId) {
        Message msg = handler.obtainMessage(0, context.getString(resId));
        handler.sendMessage(msg);
    }


    private Runnable mUpdateProgress = new Runnable() {
 	   public void run() {
 		   setProgress(progress);
 		   handler.postDelayed(mUpdateProgress, 100);
 	   }
 	};
	
}
