package com.write.Quill.sync;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.net.ssl.HttpsURLConnection;

import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

/**
 * A Loader that send HTTP POST and receives back a JSON object.
 * @author vbraun
 *
 */
public abstract class HttpLoader extends
		AsyncTaskLoader<HttpLoader.Response> {

	public HttpLoader(Context context) {
		super(context);
	}

	private final static String TAG = "HttpLoader";

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

	@Override
	protected void onStartLoading() {
		forceLoad();
	}
	
	public final static String CRLF = "\r\n"; // Line separator required by multipart/form-data.
	public final static String charsetUTF8 = "UTF-8";
	public final String boundary = Long.toHexString(System.currentTimeMillis());
	
	
	/**
	 * Return the path-component on the server to send the request to
	 * @return
	 */
	abstract protected String getServerPath();
	
	/**
	 * This method must be implemented to write out the multipart/form-data
	 * @param writer
	 */
	abstract protected void writePostRequest(PrintWriter writer);
	
	/**
	 * Helper to implement writePostRequest
	 * @param writer
	 * @param name
	 * @param charset
	 * @param data
	 */
	protected void writePostRequestPart(PrintWriter writer, String name, String charset, String data) {
	    writer.append("--" + boundary).append(CRLF);
	    writer.append("Content-Disposition: form-data; name=\"" + name + "\"").append(CRLF);
	    writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
	    writer.append(CRLF);
	    writer.append(data).append(CRLF).flush();
	}
	
	@Override
	public Response loadInBackground() {
		// see http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
		
		HttpsURLConnection connection = null;
		try {
			URL url = new URL("https://quill.sagepad.org/" + getServerPath());
			connection = (HttpsURLConnection) url.openConnection();
			connection.setHostnameVerifier(new org.apache.http.conn.ssl.StrictHostnameVerifier());
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
		    writePostRequest(writer);
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

	@Override
	protected void onStopLoading() {
		cancelLoad();
	}

}
