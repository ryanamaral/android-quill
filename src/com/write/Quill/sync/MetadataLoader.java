package com.write.Quill.sync;

import java.io.PrintWriter;
import java.util.Timer;

import android.content.Context;
import android.util.Log;

public class MetadataLoader extends HttpLoader {
	private final static String TAG = "MetadataLoader";

	protected final String email;
	protected final String sessionToken;
	
	public MetadataLoader(Context context, String email, String sessionToken) {
		super(context);
		this.email = email;
		this.sessionToken = sessionToken;
	}

	@Override
	protected String getServerPath() {
		return "_metadata";
	}
	
	@Override
	protected void writePostRequest(PrintWriter writer) {
		writePostRequestPart(writer, "email", charsetUTF8, email);
		writePostRequestPart(writer, "session_token", charsetUTF8, sessionToken);

		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}

