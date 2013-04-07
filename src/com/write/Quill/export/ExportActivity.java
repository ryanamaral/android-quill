package com.write.Quill.export;

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

import com.write.Quill.ActivityBase;
import com.write.Quill.PDFExporter;
import com.write.Quill.R;
import com.write.Quill.R.array;
import com.write.Quill.R.id;
import com.write.Quill.R.layout;
import com.write.Quill.R.string;
import com.write.Quill.artist.PaperType;
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
import android.os.Environment;
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
	private String mimeType;
	private Button exportButton;
	private PDFExporter pdfExporter = null;
	private Thread exportThread;
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
    	
    	checkConstants();
	}
	
	public ExportActivity() {
		if (DIRECTORY_SDCARD == null) 
			DIRECTORY_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();
		if (DIRECTORY_EXTERNALSD == null) 
			DIRECTORY_EXTERNALSD = tryDirectories(DIRECTORY_EXTERNALSD_TRY);
		if (DIRECTORY_USBDRIVE == null) 
			DIRECTORY_USBDRIVE   = tryDirectories(DIRECTORY_USBDRIVE_TRY);
	}

	private String tryDirectories(String[] dirs) {
		for (String name : dirs) {
			File dir = new File(name);
			if (dir.exists())
				return name;
		}
		return dirs[0];
	}
	
	private static final String[] DIRECTORY_EXTERNALSD_TRY = {"/mnt/external_sd", "/mnt/sdcard2", "/mnt/sdcard/external_sd", "/mnt/extSdCard"};
	private static final String[] DIRECTORY_USBDRIVE_TRY   = {"/mnt/usbdrive", "/mnt/usb", "/mnt/usb0", "/mnt/sdcard/usbStorage", "/mnt/UsbDriveA", "/mnt/UsbDriveB"};
	private static String DIRECTORY_SDCARD     = null;
	private static String DIRECTORY_EXTERNALSD = null;
	private static String DIRECTORY_USBDRIVE   = null;
	
	private void checkConstants() {
		String[] export_via_values = getResources().getStringArray(R.array.export_via_values);
		Assert.assertEquals(export_via_values[SHARE_GENERIC],  "EXPORT_VIA_APP");
		Assert.assertEquals(export_via_values[SHARE_EVERNOTE], "EXPORT_VIA_EVERNOTE");
		Assert.assertEquals(export_via_values[SHARE_PICK_DIR], "EXPORT_VIA_PICK");
		Assert.assertEquals(export_via_values[SHARE_EXTERNAL], "EXPORT_VIA_EXTERNALSD");
		Assert.assertEquals(export_via_values[SHARE_INTERNAL], "EXPORT_VIA_SDCARD");
		Assert.assertEquals(export_via_values[SHARE_USB],      "EXPORT_VIA_USB");
		String[] export_file_values = getResources().getStringArray(R.array.export_file_values);
		Assert.assertEquals(export_file_values[OUTPUT_FORMAT_PNG],        "EXPORT_FILE_PNG");
		Assert.assertEquals(export_file_values[OUTPUT_FORMAT_PDF_SINGLE], "EXPORT_FILE_PDF_PAGE");
		Assert.assertEquals(export_file_values[OUTPUT_FORMAT_PDF_TAGGED], "EXPORT_FILE_PDF_TAGGED");
		Assert.assertEquals(export_file_values[OUTPUT_FORMAT_PDF_ALL],    "EXPORT_FILE_PDF_ALL");
		Assert.assertEquals(export_file_values[OUTPUT_FORMAT_BACKUP],     "EXPORT_FILE_QUILL");
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
	public void onItemSelected(AdapterView<?> spinner, View view, int position,	long id) {
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
		// Log.e(TAG, "Format "+position);
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

	
	private enum PageRange {
		CURRENT_PAGE, TAGGED_PAGES, ALL_PAGES
	}
	
    protected void doExport() {
    	Log.v(TAG, "doExport()");
    	if (pdfExporter != null) return;
    	File file = openShareFile();
    	doExport(file);
    }
    	
    private static final int REQUEST_CODE_PICK_DIRECTORY = 1;
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	super.onActivityResult(requestCode, resultCode, data);
    	switch (requestCode) {
    	case REQUEST_CODE_PICK_DIRECTORY:
    		if (resultCode != RESULT_OK || data == null) return; 
    		Uri dirUri = data.getData();
    		if (dirUri == null) return;
    		File file = new File(dirUri.getPath(), name.getText().toString());
    		file = checkShareFile(file);
    		doExport(file);
    		break;
    	}
    }

    private File openShareFile() {
      	Spinner spinner = (Spinner)layout.findViewById(R.id.export_via);
    	int pos = spinner.getSelectedItemPosition();
    	File file = null;
		String filename = name.getText().toString();
		if (filename.startsWith("/"))
    		file = new File(filename);
		else
			switch (pos) {
			case SHARE_GENERIC:
			case SHARE_EVERNOTE:
				file = new File(getExternalFilesDir(null), filename);
				file.deleteOnExit();
				break;
			case SHARE_PICK_DIR:
	    		Intent intent = new Intent(getApplicationContext(), name.vbraun.filepicker.FilePickerActivity.class);
	    		intent.setAction("org.openintents.action.PICK_DIRECTORY");
	    		intent.putExtra("org.openintents.extra.TITLE", getString(R.string.export_pick_destination_directory));
	    		startActivityForResult(intent, REQUEST_CODE_PICK_DIRECTORY);
	    		break;
			case SHARE_INTERNAL:
				file = new File(DIRECTORY_SDCARD, filename);
				break;
			case SHARE_EXTERNAL:
				file = new File(DIRECTORY_EXTERNALSD, filename);
				break;
			case SHARE_USB:
				file = new File(DIRECTORY_USBDRIVE, filename);
				break;
			default:
				Assert.fail("unreachable");
			}
		return checkShareFile(file);
    }
    
	/**
	 * Creates a new file and catches any errors.
	 * @param file 
	 * @return A new File or null in case of error.
	 */
	private File checkShareFile(File file) {
		if (file == null) return null;
		File parent = file.getParentFile();
		if (parent!=null && !parent.exists()) {
			Log.e(TAG, "Path does not exist: "+parent.toString());
        	Toast.makeText(this, R.string.export_err_path_does_not_exist, Toast.LENGTH_LONG).show();
			return null;
		}
		try {
			file.createNewFile();
		} catch(IOException e) {
			Log.e(TAG, "Error creating file "+e.toString());
        	Toast.makeText(this, getString(R.string.export_err_cannot_create_file)
        			+" "+file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        	return null;
        }
		return file;
    }

    
    private void doExport(File file) {
    	if (file == null || !file.exists()) return;
    	Spinner format = (Spinner)layout.findViewById(R.id.export_file_format);
    	switch (format.getSelectedItemPosition()) {
		case OUTPUT_FORMAT_PDF_SINGLE:
			doExportPdf(file, PageRange.CURRENT_PAGE);
			return;
		case OUTPUT_FORMAT_PDF_TAGGED:
			doExportPdf(file, PageRange.TAGGED_PAGES);
			return;
		case OUTPUT_FORMAT_PDF_ALL:
			doExportPdf(file, PageRange.ALL_PAGES);
			return;
		case OUTPUT_FORMAT_PNG:
			doExportPng(file);
			return;
		case OUTPUT_FORMAT_BACKUP:
			doExportArchive(file);
			return;
    	}
    }
    
    private void doExportArchive(File file) {
    	try {
    		Bookshelf.getBookshelf().exportCurrentBook(file);
    	} catch (Book.BookSaveException e) {
			Log.e(TAG, "Error writing file "+e.getMessage());
        	Toast.makeText(this, getString(R.string.export_err_cannot_write_file)
        			+" "+file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        	return;
    	}
    	doShare(file);
    }

	private static final int SIZE_RASTER_1920 = 0;
	private static final int SIZE_RASTER_1280 = 1;
	private static final int SIZE_RASTER_1024 = 2;
	private static final int SIZE_RASTER_800= 3;

    private void doExportPng(final File file) {
		threadLockActivity();
		drawBackground = backgroundCheckbox.isChecked();
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
        	Toast.makeText(this, getString(R.string.export_err_cannot_write_file)
        			+" "+file.getAbsolutePath(), Toast.LENGTH_LONG).show();
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
            	doShareInMainThread(file);
            }});
        exportThread.start();
    }
    
    private static final int SIZE_PDF_A4 = 0;
    private static final int SIZE_PDF_LETTER = 1;
    private static final int SIZE_PDF_LEGAL = 2;
    
    private void doExportPdf(final File file, PageRange range) {
		threadLockActivity();
		drawBackground = backgroundCheckbox.isChecked();
        Assert.assertTrue("Trying to run two export threads??",  pdfExporter == null);
        PaperType paper = null;
        int pos = sizes.getSelectedItemPosition();
		switch (pos) {
			case SIZE_PDF_A4:     paper = new PaperType(PaperType.PageSize.A4); break;
			case SIZE_PDF_LETTER: paper = new PaperType(PaperType.PageSize.LETTER); break;
			case SIZE_PDF_LEGAL:  paper = new PaperType(PaperType.PageSize.LEGAL); break;
			default: Assert.assertTrue("Unreachable", false);
    	}
    	pdfExporter = new PDFExporter(paper, file);
    	pdfExporter.setBackgroundVisible(drawBackground);
    	switch (range) {
    	case CURRENT_PAGE:     	pdfExporter.add(page); break;
    	case TAGGED_PAGES:     	pdfExporter.add(book.getFilteredPages()); break;
    	case ALL_PAGES:      	pdfExporter.add(book.getPages()); break;
    	}
        exportThread = new Thread(new Runnable() {
            public void run() {
            	pdfExporter.draw();
            	pdfExporter.destroy();
            	pdfExporter = null;
            	doShareInMainThread(file);
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
    private static final int SHARE_PICK_DIR = 2;
    private static final int SHARE_EXTERNAL = 3;
    private static final int SHARE_INTERNAL = 4;
    private static final int SHARE_USB = 5;

    
    private void doShareInMainThread(final File file) {
    	runOnUiThread(new Runnable() {
    		@Override
    		public void run() {
    			doShare(file);
    		}
    	});
    }
    
    /**
     * To be called after the exported file has been written to storage
     * @param file
     */
    private void doShare(File file) {
    	int pos = via.getSelectedItemPosition();
    	switch (pos) {
    	case SHARE_GENERIC:
    		doShareGeneric(file);
    		return;
    	case SHARE_EVERNOTE:
    		doShareEvernote(file);
    		return;
    	case SHARE_PICK_DIR:
    	case SHARE_EXTERNAL:
    	case SHARE_INTERNAL:
    	case SHARE_USB:
        	Toast.makeText(this, getString(R.string.export_saved_as)+" "+file.getAbsolutePath(), 
    				Toast.LENGTH_LONG).show();
    		doShareView(file);
        	finish();
		return;
    	}
    }
    
    
    /**
     * Send file to other app
     */
    private void doShareGeneric(File file) {
    	Uri uri = Uri.fromFile(file);
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType(mimeType);
        intent.putExtra(Intent.EXTRA_STREAM, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        try {
            startActivity(Intent.createChooser(intent, 
            		getString(R.string.export_share_title)));
            finish();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.export_err_no_way_to_share, Toast.LENGTH_LONG).show();
        }    	
    }
    
    /**
     * View the resulting file after saving
     */
    private void doShareView(File file) {
    	if (format.getSelectedItemPosition() == OUTPUT_FORMAT_BACKUP) return;
        Intent intent = new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), mimeType);        
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
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

    private void doShareEvernote(File file) {
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
        intent.setAction(ACTION_NEW_NOTE);
        intent.putExtra(Intent.EXTRA_TITLE, file.getAbsolutePath());
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
        Uri uri = Uri.fromFile(file);
        ArrayList<Uri> uriList = new ArrayList<Uri>();
        uriList.add(uri);
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM , uriList);
        try {
        	startActivity(intent);
        	finish();
        } catch (android.content.ActivityNotFoundException ex) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.export_evernote_err_not_found);
            builder.setMessage(R.string.export_evernote_download_question);
            builder.setPositiveButton(android.R.string.yes,
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent marketIntent = new Intent(Intent.ACTION_VIEW);
                            marketIntent.setData(Uri.parse("market://details?id=com.evernote"));
                            startActivity(marketIntent);
                        }
                    });
            builder.setNegativeButton(android.R.string.no, null);
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
