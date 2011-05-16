package de.tubs.ibr.android.ldap.provider;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;

/**
 * @author Till Lorentzen This Content Provider gives access to the registered
 *         LDAP Server Connections.
 */
public class LDAPContentProvider extends ContentProvider implements
    OnAccountsUpdateListener {
  public static final String AUTHORITY = "de.tubs.ibr.android.ldap";
  public static final Uri AUTHORITY_URI = Uri.parse("content://" + AUTHORITY);
  public static final String LDAP_MODIFICATION_KEY = "LDAP_MODIFICATION";
  private LinkedList<ServerInstance> instances;
  private Handler updateHandler = new Handler();
  private AccountManager accManager;

  @Override
  public int delete(Uri uri, String selection, String[] selectionArgs) {
    ServerInstance instance = getServerInstance(uri);
    if (instance != null) {
      try {
        LDAPConnection conn = instance.getConnection();
        DeleteRequest deleteRequest = new DeleteRequest(selection);
        conn.delete(deleteRequest);
      } catch (LDAPException e) {
        return 0;
      }
    } else {
      throw new IllegalArgumentException("No matching ServerInstance found");
    }
    return 0;
  }

  @Override
  public String getType(Uri uri) {
    return null;
  }

  /**
   * Inserts a given Entry to LDAP Directory
   * 
   * @see android.content.ContentProvider#insert(android.net.Uri,
   *      android.content.ContentValues)
   */
  @Override
  public Uri insert(Uri uri, ContentValues values) {
    ServerInstance i = getServerInstance(uri);
    if (i != null) {
      try {
        LDAPConnection conn = i.getConnection();
        Attribute[] attributes = new Attribute[values.valueSet().size()];
        Iterator<Entry<String, Object>> iter = values.valueSet().iterator();
        for (int j = 0; j < values.size(); j++) {
          Entry<String, Object> entry = iter.next();
          attributes[j] = new Attribute(entry.getKey(), entry.getValue()
              .toString());
        }
        conn.add(i.getBaseDN(), attributes);
      } catch (LDAPException e) {
        return null;
      }
    } else {
      throw new IllegalArgumentException("No matching ServerInstance found");
    }
    return null;
  }

  /*
   * (non-Javadoc)
   * @see android.content.ContentProvider#onCreate()
   */
  @Override
  public boolean onCreate() {
    // Create ServerInstances for each registered LDAP Account
    accManager = AccountManager.get(this.getContext());
    Account[] accounts = accManager.getAccountsByType(this.getContext()
        .getString(R.string.ACCOUNT_TYPE));
    this.instances = new LinkedList<ServerInstance>();
    for (Account account : accounts) {
      this.instances.add(new ServerInstance(accManager, account));
    }
    // Add Listener for account updates
    accManager.addOnAccountsUpdatedListener(this, updateHandler, true);
    return true;
  }

  @Override
  public Cursor query(Uri uri, String[] projection, String selection,
      String[] selectionArgs, String sortOrder) {
    ServerInstance i = getServerInstance(uri);
    if (i != null) {
      LDAPConnection connection;
      try {
        connection = i.getConnection();
        return new LDAPCursor(connection, "", SearchScope.SUB, "");
      } catch (LDAPException e1) {
        // TODO Auto-generated catch block
        e1.printStackTrace();
      }
    }
    return null;
  }

  @Override
  public int update(Uri uri, ContentValues values, String selection,
      String[] selectionArgs) {
    ServerInstance instance = getServerInstance(uri);
    if (instance != null) {
      try {
        LDAPConnection conn = instance.getConnection();
        Attribute[] attributes = new Attribute[values.valueSet().size()];
        Iterator<Entry<String, Object>> iter = values.valueSet().iterator();
        for (int j = 0; j < values.size(); j++) {
          Entry<String, Object> entry = iter.next();
          attributes[j] = new Attribute(entry.getKey(), entry.getValue()
              .toString());
        }
        if (values.containsKey("LDAP_MODIFICATION")) {
          Modification modification = (Modification) values
              .get("LDAP_MODIFICATION");
          LDAPResult result = conn.modify("uid=" + uri.getPathSegments().get(1)
              + instance.getBaseDN(), modification);
          if (result.getResultCode() == ResultCode.SUCCESS) {
            return 1;
          } else {
            return 0;
          }
        }
        return 0;
      } catch (LDAPException e) {
        return 0;
      }
    } else {
      throw new IllegalArgumentException("No matching ServerInstance found");
    }
  }

  /**
   * Extracts the ServerInstance Information from the URI and searches for the
   * instance
   * 
   * @param uri
   *          of request
   * @return found instance or null
   */
  private ServerInstance getServerInstance(Uri uri) {
    List<String> segments = uri.getPathSegments();
    if (segments.size() >= 1) {
      String id = segments.get(0);
      for (ServerInstance instance : this.instances) {
        if (instance.getID().equals(id)) {
          return instance;
        }
      }
    }
    return null;
  }

  /**
   * On Account update, the provider updates his instance list
   * 
   * @see android.accounts.OnAccountsUpdateListener#onAccountsUpdated(android.accounts.Account[])
   */
  @Override
  public void onAccountsUpdated(Account[] accounts) {
    this.instances.clear();
    for (Account account : accounts) {
      if (account.type.equals(this.getContext()
          .getString(R.string.ACCOUNT_TYPE))) {
        this.instances.add(new ServerInstance(accManager, account));
      }
    }
  }
}
