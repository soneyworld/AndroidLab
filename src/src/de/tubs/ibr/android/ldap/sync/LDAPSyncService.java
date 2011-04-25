package de.tubs.ibr.android.ldap.sync;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class LDAPSyncService extends Service {
  private static final Object sSyncAdapterLock = new Object();
  private static LDAPSyncAdapter sSyncAdapter = null;

  /*
   * {@inheritDoc}
   */
  @Override
  public void onCreate() {
    synchronized (sSyncAdapterLock) {
      if (sSyncAdapter == null) {
        sSyncAdapter = new LDAPSyncAdapter(getApplicationContext(), true);
      }
    }
  }

  @Override
  public IBinder onBind(Intent arg0) {
    return sSyncAdapter.getSyncAdapterBinder();
  }

}
