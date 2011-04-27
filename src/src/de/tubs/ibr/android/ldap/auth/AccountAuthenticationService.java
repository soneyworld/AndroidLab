package de.tubs.ibr.android.ldap.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

/**
 * Service to handle Account authentication. It instantiates the authenticator
 * and returns its IBinder.
 */
public class AccountAuthenticationService extends Service {
  private static Authenticator mAuthenticator = null;

  public AccountAuthenticationService() {
    super();
  }

  @Override
  public void onCreate() {
    mAuthenticator = new Authenticator(this);
  }

  @Override
  public void onDestroy() {

  }

  @Override
  public IBinder onBind(Intent intent) {
    return getAuthenticator().getIBinder();
  }

  private Authenticator getAuthenticator() {
    if (mAuthenticator == null)
      mAuthenticator = new Authenticator(this);
    return mAuthenticator;
  }

}
