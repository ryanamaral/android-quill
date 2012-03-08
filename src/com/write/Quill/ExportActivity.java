package com.write.Quill;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.Format;
import java.util.ArrayList;
import java.util.LinkedList;

import name.vbraun.view.write.Page;

import org.libharu.Document.CompressionMode;
import org.libharu.Page.PageSize;

import com.write.Quill.R;
import com.write.Quill.data.Book;
import com.write.Quill.data.Bookshelf;

import junit.framework.Assert;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;


public class ExportActivity 
	extends ActivityBase
	implements OnClickListener, OnItemSelectedListener {

	private static final String TAG = "ExportActivity";
	
	// Tag for the Intent extra data carrying the book
	protected static final String INTENT_EXTRA_BOOK = "Quill_Book"; 
	
	private View layout;
	private Book book;
	private Page page;
	private Handler handler = new Handler();
	private ProgressBar progressBar;
	private String filename;
	private String mimeType;
	private Button exportButton;
	private PDFExporter pdfExporter = null;
	private Thread exportThread;
	private String fullFilename;
	private File file;
	private FileOutputStream outStream = null;
	private Spinner format, sizes, via;
	private ArrayAdapter<CharSequence> exportSizes;
	private TextView name;
	private CheckBox backgroundCheckbox;
	
	volatile private int size_raster_width, size_raster_height;
	volatile private boolean drawBackground;
	
	private String getFilenameFromIntent() {
		String s = getIntent().getExtras().getString("filename");
		String[] lines = s.split("\\n");
		if (lines.length > 0) {
			s = lines[0];
			s = s.replaceAll("\\P{Alnum}", "_");
		} else
			s = "Filename";
		return s;	
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        book = Bookshelf.getCurrentBook();
        page = book.currentPage();
        
    	LayoutInflater inflater = getLayoutInflater();
    	
    	layout = inflater.inflate(R.layout.export, null);
    	exportButton = (Button)layout.findViewById(R.id.export_button);
    	exportButton.setOnClickListener(this);
    	Button cancel = (Button)layout.findViewById(R.id.export_cancel);
    	cancel.setOnClickListener(this);

		LinkedList<String> sizes_values = new LinkedList<String>();
    	exportSizes = new ArrayAdapter(this,
    			android.R.layout.simple_spinner_item, sizes_values);
		String [] strings = getResources().getStringArray(R.array.export_size_vector);
		exportSizes.addAll(strings);
    	exportSizes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    	sizes = (Spinner)layout.findViewById(R.id.export_size);
    	sizes.setAdapter(exportSizes);
    	sizes.setOnItemSelectedListener(this);

    	name = (TextView)layout.findViewById(R.id.export_name);
    	
    	format = (Spinner)layout.findViewById(R.id.export_file_format);
    	format.setOnItemSelectedListener(this);

    	via = (Spinner)layout.findViewById(R.id.export_via);

    	progressBar = (ProgressBar)layout.findViewById(R.id.export_progress);
    	backgroundCheckbox = (CheckBox) layout.findViewById(R.id.export_background);
    	setContentView(layout);
	}
	
    
    // Somebody clicked on Cancel, Export
	@Override
    public void onClick(View v) {
      	switch (v.getId()) {
    	case R.id.export_button:
    		doExport();
    		return;
    	case R.id.export_cancel:
    		doCancel();
    		return;
    	}
    }

	void changeFileExtensionTo(String ext) {
		String txt = name.getText().toString();
		int dot = txt.lastIndexOf('.');
		if (dot == -1 || dot == 0) 
			txt = txt + ext;
		else
			txt = txt.substring(0, dot) + ext;
		name.setText(txt);
	}	
	
	@Override
	public void onItemSelected(AdapterView<?> spinner, View view, int position,
			long id) {
		if (spinner == format)
			onItemSelectedFormat(position);
		else if (spinner == sizes)
			onItemSelectedSizes(position);
	}
		
		
	/**
	 * The callback for changes in the format spinner
	 * @param position
	 */
	public void onItemSelectedFormat(int position) {
		// Log.v(TAG, "Format "+position);
      	String[] strings;
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        int sizesPos;
      	switch (position) {
		case OUTPUT_FORMAT_PDF_SINGLE:  // PDF format
		case OUTPUT_FORMAT_PDF_TAGGED:  
		case OUTPUT_FORMAT_PDF_ALL:     
			backgroundCheckbox.setEnabled(true);
			sizes.setEnabled(true);
			strings = getResources().getStringArray(R.array.export_size_vector);
			exportSizes.clear();
			exportSizes.addAll(strings);
        	exportSizes.notifyDataSetChanged();
            sizesPos = settings.getInt("export_size_pdf", AdapterView.INVALID_POSITION);
            if (sizesPos !=  AdapterView.INVALID_POSITION)
            	sizes.setSelection(sizesPos); 
        	changeFileExtensionTo(".pdf");
        	mimeType = "application/pdf";
			return;
		case OUTPUT_FORMAT_PNG:  // Raster image format
			backgroundCheckbox.setEnabled(true);
			sizes.setEnabled(true);
			strings = getResources().getStringArray(R.array.export_size_raster);
			exportSizes.clear();
			exportSizes.addAll(strings);
        	exportSizes.notifyDataSetChanged();
            sizesPos = settings.getInt("export_size_png", AdapterView.INVALID_POSITION);
            if (sizesPos !=  AdapterView.INVALID_POSITION)
            	sizes.setSelection(sizesPos); 
        	changeFileExtensionTo(".png");
        	mimeType = "image/png";
			return;
		case OUTPUT_FORMAT_BACKUP:  // Quill backup archive
			backgroundCheckbox.setEnabled(false);
			sizes.setEnabled(false);
			exportSizes.clear();
        	exportSizes.notifyDataSetChanged();
        	changeFileExtensionTo(".quill");
        	mimeType = "application/vnd.name.vbraun.quill";
			return;
		}		
	}
	
	/**
	 * The callback for changes in the sizes spinner
	 * @param position
	 */
	public void onItemSelectedSizes(int position) {
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor;
      	switch (format.getSelectedItemPosition()) {
		case OUTPUT_FORMAT_PDF_SINGLE:  // PDF format
		case OUTPUT_FORMAT_PDF_TAGGED:  
		case OUTPUT_FORMAT_PDF_ALL:
			editor = settings.edit();
			editor.putInt("export_size_pdf", position);
			editor.commit();
			return;
		case OUTPUT_FORMAT_PNG:  // Raster image format
			editor = settings.edit();
			editor.putInt("export_size_png", position);
			editor.commit();
			return;
		}		
    }

	@Override
	public void onNothingSelected(AdapterView<?> arg0) {
		Log.v(TAG, "onNothingSelected");
	}
    
	private static final int OUTPUT_FORMAT_PNG = 0;
	private static final int OUTPUT_FORMAT_PDF_SINGLE = 1;
	private static final int OUTPUT_FORMAT_PDF_TAGGED = 2;
	private static final int OUTPUT_FORMAT_PDF_ALL= 3;
	private static final int OUTPUT_FORMAT_BACKUP = 4;

	
//	// somebody changed the Output format
//	private void changeExportFileFormat(Spinner format) {
//      	Spinner sizes = (Spinner)layout.findViewById(R.id.export_size);
//      	Log.v(TAG, "Format "+format.getSelectedItemPosition());
//		switch (format.getSelectedItemPosition()) {
//		case OUTPUT_FORMAT_PDF_SINGLE:
//		case OUTPUT_FORMAT_PDF_TAGGED:
//		case OUTPUT_FORMAT_PDF_ALL:
//			sizes.setEnabled(true);
//			sizes.setAdapter(new ArrayAdapter<String>(this, R.array.export_size_vector));
//			return;
//		case OUTPUT_FORMAT_PNG:
//			sizes.setEnabled(true);
//			sizes.setAdapter(new ArrayAdapter<String>(this, R.array.export_size_raster));
//			return;
//		case OUTPUT_FORMAT_BACKUP:
//			sizes.setEnabled(false);
//			return;
//		}
//	}

	private enum PageRange {
		CURRENT_PAGE, TAGGED_PAGES, ALL_PAGES
	}
	
    protected void doExport() {
    	Log.v(TAG, "doExport()");
    	if (pdfExporter != null) return;
    	if (!openShareFile()) return;
    	Spinner format = (Spinner)layout.findViewById(R.id.export_file_format);
    	switch (format.getSelectedItemPosition()) {
		case OUTPUT_FORMAT_PDF_SINGLE:
			doExportPdf(PageRange.CURRENT_PAGE);
			return;
		case OUTPUT_FORMAT_PDF_TAGGED:
			doExportPdf(PageRange.TAGGED_PAGES);
			return;
		case OUTPUT_FORMAT_PDF_ALL:
			doExportPdf(PageRange.ALL_PAGES);
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
    		Bookshelf.getBookshelf().exportCurrentBook(file);
    	} catch (Book.BookSaveException e) {
			Log.e(TAG, "Error writing file "+e.getMessage());
        	Toast.makeText(this, "Unable to write file "+fullFilename, Toast.LENGTH_LONG).show();
        	return;
    	}
    	doShare();
    }

	private static final int SIZE_RASTER_1920 = 0;
	private static final int SIZE_RASTER_1280 = 1;
	private static final int SIZE_RASTER_1024 = 2;
	private static final int SIZE_RASTER_800= 3;

    private void doExportPng() {
		threadLockActivity();
		drawBackground = backgroundCheckbox.isChecked();
		Log.d(TAG, "drawBackground = "+drawBackground);
		int pos = sizes.getSelectedItemPosition();
		int dim_big = 0, dim_small = 0;
		switch (pos) {
			case SIZE_RASTER_1920:  dim_big = 1920; dim_small = 1080; break;
			case SIZE_RASTER_1280:  dim_big = 1280; dim_small = 800; break;
			case SIZE_RASTER_1024:  dim_big = 1024; dim_small = 768; break;
			case SIZE_RASTER_800:   dim_big =  800; dim_small = 600; break;
			default: Assert.assertTrue("Unreachable", false);
    	}
		if (page.getAspectRatio() > 1) {
			size_raster_width = dim_big;    size_raster_height = dim_small;
		} else {
			size_raster_width = dim_small;  size_raster_height = dim_big;
		}
    	try {
			outStream = new FileOutputStream(file);
		} catch (IOException e) {
			Log.e(TAG, "Error writing file "+e.toString());
        	Toast.makeText(this, "Unable to write file "+fullFilename, Toast.LENGTH_LONG).show();
        	return;
		}
        exportThread = new Thread(new Runnable() {
            public void run() {
            	Bitmap bitmap = page.renderBitmap(size_raster_width, size_raster_height, drawBackground);
            	bitmap.compress(CompressFormat.PNG, 0, outStream);
            	try {
            		outStream.close();
            	} catch (IOException e) {
        			Log.e(TAG, "Error closing file "+e.toString());
            	}
            	outStream = null;
            }});
        exportThread.start();
    }
    
    private static final int SIZE_PDF_A4 = 0;
    private static final int SIZE_PDF_LETTER = 1;
    private static final int SIZE_PDF_LEGAL = 2;
    
    private void doExportPdf(PageRange range) {
		threadLockActivity();
        Assert.assertTrue("Trying to run two export threads??",  pdfExporter == null);
    	pdfExporter = new PDFExporter();
    	pdfExporter.setCompressionMode(CompressionMode.COMP_ALL);
		int pos = sizes.getSelectedItemPosition();
		switch (pos) {
			case SIZE_PDF_A4:     pdfExporter.setPageSize(PageSize.A4); break;
			case SIZE_PDF_LETTER: pdfExporter.setPageSize(PageSize.LETTER); break;
			case SIZE_PDF_LEGAL:  pdfExporter.setPageSize(PageSize.LEGAL); break;
			default: Assert.assertTrue("Unreachable", false);
    	}
    	switch (range) {
    	case CURRENT_PAGE:     	pdfExporter.add(page); break;
    	case TAGGED_PAGES:     	pdfExporter.add(book.getFilteredPages()); break;
    	case ALL_PAGES:      	pdfExporter.add(book.getPages()); break;
    	}
        exportThread = new Thread(new Runnable() {
            public void run() {
            	pdfExporter.draw();
            	pdfExporter.export(file);
            	pdfExporter.destructAll();
            	pdfExporter = null;
            }});
        // exportThread.setPriority(Thread.MIN_PRIORITY);
        exportThread.start();
   }
    
    protected void doCancel() {
    	if (pdfExporter == null) {
    		finish();
    		threadUnlockActivity();
    	} else
    		pdfExporter.interrupt();
	}
    
    private static final int SHARE_GENERIC = 0;
    private static final int SHARE_EVERNOTE = 1;
    private static final int SHARE_EXTERNAL = 2;
    private static final int SHARE_INTERNAL = 3;
    private static final int SHARE_USB = 4;

    
    private void doShare() {
    	int pos = via.getSelectedItemPosition();
    	switch (pos) {
    	case SHARE_GENERIC:
    		doShareGeneric();
    		return;
    	case SHARE_EVERNOTE:
    		doShareEvernote();
    		return;
    	case SHARE_EXTERNAL:
    	case SHARE_INTERNAL:
    	case SHARE_USB:
        	Toast.makeText(this, getString(R.string.export_saved_as)+" "+fullFilename, 
    				Toast.LENGTH_LONG).show();
    		doShareView();
        	finish();
		return;
    	}
    }
    
    private boolean openShareFile() {
      	Spinner spinner = (Spinner)layout.findViewById(R.id.export_via);
    	int pos = spinner.getSelectedItemPosition();
		filename = name.getText().toString();
		if (filename.startsWith("/"))
    		file = new File(filename);
		else
			switch (pos) {
			case SHARE_GENERIC:
			case SHARE_EVERNOTE:
				file = new File(getExternalFilesDir(null), filename);
				break;
			case SHARE_INTERNAL:
				// file = new File(getExternalFilesDir(null), filename);
				file = new File("/mnt/sdcard", filename);
				break;
			case SHARE_EXTERNAL:
				file = new File("/mnt/external_sd", filename);
				break;
			case SHARE_USB:
				file = new File("/mnt/usbdrive", filename);
				break;
			}
		try {
			fullFilename = file.getCanonicalPath();
		} catch (IOException e) {
			Log.e(TAG, "Path does not exist: "+e.toString());
        	Toast.makeText(this, "Path does not exist", Toast.LENGTH_LONG).show();
			return false;
		}
		try {
			file.createNewFile();
		} catch(IOException e) {
			Log.e(TAG, "Error creating file "+e.toString());
        	Toast.makeText(this, "Unable to create file "+fullFilename, Toast.LENGTH_LONG).show();
        	return false;
        }
		return true;
    }
    
    /**
     * Send file to other app
     */
    private void doShareGeneric() {
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        try {
            startActivity(Intent.createChooser(intent, 
            		getString(R.string.export_share_title)));
            finish();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, getString(R.string.err_no_way_to_share), Toast.LENGTH_LONG).show();
        }    	
    }
    
    /**
     * View the resulting file after saving
     */
    private void doShareView() {
    	if (format.getSelectedItemPosition() == OUTPUT_FORMAT_BACKUP) return;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), mimeType);        
        try {
            startActivity(Intent.createChooser(intent, 
            		getString(R.string.export_view_file_title)));
            finish();
        } catch (android.content.ActivityNotFoundException ex) {
        	// ignore silently
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
        	startActivity(intent);
        	finish();
        } catch (android.content.ActivityNotFoundException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Evernote application not found");
            builder.setMessage("Download from Android Market?");
            builder.setPositiveButton("Yes",
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=com.evernote"));
                            startActivity(marketIntent);
                        }
                    });
            builder.setNegativeButton("No", null);
            builder.create().show();
        } 
    }
            
    private void threadLockActivity() {
    	exportButton.setPressed(true);
        handler.post(mUpdateProgress);
    }
    
    private void threadUnlockActivity() {
    	progressBar.setProgress(0);
    	handler.removeCallbacks(mUpdateProgress);
    	exportButton.setPressed(false);
    }
    

    private Runnable mUpdateProgress = new Runnable() {
    	   public void run() {
    		   boolean isFinished = true;
    		   isFinished &= (pdfExporter == null);
    		   isFinished &= (outStream == null);
    		   if (isFinished) {
    			   threadUnlockActivity();
    			   doShare();
       		   return;
    		   }
    		   exportButton.setPressed(true);
    		   if (pdfExporter != null)
    			   progressBar.setProgress(pdfExporter.get_progress());
    		   progressBar.invalidate();
               handler.postDelayed(mUpdateProgress, 200);
    	   }
    	};
    
    	
    @Override
    protected void onResume() {
    	super.onResume();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
    	String nameString = getFilenameFromIntent();
    	if (nameString == null)
    		nameString = settings.getString("export_name", null);
    	if (nameString != null)
    		name.setText(nameString);
        int formatPos = settings.getInt("export_file_format", AdapterView.INVALID_POSITION);
        if (formatPos !=  AdapterView.INVALID_POSITION)
        	format.setSelection(formatPos);
        int viaPos = settings.getInt("export_via", AdapterView.INVALID_POSITION);
        if (viaPos !=  AdapterView.INVALID_POSITION)
        	via.setSelection(viaPos);        
        backgroundCheckbox.setChecked(settings.getBoolean("export_background", true));
    }

    @Override
    protected void onPause() {
    	super.onPause();
        SharedPreferences settings= PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("export_file_format", format.getSelectedItemPosition());
      	switch (format.getSelectedItemPosition()) {
		case OUTPUT_FORMAT_PDF_SINGLE:  // PDF format
		case OUTPUT_FORMAT_PDF_TAGGED:  
		case OUTPUT_FORMAT_PDF_ALL:     
	        editor.putInt("export_size_pdf", sizes.getSelectedItemPosition());
			break;
		case OUTPUT_FORMAT_PNG:  // Raster image format
	        editor.putInt("export_size_png", sizes.getSelectedItemPosition());
			break;
      	}		
        editor.putInt("export_via", via.getSelectedItemPosition());
        editor.putString("export_name", name.getText().toString());
        editor.putBoolean("export_background", backgroundCheckbox.isChecked());
        editor.commit();
    };
    
}
