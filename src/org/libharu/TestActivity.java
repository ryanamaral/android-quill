package org.libharu;

import java.io.File;
import java.io.IOException;

import org.libharu.Font.BuiltinFont;
import org.libharu.Page.LineCap;
import org.libharu.Page.PageDirection;
import org.libharu.Page.PageSize;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

public class TestActivity extends Activity {
	private static final String TAG = "HPDF_TestActivity";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		String filename = "/test.pdf";
		File file = new File(filename);
		
		Document pdf = new Document();
		Page page = pdf.addPage();
		page.setSize(PageSize.LETTER, PageDirection.LANDSCAPE);
		page.setLineCap(LineCap.ROUND_END);
		page.setLineWidth(10);
		page.setRGBStroke(0.2f, 0.5f, 0.7f);
		page.moveTo(1, 1);
		page.lineTo(100, 100);
		page.stroke();
		
		String text = new String("The quick brown fox jumps over the lazy dog.");
		Font helvetica = pdf.getFont(BuiltinFont.COURIER_BOLD_OBLIQUE);
		page.setFontAndSize(helvetica, 24);
		float tw = page.getTextWidth(text);
		page.beginText();
		page.textOut((page.getWidth()-tw)/2, page.getHeight()-50, text);
		page.endText();
		
		try {
			pdf.saveToFile(filename);
		} catch (IOException e) {
			finish();
		}
		
		Intent intent = new Intent();
        intent.setAction(Intent.ACTION_SEND);
        intent.setType("application/pdf");
        intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        try {
            startActivity(Intent.createChooser(intent, "Open PDF file:"));
            finish();
        } catch (android.content.ActivityNotFoundException ex) {
            Toast.makeText(this, "Error", Toast.LENGTH_LONG).show();
        }

	}
	
}
