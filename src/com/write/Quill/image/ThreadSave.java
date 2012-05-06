package com.write.Quill.image;

import java.io.File;

public class ThreadSave extends ThreadBase {
	private final static String TAG = "ThreadSave";
	
	File input, output;
	
	protected ThreadSave(File input, File output) {
		super(output);
		this.input = input;
		this.output = output;
	}

	@Override
	protected void worker() {
		Util.copyfile(input, output);
	}

	
}
