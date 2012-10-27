package com.write.Quill.sync;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import android.util.Log;

/**
 *  Http POST a set of key/value pairs and save the result to a File
 *  
 * @author vbraun
 *
 */
public class HttpPostFile extends HttpPostBase {
	@SuppressWarnings("unused")
	private final static String TAG = "HttpPostFile";
	
	private final File file;
	
	public HttpPostFile(String url, File file) {
		super(url);
		this.file = file;
	}

	
	
	@Override
	protected Response processServerReply(HttpURLConnection connection) {
		String contentType = connection.getHeaderField("Content-Type");
		if (contentType.equals("application/json"))
			return readJsonReply(connection);   // JSON error message
		if (!contentType.equals("application/octet-stream"))
			return new Response("Wrong content type");
		
		Log.e(TAG, "got Content-Length "+connection.getHeaderField("Content-Length"));
		
		InputStream in = null;
		FileOutputStream fos = null;
		BufferedOutputStream buffer = null;
		DataOutputStream out = null;
		try {
			in = connection.getInputStream();
			fos = new FileOutputStream(file);
			buffer = new BufferedOutputStream(fos);
			out = new DataOutputStream(buffer);
			
			byte[] buf = new byte[1024];
			while (true && !cancelled) {
		        int n = in.read(buf);
		        if (n < 0)
		        	break;
		        out.write(buf, 0, n);
			}

			
		} catch (IOException e) {
			return new Response(e.getMessage());		
		}  finally {
			try {
				if (out != null) out.close();
				else if (buffer != null) buffer.close();
				else if (fos != null) fos.close();
				if (in != null) in.close();
			} catch (IOException e) {}
			connection.disconnect();
		}			

    	if (cancelled) return Response.cancelled();
		return new Response(file);		
	}

}
