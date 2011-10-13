package com.write.Quill;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;

public class ExportDialog extends Dialog implements OnClickListener {
	private static final String TAG = "ExportDialog";

    private Context context;
	private View layout;
	private Page page;
	private Handler handler = new Handler();
	private ProgressBar progress_bar;
	private String filename;
	private Button export_button;
	private PDFExporter pdf_exporter;
	private Thread export_thread;
	private String fullFilename;
	private File file;
	
	public ExportDialog(Context context, int theme) {
        super(context, theme);
    }
 
    public ExportDialog(Context context) {
        super(context);
    }

    public static class Builder {
        private Context context;
        private Page page;
        Builder(Context c) {
        	context = c;
        }
        public Builder setPage(Page p) {
        	page = p;
        	return this;
        }
        public ExportDialog create() {
        	ExportDialog dlg = new ExportDialog(context);
        	dlg.setTitle(R.string.export_title);
        	LayoutInflater inflater = (LayoutInflater) 
        		context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        	View layout = inflater.inflate(R.layout.export, null);
        	Button ok = (Button)layout.findViewById(R.id.export_button);
        	ok.setOnClickListener(dlg);
        	Button cancel = (Button)layout.findViewById(R.id.export_cancel);
        	cancel.setOnClickListener(dlg);
        	dlg.setContentView(layout);
        	dlg.layout = layout;
        	assert page != null : "You must call Builder.setPage()";
        	dlg.page = page;
        	dlg.context = context;
        	dlg.progress_bar = (ProgressBar)layout.findViewById(R.id.export_progress);
        	dlg.export_button = ok;
        	return dlg;
       }
    } // Builder
    
    public void onClick(View v) {
    	switch (v.getId()) {
    	case R.id.export_button:
    		do_export();
    		return;
    	case R.id.export_cancel:
    		do_cancel();
    		return;
    	}
    }

    protected void do_export() {
    	Log.v(TAG, "do_export()");

    	TextView text = (TextView)layout.findViewById(R.id.export_name);
		filename = text.getText().toString();
		file = new File(context.getExternalFilesDir(null), filename);
		try {
			fullFilename = file.getCanonicalPath();
		} catch (IOException e) {
			Log.e(TAG, "Path does note exist: "+e.toString());
			return;
		}

		thread_lock_dialog();
        assert pdf_exporter == null : "Trying to run two export threads??";
    	pdf_exporter = new PDFExporter();
        export_thread = new Thread(new Runnable() {
            public void run() {
            	pdf_exporter.add(page);
            	pdf_exporter.export(file);
            	pdf_exporter = null;
            }});
        // export_thread.setPriority(Thread.MIN_PRIORITY);
        export_thread.start();
   }
    
    protected void do_cancel() {
    	if (pdf_exporter == null) {
    		dismiss();
    		thread_unlock_dialog();
    	} else
    		pdf_exporter.interrupt();
	}
    
    private void do_share() {
      	Spinner spinner = (Spinner)layout.findViewById(R.id.export_via);
    	int pos = spinner.getSelectedItemPosition();
    	switch (pos) {
    	case 0: // SD card
    		do_share_generic();
    		return;
    	case 1: // Evernote
    		do_share_evernote();
    		return;
    	case 2: // generic
    		do_share_generic();
        	Toast.makeText(context, 
    				context.getString(R.string.export_saved_as)+" "+fullFilename, 
    				Toast.LENGTH_LONG).show();
		return;
    	}
    }
    
    private void do_share_generic() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        try {
            context.startActivity(Intent.createChooser(intent, 
            		context.getString(R.string.export_share_title)));
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(context, 
            		context.getString(R.string.err_no_way_to_share),
                    Toast.LENGTH_LONG).show();
        }
    	
    }
    
    // Names of Evernote-specific Intent actions and extras
    public static final String ACTION_NEW_NOTE             = "com.evernote.action.CREATE_NEW_NOTE";
    public static final String EXTRA_NOTE_GUID             = "NOTE_GUID";
    public static final String EXTRA_SOURCE_APP            = "SOURCE_APP";
    public static final String EXTRA_AUTHOR                = "AUTHOR";
    public static final String EXTRA_QUICK_SEND            = "QUICK_SEND";
    public static final String EXTRA_TAGS                  = "TAG_NAME_LIST";

    private void do_share_evernote() {
        Intent intent = new Intent();
        intent.setAction(ACTION_NEW_NOTE);
        intent.putExtra(Intent.EXTRA_TITLE, filename);
        // intent.putExtra(Intent.EXTRA_TEXT, text);

        // Add tags, which will be created if they don't exist
        ArrayList<String> tags = new ArrayList<String>();
        tags.add("Quill");
        tags.add("Android");
        intent.putExtra(EXTRA_TAGS, tags);
        
        // If we knew the GUID of a notebook that we wanted to put the new note in, we could set it here
        //String notebookGuid = "d7c41948-f4aa-46e1-a818-e6ff73877145";
        //intent.putExtra(EXTRA_NOTE_GUID, notebookGuid);
        
        // Set the note's author, souceUrl and sourceApplication attributes.
        // To learn more, see
        // http://www.evernote.com/about/developer/api/ref/Types.html#Struct_NoteAttributes
        // intent.putExtra(EXTRA_AUTHOR, "");
        intent.putExtra(EXTRA_SOURCE_APP, "Quill");
        
        // If you set QUICK_SEND to true, Evernote for Android will automatically "save"
        // the new note. The user will see the "New note" activity briefly, then
        // return to your application.
        //	intent.putExtra(EXTRA_QUICK_SEND, true);
        
        // Add file(s) to be attached to the note
        ArrayList<Uri> uriList = new ArrayList<Uri>();
        uriList.add(Uri.fromFile(file));
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM , uriList);
        try {
        	context.startActivity(intent);
        } catch (android.content.ActivityNotFoundException ex) {
        	Toast.makeText(context, R.string.err_evernote_not_found, Toast.LENGTH_LONG).show();
        } 

    }
    
    private void thread_lock_dialog() {
    	export_button.setPressed(true);
        handler.post(mUpdateProgress);
    }
    
    private void thread_unlock_dialog() {
    	progress_bar.setProgress(0);
    	handler.removeCallbacks(mUpdateProgress);
    	export_button.setPressed(false);
    }
    

    private Runnable mUpdateProgress = new Runnable() {
    	   public void run() {
    		   PDFExporter exporter = pdf_exporter;
    		   if (exporter == null) {
    			   thread_unlock_dialog();
    			   do_share();
    			   dismiss();
       		   return;
    		   }
    		   export_button.setPressed(true);
    		   progress_bar.setProgress(exporter.get_progress());
    		   progress_bar.invalidate();
               handler.postDelayed(mUpdateProgress, 200);
    	   }
    	};
}
