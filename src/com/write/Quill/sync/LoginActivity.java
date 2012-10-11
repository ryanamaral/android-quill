package com.write.Quill.sync;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.EditText;
import android.widget.ProgressBar;

import com.write.Quill.ActivityBase;
import com.write.Quill.R;

public class LoginActivity 
	extends ActivityBase 
	implements OnCheckedChangeListener, OnClickListener, TextWatcher {
	
	private final static String TAG = "LoginActivity";
	
	private EditText email, password;
	private Button newButton, okButton, cancelButton;
	private CheckBox showPassword;
	private ProgressBar progress;
	    
	private UserProfile profile;
	
	// Intent actions
	public final static String ACTION_LOGIN = "account_login";
	
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
		} 
		if (v == cancelButton) {
			enable(true);
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
	
}
