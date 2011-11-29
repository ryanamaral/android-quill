package com.write.Quill;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.UUID;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.widget.Toast;

import name.vbraun.view.write.Page;
import name.vbraun.view.write.TagManager.Tag;
import name.vbraun.view.write.TagManager.TagSet;

public class EvernoteExporter {
	private final static String TAG = "EvernoteExporter";
	
    // Names of Evernote-specific Intent actions and extras
    public static final String ACTION_NEW_NOTE             = "com.evernote.action.CREATE_NEW_NOTE";
    public static final String EXTRA_NOTE_GUID             = "NOTE_GUID";
    public static final String EXTRA_SOURCE_APP            = "SOURCE_APP";
    public static final String EXTRA_AUTHOR                = "AUTHOR";
    public static final String EXTRA_QUICK_SEND            = "QUICK_SEND";
    public static final String EXTRA_TAGS                  = "TAG_NAME_LIST";

    private Book book;
    private LinkedList<Page> pages;
    private UUID uuid;
    TagSet tagSet;

    private ArrayList<File> fileList = new ArrayList<File>();
	private ArrayList<Uri> uriList = new ArrayList<Uri>();
    
    public EvernoteExporter(Book exportBook, LinkedList<Page> exportPages) {
    	book = exportBook;
    	pages = exportPages;
    	tagSet = book.getTagManager().newTagSet();
    	ListIterator<Page> iter = pages.listIterator();
    	while (iter.hasNext()) {
    		Page p = iter.next();
    		tagSet.add(p.getTags());
    	}
    }
    
    public void setUuid(UUID id) {
    	uuid = id;
    }

    int width = 800;
    int height = 800;
    
    
    private void renderPages(Context context) {
    	File dir = Environment.getExternalStorageDirectory();
    	ListIterator<Page> iter = pages.listIterator();
    	FileOutputStream outStream;
    	while (iter.hasNext()) {
    		File file = new File(dir, iter.toString()+".png");
			Log.e(TAG, "Writing file "+file.toString());
			try {
    			outStream = new FileOutputStream(file);
    		} catch (IOException e) {
    			Log.e(TAG, "Error writing file "+e.toString());
            	Toast.makeText(context, "Unable to write file "+file.toString(), Toast.LENGTH_LONG).show();
            	return;
    		}
        	Bitmap bitmap = iter.next().renderBitmap(width, height);
        	bitmap.compress(CompressFormat.PNG, 0, outStream);
        	try {
        		outStream.close();
        	} catch (IOException e) {
    			Log.e(TAG, "Error closing file "+e.toString());
            	Toast.makeText(context, "Unable to close file "+file.toString(), Toast.LENGTH_LONG).show();
            	return;     		
        	}
    		fileList.add(file);
    		uriList.add(Uri.fromFile(file));
    	}
    }
    
    
    public void doExport(Activity activity) {
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
        
     //   intent.putExtra(EXTRA_QUICK_SEND, true);
    
        renderPages(activity.getApplicationContext());
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM , uriList);
        try {
        	activity.startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
        	Toast.makeText(activity, activity.getString(R.string.err_evernote_not_found), 
        			Toast.LENGTH_LONG).show();
        } 
        cleanUp();
    }

    
    private void cleanUp() {
    	ListIterator<File> iter = fileList.listIterator();
    	while (iter.hasNext()) {
    		File f = iter.next();
    		f.delete();
    	}
    }
}
