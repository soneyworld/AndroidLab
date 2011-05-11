package de.tubs.ibr.android.ldap.sync;

import android.accounts.Account;
import android.accounts.OperationCanceledException;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.Context;
import android.content.SyncResult;
import android.os.Bundle;

public class LDAPSyncAdapter extends AbstractThreadedSyncAdapter {
  private Context mContext;
  public LDAPSyncAdapter(Context context, boolean autoInitialize) {
    super(context, autoInitialize);
    mContext = context;
  }

  @Override
  public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    try {
        LDAPSyncService.performSync(mContext, account, extras, authority, provider, syncResult);
       } catch (OperationCanceledException e) {
       }

  }

}
