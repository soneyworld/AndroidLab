package de.tubs.ibr.android.ldap.provider;

import java.util.List;
import android.content.Context;

public abstract class TestServerRunnable implements Runnable {
  public boolean acceptable;
  public List<String> reasons;
  public Context context;
}
