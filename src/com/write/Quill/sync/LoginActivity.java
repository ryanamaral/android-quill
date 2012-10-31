package com.write.Quill.sync;

import org.json.JSONException;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Intent;
import android.content.Loader;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
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

public class LoginActivity 
	extends 
		Activity 
	implements 
		OnCheckedChangeListener, 
		OnClickListener, 
		TextWatcher, 
		LoaderCallbacks<LoginLoader.Response> {
	
	private final static String TAG = "LoginActivity";
	
	private EditText email, password;
	private Button newButton, okButton, cancelButton;
	private CheckBox showPassword;
	private ProgressBar progress;
	    
	private UserProfile profile;
	
	// Intent actions
	public final static String ACTION_LOGIN = "account_login";
	public final static String EXTRA_NAME = QuillAccount.EXTRA_NAME;
	public final static String EXTRA_EMAIL_ADDRESS = QuillAccount.EXTRA_EMAIL_ADDRESS;
	public final static String EXTRA_PASSWORD = QuillAccount.EXTRA_PASSWORD;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
    	profile = UserProfile.getInstance(this); 
    	setTitle(R.string.account_login_title);
    	setContentView(R.layout.account_login);
    	email = (EditText) findViewById(R.id.account_login_email);
    	email.setText(profile.email());
    	password = (EditText) findViewById(R.id.account_login_password);
    	showPassword = (CheckBox) findViewById(R.id.account_login_show_password);
    	showPassword.setOnCheckedChangeListener(this);
    	onCheckedChanged(showPassword, showPassword.isChecked());
    	newButton = (Button) findViewById(R.id.account_login_new);
    	newButton.setOnClickListener(this);
    	okButton = (Button) findViewById(R.id.account_login_ok);
    	okButton.setOnClickListener(this);
    	cancelButton = (Button) findViewById(R.id.account_login_cancel);
    	cancelButton.setOnClickListener(this);
    	progress = (ProgressBar) findViewById(R.id.account_login_progress);
    	email.addTextChangedListener(this);
    	password.addTextChangedListener(this);
    	afterTextChanged(null);
	}
	
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    	switch (requestCode) {
    	case NewAccountActivity.REQUEST_RETURN_ACCOUNT:
    		if (resultCode != RESULT_OK || data == null) return;
    		Bundle extras = data.getExtras();
    		String email_str = extras.getString(NewAccountActivity.EXTRA_EMAIL_ADDRESS);
    		String pass_str  = extras.getString(NewAccountActivity.EXTRA_PASSWORD);
    		if (email_str != null)
    			email.setText(email_str);
    		if (pass_str != null)
        		password.setText(pass_str);
    		break;
    	}
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
		email.setEnabled(enabled);
		password.setEnabled(enabled);
		showPassword.setEnabled(enabled);
		newButton.setEnabled(enabled);
		okButton.setEnabled(enabled);
	}
	
	@Override
	public void onClick(View v) {
		if (v == newButton) {
			Intent intent = new Intent(this, NewAccountActivity.class);
			intent.setAction(NewAccountActivity.ACTION_NEW_ACCOUNT);
			intent.putExtra(NewAccountActivity.EXTRA_EMAIL_ADDRESS, email.getText().toString());
			startActivityForResult(intent, NewAccountActivity.REQUEST_RETURN_ACCOUNT);
		} 	
		if (v == okButton) {
			enable(false);
			getLoaderManager().initLoader(0, null, this);
		} 
		if (v == cancelButton) {
			enable(true);
			setResult(RESULT_CANCELED);
			finish();
		}
	}

	@Override
	public void afterTextChanged(Editable s) {
		boolean empty = (password.getText().length() == 0);
		okButton.setEnabled(!empty);
		newButton.setVisibility(empty ? View.VISIBLE : View.INVISIBLE);		
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,
			int after) {}

	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {}
	
	@Override
	public Loader<LoginLoader.Response> onCreateLoader(int id, Bundle args) {
		String email_str = email.getText().toString();
		String password_str = password.getText().toString();
		return new LoginLoader(this, email_str, password_str);
	}

	@Override
	public void onLoadFinished(Loader<LoginLoader.Response> loader, LoginLoader.Response response) {
		enable(true);
		Log.e(TAG, "onLoadFinished "+response.getHttpCode() + " " + response.getMessage()); 
		if (!response.isSuccess()) {
			String msg = getResources().getString(R.string.account_error_login, response.getMessage());
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			finish();
			return;
		}

		String email_str = email.getText().toString();
		String password_str = password.getText().toString();
		String name_str;
		try {
			name_str = response.getJSON().getString("name");
		} catch (JSONException e) {
			Log.e(TAG, "JSON[name] "+e.getMessage());
			String msg = getResources().getString(R.string.account_error_login, "Invalid JSON");
			Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
			finish();
			return;
		}
		Toast.makeText(this, R.string.account_login_successful, Toast.LENGTH_SHORT).show();

		
		Log.e(TAG, "Login: create account");
		final Bundle userData = new Bundle();
		userData.putString(EXTRA_EMAIL_ADDRESS, email_str);
		userData.putString(EXTRA_NAME, name_str);
		
		final AccountManager mgr = AccountManager.get(this);
		final Account account = new Account(email_str, QuillAccount.ACCOUNT_TYPE);
		mgr.addAccountExplicitly(account, password_str, userData);

		setResult(RESULT_OK);
		finish();
	}

	@Override
	public void onLoaderReset(Loader<LoginLoader.Response> loader) {
	}
}
