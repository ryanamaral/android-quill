package com.write.Quill.image;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

public class ThreadPicasaDownload extends ThreadBase {
	private final static String TAG = "ThreadPicasaDownload";
	
	private int total = 0;
	
    private InputStream input;
    private OutputStream output;
    
	private byte[] buf = new byte[1024];

	protected ThreadPicasaDownload(InputStream input, File file) {
		super(file);
		this.input = input;
	}
	
	@Override
	protected int getProgress() {
		return total;
	}

	@Override
	protected void worker() {
        total = 0;
    	open();
    	while (!isInterrupted()) {
    		debuggingDelay();
    		int len = 0;
			try {
				len = input.read(buf);
				if (len >= 0)
					output.write(buf, 0, len);
				else
					break;
				total += len;
			} catch (IOException e) {
                Log.e(TAG, e.getMessage());
			}          	
    		// Log.v(TAG, "Got "+len+" bytes");
        }
        close();

	}

	
    private void open() {
        try {
			output = new FileOutputStream(file);
		} catch (FileNotFoundException e) {
            Log.e(TAG, e.getMessage());
		}
    }
    
    private void close() {
    	try {
    		input.close();
    		output.close();
    	} catch (IOException e) {
    		Log.e(TAG, e.getMessage());
    	}	
    }
    
}
