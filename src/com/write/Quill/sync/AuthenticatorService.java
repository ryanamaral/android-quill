package com.write.Quill.sync;

import org.apache.http.impl.client.AbstractAuthenticationHandler;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

public class AuthenticatorService extends Service {
	private final static String TAG = "AuthenticatorService";
	
	private static class Authenticator extends AbstractAccountAuthenticator {
		private final static String TAG = "Authenticator";
		private final Context context;
		
		public Authenticator(Context context) {
			super(context);
			this.context = context;
		}

		@Override
		public Bundle addAccount(AccountAuthenticatorResponse response,
				String accountType, String authTokenType,
				String[] requiredFeatures, Bundle options)
				throws NetworkErrorException {
			Log.e(TAG, "addAccount");
			
			// check if an account already exists; we only allow one 
			QuillAccount account = new QuillAccount(context);
			if (account.exists())
				return null;
			
			// ok, go ahead and create new account
			Intent intent = new Intent(context, LoginActivity.class);
			intent.setAction(LoginActivity.ACTION_LOGIN);
			intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
			Bundle reply = new Bundle();
			reply.putParcelable(AccountManager.KEY_INTENT, intent);
			return reply;
		}

		@Override
		public Bundle confirmCredentials(AccountAuthenticatorResponse response,
				Account account, Bundle options) throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle editProperties(AccountAuthenticatorResponse response,
				String accountType) {
			return null;
		}

		@Override
		public Bundle getAuthToken(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			return null;
		}

		@Override
		public String getAuthTokenLabel(String authTokenType) {
			return null;
		}

		@Override
		public Bundle hasFeatures(AccountAuthenticatorResponse response,
				Account account, String[] features)
				throws NetworkErrorException {
			return null;
		}

		@Override
		public Bundle updateCredentials(AccountAuthenticatorResponse response,
				Account account, String authTokenType, Bundle options)
				throws NetworkErrorException {
			return null;
		}
		
	}
	
	 private static Authenticator authenticator = null;
	 
	 protected Authenticator getAuthenticator() {
		 if (authenticator == null) {
			 authenticator = new Authenticator(this);
		 }
		 return authenticator;
	 }

	
	@Override
	public IBinder onBind(Intent intent) {
		if (intent.getAction().equals(AccountManager.ACTION_AUTHENTICATOR_INTENT))
		   return getAuthenticator().getIBinder();
		else
			return null;
	}
}
