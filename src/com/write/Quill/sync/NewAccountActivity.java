package com.write.Quill.sync;

import java.util.Random;

import android.os.Bundle;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.write.Quill.R;

public class NewAccountActivity 
	extends NetworkActivity 
	implements OnCheckedChangeListener, OnClickListener {
	
	private final static String TAG = "NewAccountActivity";
	
	public final static String NEW_ACCOUNT = "new_account";
	public final static String EDIT_ACCOUNT = "edit_account";
	
	private EditText name, email, password;
	private Button okButton, cancelButton;
	private CheckBox showPassword;
	private ProgressBar progress;
	
    private final String alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private final int alphabetLength = alphabet.length();
	
    private final int passwordLength = 6;
    
    private String randomPassword() {
    	String pass = new String();
        Random r = new Random();
        for (int i = 0; i < passwordLength; i++)
            pass += alphabet.charAt(r.nextInt(alphabetLength));
        return pass;
    }
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	setTitle(R.string.account_new_title);
    	setContentView(R.layout.account_new);
    	name = (EditText) findViewById(R.id.account_new_name);
    	email = (EditText) findViewById(R.id.account_new_email);
    	password = (EditText) findViewById(R.id.account_new_password);
    	showPassword = (CheckBox) findViewById(R.id.account_new_show_password);
    	showPassword.setOnCheckedChangeListener(this);
    	onCheckedChanged(showPassword, showPassword.isChecked());
    	okButton = (Button) findViewById(R.id.account_new_create);
    	okButton.setOnClickListener(this);
    	cancelButton = (Button) findViewById(R.id.account_new_cancel);
    	cancelButton.setOnClickListener(this);
    	progress = (ProgressBar) findViewById(R.id.account_new_progress);
    	
    	UserProfile profile = UserProfile.getInstance(this);
    	name.setText(profile.name());
    	email.setText(profile.email());
    	password.setText(randomPassword());	
	}
	
	@Override
	protected void onResume() {
		super.onResume();
	}
	
	@Override
	protected void onPause() {
		super.onPause();
	}

	@Override
	public void onCheckedChanged(CompoundButton view, boolean isChecked) {
		if (view == showPassword) {
			Log.e(TAG, "checked "+isChecked);
			if (isChecked) 
				password.setTransformationMethod(null);
			else
				password.setTransformationMethod(new PasswordTransformationMethod());
		}
	}

	private void enable(boolean enabled) {
		progress.setVisibility(enabled ? View.INVISIBLE : View.VISIBLE);
		name.setEnabled(enabled);
		email.setEnabled(enabled);
		password.setEnabled(enabled);
		showPassword.setEnabled(enabled);
		okButton.setEnabled(enabled);
	}
	
	@Override
	public void onClick(View v) {
		if (v == okButton) {
			enable(false);
		} 
		if (v == cancelButton) {
			enable(true);
		}
	}
	
}
