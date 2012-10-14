package com.write.Quill.sync;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import org.json.JSONException;
import org.json.JSONObject;

import android.util.Pair;

/**
 * Http POST a set of key/value pairs and receive a JSON object
 * 
 * The JSON object must have { "msg":"string", "status":boolean } fields. 
 * 
 * @author vbraun
 * 
 */
public class HttpPostJson {
	private final static String TAG = "HttpPostJson";
	
	/**
	 * The server response 
	 */
	public static class Response {
		private final boolean success;
		private final String msg;
		private final int code;
		private final JSONObject json;
		public final static int ERROR = -1;

		public Response(String errorMsg) {
			this.success = false;
			this.msg = errorMsg;
			this.code = ERROR;
			this.json = new JSONObject();
		}

		public Response(int code) {
			this.success = false;
			this.msg = "HTTP Error " + code + "(network down?)";
			this.code = code;
			this.json = new JSONObject();
		}

		public Response(JSONObject json) {
			boolean success;
			String msg;
			try {
				success = json.getBoolean("success");
				msg = json.getString("msg");
			} catch (JSONException e) {
				success = false;
				msg = "Received invalid JSON";
			}
			this.success = success;
			this.msg = msg;
			this.code = HttpURLConnection.HTTP_OK;
			this.json = json;
		}
		
		public String getMessage() {
			return msg;
		}

		public int getHttpCode() {
			return code;
		}

		public boolean isSuccess() {
			return success;
		}
		
		public JSONObject getJSON() {
			return json;
		}
	}
	
	// The POST data
	private String url;
	private LinkedList<Pair<String ,String>> data = new LinkedList<Pair<String, String>>();
	
	// Helper variables
	public final static String CRLF = "\r\n"; // Line separator required by multipart/form-data.
	public final static String charsetUTF8 = "UTF-8";
	public final String boundary = Long.toHexString(System.currentTimeMillis());

	public HttpPostJson(String url) {
		this.url = url;
	}
	
	/**
	 * Add a key/value pair
	 * @param key
	 * @param value
	 */
	public HttpPostJson send(String key, String value) {
		Pair<String, String> pair = new Pair(key, value);
		data.add(pair);
		return this;
	}
	
	
	/**
	 * Send the Http POST over the network and receive the response.
	 * If there is no error sending, this method blocks until the 
	 * response is received.
	 */
	public Response receive() {
		// see http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
		
		HttpURLConnection connection = null;
		try {
			URL urlJava = new URL(url);
			connection = (HttpURLConnection) urlJava.openConnection();
		} catch (MalformedURLException e) {
			return new Response(e.getMessage());
		} catch (IOException e) {
			return new Response(e.getMessage());
		}

		connection.setDoOutput(true);
		connection.setRequestProperty("Accept-Charset", charsetUTF8);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

		PrintWriter writer = null;
		try {
		    OutputStream output = connection.getOutputStream();
		    writer = new PrintWriter(new OutputStreamWriter(output, charsetUTF8), true);

		    for (Pair<String, String> pair : data)
		    	writePostRequestPart(writer, pair.first, pair.second);
		    
		    writer.append("--" + boundary + "--").append(CRLF);
		} catch (IOException e) {
			return new Response(e.getMessage());		
		} finally {
		    if (writer != null) writer.close();
		}
		
		int code = 0;
		try {
			code = connection.getResponseCode();
		} catch (IOException e) {
			return new Response(e.getMessage());
		}
		if (code != HttpURLConnection.HTTP_OK)
			return new Response(code);

		String contentType = connection.getHeaderField("Content-Type");
		if (!contentType.equals("application/json"))
			return new Response("Wrong content type");
		// Log.e(TAG, "content type = "+contentType);
		
		String msg;
		try {
			InputStream in = connection.getInputStream();
			byte[] buf = new byte[1024];
			final StringBuilder out = new StringBuilder();
			while (true) {
		        int n = in.read(buf);
		        if (n < 0)
		          break;
		        String str = new String(buf, 0, n, charsetUTF8);
		        out.append(str);
			}
			msg = out.toString();
		} catch (IOException e) {
			return new Response(e.getMessage());		
		} finally {
			connection.disconnect();
		}

		JSONObject json = null;
		try {
			json = new JSONObject(msg);
		} catch (JSONException e) {
			return new Response(e.getMessage());
		}
		return new Response(json);

		
	}

	
	private void writePostRequestPart(PrintWriter writer, String name, String data) {
	    writer.append("--" + boundary).append(CRLF);
	    writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(CRLF);
	    writer.append("Content-Type: text/plain; charset=" + charsetUTF8).append(CRLF);
	    writer.append(CRLF);
	    writer.append(data).append(CRLF).flush();
	}

}








