package com.write.Quill.sync;

import android.app.Activity;

public abstract class UserProfile {

	private static UserProfile instance = null;
	
	public static UserProfile getInstance(Activity activity) {
		if (instance != null)
			return instance;
		if (android.os.Build.VERSION.SDK_INT >= 14 )
			instance = new UserProfileICS(activity);
		else
			instance = new UserProfileHoneycomb();
		return instance;
	}
	
	
	abstract String name();
	abstract String email();
	
}
