package de.tubs.ibr.android.ldap.provider;

import java.util.LinkedList;
import java.util.List;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;

public class LDAPContentProvider extends ContentProvider {
  public static final String AUTHORITY = "de.tubs.ibr.android.ldap";
  public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
  private LinkedList<ServerInstance> instances;

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    return 0;
  }

  @Override
  public String getType(Uri uri) {
    
    return null;
  }

  @Override
  public Uri insert(Uri uri, ContentValues values) {
    return null;
  }

  /* (non-Javadoc)
   * @see android.content.ContentProvider#onCreate()
   */
  @Override
  public boolean onCreate() {
    // Create ServerInstances for each registered LDAP Account
    AccountManager accManager = AccountManager.get(this.getContext());
    Account[] accounts = accManager.getAccountsByType(this.getContext()
        .getString(R.string.ACCOUNT_TYPE));
    this.instances = new LinkedList<ServerInstance>();
    for (Account account : accounts) {
      this.instances.add(new ServerInstance(accManager, account));
    }
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    return null;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    return 0;
  }

  private ServerInstance getServerInstance(Uri uri){
    List<String> segments = uri.getPathSegments();
    if(segments.size()>=1){
      String id = segments.get(0);
      for (ServerInstance instance : this.instances) {
        if(instance.getID().equals(id)){
          return instance;
        }
      }
    }
    return null;
  }
}
