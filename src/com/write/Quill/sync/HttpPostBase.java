package com.write.Quill.sync;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.LinkedList;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;
import android.util.Pair;

/**
 * Base class for Http POST.
 * Derived classes must implement processServerReply() to do something with the output. 
 * 
 * @author vbraun
 * 
 */
public abstract class HttpPostBase {
	@SuppressWarnings("unused")
	private final static String TAG = "HttpPostBase";
	
	protected volatile boolean cancelled = false;
	
	/**
	 * cancel the sending
	 */
	public synchronized void cancel() {
		cancelled = true;
	}
	
	
	/**
	 * The server response 
	 */
	public static class Response {
		private final boolean success;
		private final String msg;
		private final int code;
		private final JSONObject json;
		private final File file;
		public final static int ERROR = -1;
		public final static int CANCELLED = -2;

		private Response(boolean success, String msg, int code, JSONObject json) {
			this.success = success;
			this.msg = msg;
			this.code = code;
			this.json = json;
			this.file = null;
		}
		
		public Response(String errorMsg) {
			this.success = false;
			this.msg = errorMsg;
			this.code = ERROR;
			this.json = new JSONObject();
			this.file = null;
		}

		public Response(int code) {
			this.success = false;
			this.msg = "HTTP Error " + code + "(network down?)";
			this.code = code;
			this.json = new JSONObject();
			this.file = null;
		}

		public Response(File file) {
			this.success = true;
			this.msg = "Received "+file.getName();
			this.code = HttpURLConnection.HTTP_OK;
			this.json = new JSONObject();
			this.file = file;
		}
		
		public static Response cancelled() {
			return new Response(false, "Cancelled upon request", CANCELLED, new JSONObject());
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
			this.file = null;
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
		
		public boolean isCancelled() {
			return code == CANCELLED;
		}
		
		public JSONObject getJSON() {
			return json;
		}
	}
	
	// The POST data
	private String url;
	private LinkedList<Pair<String ,String>> data = new LinkedList<Pair<String, String>>();
	private LinkedList<Pair<String ,File>> dataFile = new LinkedList<Pair<String, File>>();

	// Helper variables
	public final static String CRLF = "\r\n"; // Line separator required by multipart/form-data.
	public final static String charsetUTF8 = "UTF-8";
	public final String boundary = Long.toHexString(System.currentTimeMillis());

	public HttpPostBase(String url) {
		this.url = url;
	}
	
	/**
	 * Add a key/value pair to the http POST
	 * @param key
	 * @param value
	 */
	protected HttpPostBase send(String key, String value) {
		Pair<String, String> pair = new Pair<String,String>(key, value);
		data.add(pair);
		return this;
	}
	
	/**
	 * Add a key/file pair to the http POST
	 * @param key
	 * @param file the file whose content will be uploaded to the server
	 */
	protected HttpPostBase send(String key, File file) {
		Pair<String, File> pair = new Pair<String,File>(key, file);
		dataFile.add(pair);
		return this;
	}
	
	
	// whether chunked transfer works (old squid proxies break it)
	private static boolean chunked = true; 
		
	/**
	 * Send the Http POST over the network and receive the response.
	 * If there is no error sending, this method blocks until the 
	 * response is received.
	 */
	public Response receive() {
		// see http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
		
		HttpsURLConnection connection = null;
		try {
			URL urlJava = new URL(url);
			connection = (HttpsURLConnection) urlJava.openConnection();
			connection.setHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier());
		} catch (MalformedURLException e) {
			return new Response(e.getMessage());
		} catch (IOException e) {
			return new Response(e.getMessage());
		}
		
		connection.setDoOutput(true);
		if (chunked)
			connection.setChunkedStreamingMode(0);
		connection.setRequestProperty("Accept-Charset", charsetUTF8);
		connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
		connection.setRequestProperty("Cache-Control", "no-cache");

		PrintWriter writer = null;
		try {
		    OutputStream output = connection.getOutputStream();
		    writer = new PrintWriter(new OutputStreamWriter(output, charsetUTF8), true);

		    for (Pair<String, String> pair : data) {
		    	if (cancelled) return Response.cancelled();
		    	writePostRequestPart(writer, pair.first, pair.second);
		    }

		    for (Pair<String, File> pair : dataFile) {
		    	if (cancelled) return Response.cancelled();
		    	writePostRequestPart(writer, output, pair.first, pair.second);
		    }
		    writer.print("--" + boundary + "--"); writer.append(CRLF);
		} catch (IOException e) {
			return new Response(e.getMessage());		
		} finally {
		    if (writer != null)
		    	writer.close();
		}

		int code = 0;
		try {
			code = connection.getResponseCode();
		} catch (IOException e) {
			return new Response(e.getMessage());
		}
		if (code == HttpURLConnection.HTTP_LENGTH_REQUIRED && chunked) {
			Log.e(TAG, "Broken proxy detected, disable chunking..");
			chunked = false;
			return receive();  // try again
		}
		if (code != HttpURLConnection.HTTP_OK)
			return new Response(code);
    	if (cancelled) return Response.cancelled();

    	return processServerReply(connection);
	}

	/**
	 * Parse the server response. Derived classes must implement this.
	 * Error checking has already been done, we got HTTP_OK.
	 */
	protected abstract Response processServerReply(HttpURLConnection connection);

	
	private void writePostRequestPart(PrintWriter writer, String name, String data) {
	    writer.print("--" + boundary); writer.append(CRLF);
	    writer.print("Content-Disposition: form-data; name=\"" + name + "\""); writer.append(CRLF);
	    writer.print("Content-Type: text/plain; charset=" + charsetUTF8); writer.append(CRLF);
	    writer.append(CRLF);
	    writer.print(data); writer.append(CRLF).flush();
	}

	
	private void writePostRequestPart(PrintWriter writer, OutputStream output, String name, File file)
		throws IOException 
	{
        writer.print("--" + boundary);  writer.append(CRLF);
        writer.print("Content-Disposition: form-data; name=\""+ name +"\"; filename=\"" + file.getName() + "\""); writer.append(CRLF); 
        writer.print("Content-Type: application/octet-stream"); writer.append(CRLF); 
        writer.print("Content-Transfer-Encoding: binary"); writer.append(CRLF); 
        writer.append(CRLF).flush(); 

        InputStream input = null;
        try {
            input = new FileInputStream(file);
            byte[] buffer = new byte[1024];
            for (int length = 0; (length = input.read(buffer)) > 0;)
                output.write(buffer, 0, length);
            output.flush();
        } finally {
            if (input != null) 
            	input.close();
        }
        
        writer.append(CRLF).flush();
	}

	protected Response readJsonReply(HttpURLConnection connection) {
		String contentType = connection.getHeaderField("Content-Type");
		if (!contentType.equals("application/json"))
			return new Response("Wrong content type");
		// Log.e(TAG, "content type = "+contentType);
		
		String msg;
		try {
			InputStream in = connection.getInputStream();
			byte[] buf = new byte[1024];
			final StringBuilder out = new StringBuilder();
			while (true && !cancelled) {
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
    	if (cancelled) return Response.cancelled();

		JSONObject json = null;
		try {
			json = new JSONObject(msg);
		} catch (JSONException e) {
			return new Response(e.getMessage());
		}
		return new Response(json);		
	}

}




