package com.write.Quill;

import com.write.Quill.R;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.util.Log;
 
 public class Preferences extends PreferenceActivity {
	 private static final String TAG = "Preferences";
	 
    @Override
     public void onCreate(Bundle savedInstanceState) {
         super.onCreate(savedInstanceState);  
         try {
        	 addPreferencesFromResource(R.xml.preferences);
         } catch (ClassCastException e) {
        	 Log.e(TAG, e.toString());
         }
     }
}