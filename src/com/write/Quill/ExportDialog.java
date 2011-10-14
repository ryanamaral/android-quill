package com.write.Quill;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;


public class ExportDialog 
	extends Dialog 
	implements OnClickListener, OnItemSelectedListener {

	private static final String TAG = "ExportDialog";

    private Context context;
	private View layout;
	private Page page;
	private Book book;
	private Handler handler = new Handler();
	private ProgressBar progressBar;
	private String filename;
	private Button exportButton;
	private PDFExporter pdfExporter;
	private Thread exportThread;
	private String fullFilename;
	private File file;
	private ArrayAdapter<CharSequence> exportSizes;
	
	public ExportDialog(Context context, int theme) {
        super(context, theme);
    }
 
    public ExportDialog(Context context) {
        super(context);
    }

    public static class Builder {
        private Context context;
        private Book book;
        Builder(Context c) {
        	context = c;
        }
        public Builder setBook(Book bk) {
        	book = bk;
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

			LinkedList<String> sizes_values = new LinkedList<String>();
        	dlg.exportSizes = new ArrayAdapter(context,
        			android.R.layout.simple_spinner_item, sizes_values);
 			String [] strings = context.getResources().getStringArray(R.array.export_size_vector);
 			dlg.exportSizes.addAll(strings);
        	dlg.exportSizes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        	Spinner sizes = (Spinner)layout.findViewById(R.id.export_size);
        	sizes.setAdapter(dlg.exportSizes);

        	Spinner format = (Spinner)layout.findViewById(R.id.export_file_format);
        	format.setOnItemSelectedListener(dlg);
        	
        	dlg.setContentView(layout);
        	dlg.layout = layout;
        	assert book != null : "You must call Builder.setBook()";
        	dlg.page = book.current_page();
        	dlg.book = book;
        	dlg.context = context;
        	dlg.progressBar = (ProgressBar)layout.findViewById(R.id.export_progress);
        	dlg.exportButton = ok;
        	return dlg;
       }
    } // Builder
    
    // Somebody clicked on Cancel, Export, Output format
	@Override
    public void onClick(View v) {
      	switch (v.getId()) {
    	case R.id.export_button:
    		doExport();
    		return;
    	case R.id.export_cancel:
    		doCancel();
    		return;
    	case R.id.export_file_format:
    		changeExportFileFormat((Spinner)v);
    		return;
    	}
    }

	@Override
	public void onItemSelected(AdapterView<?> spinner, View view, int position,
			long id) {
      	Spinner sizes = (Spinner)layout.findViewById(R.id.export_size);
      	Log.v(TAG, "Format "+position);
      	String[] strings;
      	switch (position) {
		case 0:  // PDF format
			sizes.setEnabled(true);
			strings = context.getResources().getStringArray(R.array.export_size_vector);
			exportSizes.clear();
			exportSizes.addAll(strings);
        	exportSizes.notifyDataSetChanged();
			return;
		case 1:  // Raster image format
			sizes.setEnabled(true);
			strings = context.getResources().getStringArray(R.array.export_size_raster);
			exportSizes.clear();
			exportSizes.addAll(strings);
        	exportSizes.notifyDataSetChanged();
			return;
		case 2:  // Quill backup archive
			sizes.setEnabled(false);
			exportSizes.clear();
        	exportSizes.notifyDataSetChanged();
			return;
		}		
	}

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		Log.v(TAG, "onNothingSelected");
	}
    
	private static final int OUTPUT_FORMAT_PDF = 0;
	private static final int OUTPUT_FORMAT_PNG = 1;
	private static final int OUTPUT_FORMAT_BACKUP = 2;

	
	// somebody changed the Output format
	private void changeExportFileFormat(Spinner format) {
      	Spinner sizes = (Spinner)layout.findViewById(R.id.export_size);
      	Log.v(TAG, "Format "+format.getSelectedItemPosition());
		switch (format.getSelectedItemPosition()) {
		case OUTPUT_FORMAT_PDF:
			sizes.setEnabled(true);
			sizes.setAdapter(new ArrayAdapter<String>(context, R.array.export_size_vector));
			return;
		case OUTPUT_FORMAT_PNG:
			sizes.setEnabled(true);
			sizes.setAdapter(new ArrayAdapter<String>(context, R.array.export_size_raster));
			return;
		case OUTPUT_FORMAT_BACKUP:
			sizes.setEnabled(false);
			return;
		}
	}

    protected void doExport() {
    	Log.v(TAG, "do_export()");
    	if (pdfExporter != null) return;
    	
    	TextView text = (TextView)layout.findViewById(R.id.export_name);
		filename = text.getText().toString();
		file = new File(context.getExternalFilesDir(null), filename);
		try {
			fullFilename = file.getCanonicalPath();
		} catch (IOException e) {
			Log.e(TAG, "Path does not exist: "+e.toString());
        	Toast.makeText(context, "Path does not exist", Toast.LENGTH_LONG).show();
			return;
		}
		try {
			file.createNewFile();
		} catch(IOException e) {
			Log.e(TAG, "Error creating file "+e.toString());
        	Toast.makeText(context, "Unable to create file "+fullFilename, Toast.LENGTH_LONG).show();
        	return;
        }
		
    	Spinner format = (Spinner)layout.findViewById(R.id.export_file_format);
    	switch (format.getSelectedItemPosition()) {
		case OUTPUT_FORMAT_PDF:
			doExportPdf();
			return;
		case OUTPUT_FORMAT_PNG:
			doExportPng();
			return;
		case OUTPUT_FORMAT_BACKUP:
			doExportArchive();
			return;
    	}
    }
    
    private void doExportArchive() {
    	try {
    		book.saveArchive(file);
    	} catch (IOException e) {
			Log.e(TAG, "Error writing file "+e.toString());
        	Toast.makeText(context, "Unable to write file "+fullFilename, Toast.LENGTH_LONG).show();   		
    	}
    }

    
    private void doExportPng() {
    	// TODO
    }
    
    private void doExportPdf() {
		threadLockDialog();
        assert pdfExporter == null : "Trying to run two export threads??";
    	pdfExporter = new PDFExporter();
        exportThread = new Thread(new Runnable() {
            public void run() {
            	pdfExporter.add(page);
            	pdfExporter.export(file);
            	pdfExporter = null;
            }});
        // exportThread.setPriority(Thread.MIN_PRIORITY);
        exportThread.start();
   }
    
    protected void doCancel() {
    	if (pdfExporter == null) {
    		dismiss();
    		threadUnlockDialog();
    	} else
    		pdfExporter.interrupt();
	}
    
    private void doShare() {
      	Spinner spinner = (Spinner)layout.findViewById(R.id.export_via);
    	int pos = spinner.getSelectedItemPosition();
    	switch (pos) {
    	case 0: // Generic share using Android intents
    		doShareGeneric();
    		return;
    	case 1: // Evernote
    		doShareEvernote();
    		return;
    	case 2: // SD card
        	Toast.makeText(context, 
    				context.getString(R.string.export_saved_as)+" "+fullFilename, 
    				Toast.LENGTH_LONG).show();
		return;
    	}
    }
    
    private void doShareGeneric() {
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

    private void doShareEvernote() {
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
    
    private void threadLockDialog() {
    	exportButton.setPressed(true);
        handler.post(mUpdateProgress);
    }
    
    private void threadUnlockDialog() {
    	progressBar.setProgress(0);
    	handler.removeCallbacks(mUpdateProgress);
    	exportButton.setPressed(false);
    }
    

    private Runnable mUpdateProgress = new Runnable() {
    	   public void run() {
    		   PDFExporter exporter = pdfExporter;
    		   if (exporter == null) {
    			   threadUnlockDialog();
    			   doShare();
    			   dismiss();
       		   return;
    		   }
    		   exportButton.setPressed(true);
    		   progressBar.setProgress(exporter.get_progress());
    		   progressBar.invalidate();
               handler.postDelayed(mUpdateProgress, 200);
    	   }
    	};
}
