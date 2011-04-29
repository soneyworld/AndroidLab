package com.syncauthenticator;

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

public class SyncAuthenticatorService extends Service {
	 private static final String TAG = "AccountAuthenticatorService";
	 private static SyncAuthenticatorImpl sSyncAuthenticator = null;
	 
	 public SyncAuthenticatorService() {
	  super();
	 }
	 
	 public IBinder onBind(Intent intent) {
	  IBinder ret = null;
	  if (intent.getAction().equals(android.accounts.AccountManager.ACTION_AUTHENTICATOR_INTENT))
	   ret = getAuthenticator().getIBinder();
	  return ret;
	 }
	 
	 private SyncAuthenticatorImpl getAuthenticator() {
	  if (sSyncAuthenticator == null)
	   sSyncAuthenticator = new SyncAuthenticatorImpl(this);
	  return sSyncAuthenticator;
	 }
	 
	 private static class SyncAuthenticatorImpl extends AbstractAccountAuthenticator {
	  private Context mContext;
	 
	  public SyncAuthenticatorImpl(Context context) {
	   super(context);
	   mContext = context;
	  }
	 
	  /*
	   *  The user has requested to add a new account to the system.  We return an intent that will launch our login screen if the user has not logged in yet,
	   *  otherwise our activity will just pass the user's credentials on to the account manager.
	   */
	  @Override
	  public Bundle addAccount(AccountAuthenticatorResponse response, String accountType, String authTokenType, String[] requiredFeatures, Bundle options)
	    throws NetworkErrorException {
	   Bundle reply = new Bundle();
	   
	   Intent i = new Intent(mContext, SyncAuthenticatorActivity.class);
	   i.setAction("com.synthauthenticator.LOGIN");
	   i.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
	   reply.putParcelable(AccountManager.KEY_INTENT, i);
	   
	   return reply;
	  }
	 
	  @Override
	  public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) {
	   return null;
	  }
	 
	  @Override
	  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
	   return null;
	  }
	 
	  @Override
	  public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) throws NetworkErrorException {
	   return null;
	  }
	 
	  @Override
	  public String getAuthTokenLabel(String authTokenType) {
	   return null;
	  }
	 
	  @Override
	  public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
	   return null;
	  }
	 
	  @Override
	  public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle options) {
	   return null;
	  }
	 }
	}
