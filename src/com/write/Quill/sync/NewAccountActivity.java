package com.write.Quill.sync;

import java.util.Random;

import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
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
import android.widget.Toast;

import com.write.Quill.R;

public class NewAccountActivity 
	extends Activity
	implements 
		OnCheckedChangeListener, 
		OnClickListener,
		LoaderCallbacks<NewAccountLoader.Response> {
	
	private final static String TAG = "NewAccountActivity";
	
	public final static String ACTION_NEW_ACCOUNT = "new_account";
	public final static String EXTRA_EMAIL_ADDRESS = LoginActivity.EXTRA_EMAIL_ADDRESS;
	public final static String EXTRA_PASSWORD = LoginActivity.EXTRA_PASSWORD;
	public final static int REQUEST_RETURN_ACCOUNT = 100;
	
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
    	Bundle extras = getIntent().getExtras();
    	String extraEmail = null;
    	if (extras != null)
    		extraEmail = extras.getString(EXTRA_EMAIL_ADDRESS);
    	email.setText(extraEmail!=null ? extraEmail : profile.email());
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
	
	public void successfullyCreated() {
	} 

	@Override
	public void onClick(View v) {
		if (v == okButton) {
			enable(false);
			getLoaderManager().initLoader(0, null, this);
		} 
		if (v == cancelButton) {
			enable(true);
			Intent data = new Intent();
			setResult(RESULT_CANCELED, data);
			finish();
		}
	}

	@Override
	public Loader<NewAccountLoader.Response> onCreateLoader(int id, Bundle args) {
		String name_str = name.getText().toString();
		String email_str = email.getText().toString();
		String password_str = password.getText().toString();
		return new NewAccountLoader(this, name_str, email_str, password_str);
	}

	@Override
	public void onLoadFinished(Loader<NewAccountLoader.Response> loader, NewAccountLoader.Response response) {
		enable(true);
		Log.e(TAG, "onLoadFinished "+response.getHttpCode() + " " + response.getMessage()); 
		if (!response.isSuccess()) {
			String msg = getResources().getString(R.string.account_error_create, response.getMessage());
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		Intent data = new Intent();
		data.putExtra(EXTRA_EMAIL_ADDRESS, email.getText().toString());
		data.putExtra(EXTRA_PASSWORD, password.getText().toString());
		setResult(RESULT_OK, data);
		finish();	
	}

	@Override
	public void onLoaderReset(Loader<NewAccountLoader.Response> loader) {
	}
	
}
