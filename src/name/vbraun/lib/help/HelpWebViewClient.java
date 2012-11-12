package name.vbraun.lib.help;

import android.os.Build;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class HelpWebViewClient extends WebViewClient {
	@SuppressWarnings("unused")
	private final static String TAG = "HelpWebViewClient";

	// workaround for WebView bug
	// http://stackoverflow.com/questions/6542702/basic-internal-links-dont-work-in-honeycomb-app/7297536#7297536
	@Override
	public void onReceivedError(WebView view, int errorCode, String description, String failingUrl)
	{
		if (failingUrl.contains("#")) {
			Log.v("LOG", "failing url:"+ failingUrl);
			final int sdkVersion = Integer.parseInt(Build.VERSION.SDK);
			if (sdkVersion > Build.VERSION_CODES.GINGERBREAD) {
				String[] temp;
				temp = failingUrl.split("#");
				view.loadUrl(temp[0]); // load page without internal link
				try {
					Thread.sleep(400);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			view.loadUrl(failingUrl);  // try again
		} else {
			view.loadUrl("file:///android_asset/help/Manual.html");
		}
	}

}
