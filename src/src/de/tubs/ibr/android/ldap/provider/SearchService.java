package de.tubs.ibr.android.ldap.provider;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

public class SearchService extends Service {

  private final IBinder mBinder = new SearchBinder();
  @Override
  public IBinder onBind(Intent arg0) {
    return null;
  }

}
