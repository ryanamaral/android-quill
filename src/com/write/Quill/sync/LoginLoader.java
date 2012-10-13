package com.write.Quill.sync;

import java.io.PrintWriter;
import java.util.Timer;

import android.content.Context;
import android.util.Log;

public class LoginLoader extends HttpLoader {
	private final static String TAG = "LoginLoader";

	protected final String email;
	protected final String password;
	
	public LoginLoader(Context context, String email, String password) {
		super(context);
		this.email = email;
		this.password = password;
	}

	@Override
	protected String getServerPath() {
		return "_login";
	}
	
	@Override
	protected void writePostRequest(PrintWriter writer) {
		writePostRequestPart(writer, "email", charsetUTF8, email);
		writePostRequestPart(writer, "password", charsetUTF8, password);
	}

}
