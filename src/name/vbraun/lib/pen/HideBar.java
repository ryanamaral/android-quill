package name.vbraun.lib.pen;

import android.util.Log;

/** Hide the System bar (bottom bar) 
 *  Requires root
 */
public class HideBar {
	private final static String TAG = "HideBar";
	
	
    public static void hideSystembar(boolean wait) {
        Log.v(TAG, "hideSystembar");
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    "su","-c","service call activity 79 s16 com.android.systemui"});
            if (wait) proc.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Failed to hide system bar "+e.getLocalizedMessage());
        }
    }
    
    public static void hideSystembar() {
    	hideSystembar(false);
    }

    
    public static void showSystembar(boolean wait) {
        Log.v(TAG, "showSystembar");
        try {
            Process proc = Runtime.getRuntime().exec(new String[]{
                    "am","startservice","-n","com.android.systemui/.SystemUIService"});
            if (wait) proc.waitFor();
        } catch (Exception e) {
            Log.e(TAG, "Failed to show system bar "+e.getLocalizedMessage());
        }
    }

    public static void showSystembar() {
    	showSystembar(false);
    }

	
}
