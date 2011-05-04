/*
 * Copyright (C) 2010 The Android Open Source Project
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package de.tubs.ibr.android.ldap.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

/**
 * This class is an implementation of AbstractAccountAuthenticator for
 * authenticating accounts in the com.example.android.samplesync domain.
 */
class Authenticator extends AbstractAccountAuthenticator {
  // Authentication Service context
  private final Context mContext;

  public Authenticator(Context context) {
    super(context);
    mContext = context;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bundle addAccount(AccountAuthenticatorResponse response,
      String accountType, String authTokenType, String[] requiredFeatures,
      Bundle options) {
    final Intent intent = new Intent(mContext, AddServer.class);
    intent.setAction("de.tubs.ibr.android.ldap.auth.AddServer.LOGIN");
    intent
        .putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
    final Bundle bundle = new Bundle();
    bundle.putParcelable(AccountManager.KEY_INTENT, intent);
    return bundle;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bundle confirmCredentials(AccountAuthenticatorResponse response,
      Account account, Bundle options) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response,
      String accountType) {
    throw new UnsupportedOperationException();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bundle getAuthToken(AccountAuthenticatorResponse response,
      Account account, String authTokenType, Bundle loginOptions) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getAuthTokenLabel(String authTokenType) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bundle hasFeatures(AccountAuthenticatorResponse response,
      Account account, String[] features) {
    return null;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Bundle updateCredentials(AccountAuthenticatorResponse response,
      Account account, String authTokenType, Bundle loginOptions) {
    return null;
  }

}
