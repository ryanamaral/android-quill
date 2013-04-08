package name.vbraun.view.write;

import junit.framework.Assert;

import com.write.Quill.R;

import android.content.Context;
import android.content.res.Resources;

public class Paper {
	private static final String TAG = "PaperType";
	
	public enum Type {
		EMPTY, RULED, QUAD, HEX, COLLEGERULED, NARROWRULED, 
		CORNELLNOTES, DAYPLANNER, MUSIC
	}
	
	private final static String EMPTY = "EMPTY";
	private final static String RULED = "RULED";
	private final static String COLLEGERULED = "COLLEGERULED";
	private final static String NARROWRULED = "NARROWRULED";
	private final static String QUADPAPER = "QUADPAPER";
	private final static String CORNELLNOTES = "CORNELLNOTES";
	private final static String DAYPLANNER = "DAYPLANNER";
	private final static String MUSIC = "MUSIC";

	private CharSequence resourceName;
	private Type type;
	
	public static final Paper[] Table = {
		new Paper(EMPTY, Type.EMPTY),
		new Paper(RULED, Type.RULED),
		new Paper(COLLEGERULED, Type.COLLEGERULED),
		new Paper(NARROWRULED, Type.NARROWRULED),
		new Paper(QUADPAPER, Type.QUAD),
		new Paper(CORNELLNOTES, Type.CORNELLNOTES),
		new Paper(DAYPLANNER, Type.DAYPLANNER),
		new Paper(MUSIC, Type.MUSIC),
	};

		
	public Paper(String resourceName, Paper.Type t) {
		this.resourceName = resourceName;
		type = t;
	}
		
	public CharSequence getName(Context context) {
		Resources res = context.getResources();
		String[] values  = res.getStringArray(R.array.dlg_background_values);
		String[] entries = res.getStringArray(R.array.dlg_background_entries);
		Assert.assertTrue(values.length == entries.length);
		for (int i=0; i<entries.length; i++) 
			if (resourceName.equals(values[i]))
				return entries[i];
		return null;
	}

	public Type getType() {
		return type;
	}
		

}
