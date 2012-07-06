package com.write.Quill.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.util.Log;

public class ThreadSave extends ThreadBase {
	private final static String TAG = "ThreadSave";
	
	private final File input, output;
	private final int rotation;
	private final Rect crop;

	protected ThreadSave(File input, File output, int rotation, Rect crop) {
		super(output);
		this.input = input;
		this.output = output;
		this.rotation = rotation;
		this.crop = crop;
		Log.d(TAG, "Saving "+input.getAbsolutePath()+" -> "+output.getAbsolutePath());
	}

	@Override
	protected void worker() {
		if (input.equals(output) && rotation == 0 && crop == null) return;
		
		Bitmap bitmap = BitmapFactory.decodeFile(input.getPath());
		if (isInterrupted()) return;

		bitmap = Util.rotateAndCrop(bitmap, rotation, crop);
		if (isInterrupted()) return;

		OutputStream out = openOutput();
        try {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, out);
        } catch (Exception e) {
            Log.e(TAG, "Compression and/or save failed. " + e.getMessage());
        }
        closeOutput(out);
        
        Log.d(TAG, "Saved to "+output.getPath());
	}
	
    private OutputStream openOutput() {
        try {
			return new FileOutputStream(output);
		} catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
		}
        return null;
    }
    
    private void closeOutput(OutputStream outputStream) {
    	try {
    		outputStream.close();
    	} catch (IOException e) {
    		Log.e(TAG, e.getMessage());
    	}	
    }

}
