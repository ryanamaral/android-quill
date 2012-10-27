package com.write.Quill.sync;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Http POST a set of key/value pairs and receive a JSON object
 * 
 * The JSON object must have { "msg":"string", "status":boolean } fields. 
 * 
 * @author vbraun
 * 
 */
public class HttpPostJson extends HttpPostBase {
	@SuppressWarnings("unused")
	private final static String TAG = "HttpPostJson";

	public HttpPostJson(String url) {
		super(url);
	}

	@Override
	protected Response processServerReply(HttpURLConnection connection) {
		return readJsonReply(connection);		
	}

}








