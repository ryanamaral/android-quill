package name.vbraun.lib.help;

import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.WebView;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;

public class HelpBrowser extends ActivityBase {
	@SuppressWarnings("unused")
	private final static String TAG = "HelpBrowser";
	
	private WebView browser;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.help_browser);
		browser = (WebView)findViewById(R.id.help_browser_webview);
		showManual();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.help_browser, menu);
		return super.onCreateOptionsMenu(menu);
	}
	
	private void showManual() {
		browser.loadUrl("file:///android_asset/help/Manual.html");
	}
	
	private void showFAQ() {
		browser.loadUrl("file:///android_asset/help/FAQ.html");
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.help_browser_manual:
			showManual();
			return true;
		case R.id.help_browser_faq:
			showFAQ();
			return true;
		case R.id.help_browser_quit:
			finish();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
}
