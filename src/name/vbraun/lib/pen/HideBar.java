package name.vbraun.lib.pen;

import java.io.IOException;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import junit.framework.Assert;

import com.write.Quill.R;

import android.content.Context;
import android.os.Build;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

/** Hide the System bar (bottom bar) 
 *  Requires root
 */
public class HideBar {
	private final static String TAG = "HideBar";
	
	private static HideBar instance;
		
    private final static String[] commandHideHoneycomb = new String[]{
		"su","-c","service call activity 79 s16 com.android.systemui"};

    private final static String[] commandHideICS = new String[]{
		"su","-c","service call activity 42 s16 com.android.systemui"};

    private final static String[] commandShow = new String[]{
        "am","startservice","-n","com.android.systemui/.SystemUIService"};

	private static HideBar getInstance() {
		if (HideBar.instance != null)
			return HideBar.instance;
		HideBar.instance = new HideBar();
		return HideBar.instance;
	}
	
	Process proc;
	final Callable<Integer> call;
	
	private HideBar() {
		call = new Callable<Integer>() {
		    public Integer call() throws Exception {
		        proc.waitFor();
		        return proc.exitValue();
		      }
		    };
	}
	
	public static class HideBarException extends Exception {
		private static final long serialVersionUID = -7440214585841482430L;
	}
	
    public static void hideSystembar() throws HideBarException {
    	try {
    		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) 
    			HideBar.getInstance().execute(commandHideHoneycomb);
    		else
    			HideBar.getInstance().execute(commandHideICS);
    	} catch (TimeoutException timeout) {
    		throw new HideBarException();
    	}
    }
    
    public static void showSystembar() throws HideBarException {
    	try {
    		HideBar.getInstance().execute(commandShow);
    	} catch (TimeoutException timeout) {
    		// "am" works but does not return if USB debugging is disabled
    		// so we ignore the timeout
    	}
   }

    public static void hideSystembar(Context context) {
    	try {
    		hideSystembar();
    	} catch (HideBarException e) {
    		Toast.makeText(context, R.string.hide_bar_failed_hide, Toast.LENGTH_LONG).show();
    	}
    }
        
    public static void showSystembar(Context context) {
    	try {
    		showSystembar();
    	} catch (HideBarException e) {
    		Toast.makeText(context, R.string.hide_bar_failed_show, 
    				Toast.LENGTH_LONG).show();
    	}
    }

    public void execute(String[] command) throws HideBarException, TimeoutException {
    	try {
    		proc = Runtime.getRuntime().exec(command);
    	} catch (IOException e) {
    		throw new HideBarException();
    	}
		ExecutorService service = Executors.newSingleThreadExecutor();
		try {
		    Future<Integer> ft = service.submit(call);
		    try {
		        int rc = ft.get(500, TimeUnit.MILLISECONDS);
		        if (rc != 0)
		        	throw new HideBarException();
		        return;
		    } catch (TimeoutException timeout) {
		        proc.destroy();
		        throw timeout;
		    } catch (InterruptedException e) {
		        proc.destroy();
		        throw new HideBarException();
			} catch (ExecutionException e) {
		        proc.destroy();
		        throw new HideBarException();
			}
		}
		finally {
		    service.shutdown();
		}
    }

    
    /** Check whether hiding the system bar can work
     * @return True if we think that it is possible to hide the system bar
     */
    public static boolean isPossible() {
    	return true;
    }
	
	
}
