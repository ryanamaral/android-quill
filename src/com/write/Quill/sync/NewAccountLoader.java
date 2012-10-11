package com.write.Quill.sync;

import java.io.PrintWriter;
import android.content.Context;
import android.util.Log;

public class NewAccountLoader extends HttpLoader {
	private final static String TAG = "NewAccountLoader";

	protected final String name;
	protected final String email;
	protected final String password;

	
	public NewAccountLoader(Context context, String name, String email,
			String password) {
		super(context);
		this.name = name;
		this.email = email;
		this.password = password;
	}

	@Override
	protected String getServerPath() {
		return "_create";
	}
	
	@Override
	protected void writePostRequest(PrintWriter writer) {
		writePostRequestPart(writer, "name", charsetUTF8, name);
		writePostRequestPart(writer, "email", charsetUTF8, email);
		writePostRequestPart(writer, "password", charsetUTF8, password);

	}

}

// public class NewAccountLoader extends
// AsyncTaskLoader<NewAccountLoader.Response> {
// private final static String TAG = "NewAccountLoader";
//
// public static class Response {
// private String msg;
// private int code;
// public final static int ERROR = -1;
//
// public Response() {
// }
//
// public Response(String msg, int code) {
// this.msg = msg;
// this.code = code;
// }
//
// public String getMessage() {
// return msg;
// }
//
// public int getCode() {
// return code;
// }
//
// public boolean success() {
// return code == HttpURLConnection.HTTP_OK;
// }
// }
//
// private final String name, email, password;
//
// public NewAccountLoader(Context context, String name, String email,
// String password) {
// super(context);
// this.name = name;
// this.email = email;
// this.password = password;
// }
//
// @Override
// protected void onStartLoading() {
// forceLoad();
// }
//
// @Override
// public Response loadInBackground() {
// // see
// http://stackoverflow.com/questions/2793150/how-to-use-java-net-urlconnection-to-fire-and-handle-http-requests
//
// final String CRLF = "\r\n"; // Line separator required by
// multipart/form-data.
// final String charset = "UTF-8";
// final String boundary = Long.toHexString(System.currentTimeMillis());
//
// HttpURLConnection connection = null;
// try {
// URL url = new URL("http://quill.sagepad.org/_create");
// connection = (HttpURLConnection) url.openConnection();
// } catch (MalformedURLException e) {
// return new Response(e.getMessage(), Response.ERROR);
// } catch (IOException e) {
// return new Response(e.getMessage(), Response.ERROR);
// }
//
// connection.setDoOutput(true);
// connection.setRequestProperty("Accept-Charset", charset);
// connection.setRequestProperty("Content-Type",
// "multipart/form-data; boundary=" + boundary);
//
// PrintWriter writer = null;
// try {
// OutputStream output = connection.getOutputStream();
// writer = new PrintWriter(new OutputStreamWriter(output, charset), true);
//
// writer.append("--" + boundary).append(CRLF);
// writer.append("Content-Disposition: form-data; name=\"name\"").append(CRLF);
// writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
// writer.append(CRLF);
// writer.append(name).append(CRLF).flush();
//
// writer.append("--" + boundary).append(CRLF);
// writer.append("Content-Disposition: form-data; name=\"email\"").append(CRLF);
// writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
// writer.append(CRLF);
// writer.append(email).append(CRLF).flush();
//
// writer.append("--" + boundary).append(CRLF);
// writer.append("Content-Disposition: form-data; name=\"password\"").append(CRLF);
// writer.append("Content-Type: text/plain; charset=" + charset).append(CRLF);
// writer.append(CRLF);
// writer.append(password).append(CRLF).flush();
//
// writer.append("--" + boundary + "--").append(CRLF);
// } catch (IOException e) {
// return new Response(e.getMessage(), Response.ERROR);
// } finally {
// if (writer != null) writer.close();
// }
//
// int code = 0;
// try {
// code = connection.getResponseCode();
// } catch (IOException e) {
// return new Response(e.getMessage(), Response.ERROR);
// }
// if (code != HttpURLConnection.HTTP_OK)
// return new Response("HTTP Error " + code + "(network down?)", code);
//
// String contentType = connection.getHeaderField("Content-Type");
// if (!contentType.equals("application/json"))
// return new Response("Wrong content type", Response.ERROR);
// // Log.e(TAG, "content type = "+contentType);
//
// String msg;
// try {
// InputStream in = connection.getInputStream();
// byte[] buf = new byte[1024];
// final StringBuilder out = new StringBuilder();
// while (true) {
// int n = in.read(buf);
// if (n < 0)
// break;
// String str = new String(buf, 0, n, charset);
// out.append(str);
// }
// msg = out.toString();
// } catch (IOException e) {
// return new Response(e.getMessage(), Response.ERROR);
// } finally {
// connection.disconnect();
// }
//
// JSONObject json = null;
// try {
// json = new JSONObject(msg);
// String json_msg = json.getString("msg");
// boolean json_success = json.getBoolean("success");
// if (json_success)
// return new Response(json_msg, code);
// else
// return new Response(json_msg, Response.ERROR);
// } catch (JSONException e) {
// return new Response(e.getMessage(), Response.ERROR);
// }
// }
//
// @Override
// protected void onStopLoading() {
// cancelLoad();
// }
//
// }
