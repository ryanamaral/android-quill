package com.write.Quill.sync;

/**
 * Fallback for the user profile data for pre-ICS devices
 * @author vbraun
 *
 */
public class UserProfileHoneycomb extends UserProfile {

	@Override
	String name() {
		String name = android.os.Build.DEVICE;
		if (name == null)
			name = "Firstname Lastname";
		return name;
	}

	@Override
	String email() {
		return "my.address@gmail.com";
	}

}
