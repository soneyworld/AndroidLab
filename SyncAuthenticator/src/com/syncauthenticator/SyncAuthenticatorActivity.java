package com.syncauthenticator;

import android.accounts.AccountAuthenticatorActivity;
import android.os.Bundle;

public class SyncAuthenticatorActivity extends AccountAuthenticatorActivity {
    /** Called when the activity is first created. */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.add_server);
    }
}