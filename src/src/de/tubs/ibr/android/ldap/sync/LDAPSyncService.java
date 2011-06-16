package de.tubs.ibr.android.ldap.sync;

//import java.util.ArrayList;
import java.util.HashMap;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import de.tubs.ibr.android.ldap.core.BatchOperation;
import de.tubs.ibr.android.ldap.provider.LDAPService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.ContentProviderClient;
//import android.content.ContentProviderOperation;
import android.content.ContentResolver;
//import android.content.ContentUris;
import android.content.Context;
//import android.provider.ContactsContract.RawContacts.Entity;
import android.content.Intent;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
//import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.util.Log;

public class LDAPSyncService extends Service {
  private static final Object sSyncAdapterLock = new Object();
  private static LDAPSyncAdapter sSyncAdapter = null;
  private static ContentResolver mContentResolver = null;

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

  protected static void performSync(Context context, Account account,
      Bundle extras, String authority, ContentProviderClient provider,
      SyncResult syncResult) throws OperationCanceledException {
    HashMap<String, Long> localContacts = new HashMap<String, Long>();
    mContentResolver = context.getContentResolver();
    // Load the local Last.fm contacts
    Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
        .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build();
    Cursor c1 = mContentResolver.query(rawContactUri, new String[] {
        BaseColumns._ID, RawContacts.SYNC1 }, null, null, null);
    while (c1.moveToNext()) {
      localContacts.put(c1.getString(1), c1.getLong(0));
    }
    long userId;
    long rawContactId = 0;
    final ContentResolver resolver = context.getContentResolver();
    final BatchOperation batchOperation = new BatchOperation(context, resolver);
    LDAPConnection conn = null;
    AccountManager accountManager = AccountManager.get(context);
    ServerInstance instance = new ServerInstance(accountManager, account);
    SearchResult ldapresult = null;
    try {
      conn = instance.getConnection();
      final Filter filter = Filter.create("(cn=*)");
      final SearchRequest request = new SearchRequest(instance.getBaseDN(),
          SearchScope.SUB, filter);
      request.setSizeLimit(1000000);
      request.setTimeLimitSeconds(300);
      ldapresult = conn.search(request);
    } catch (LDAPSearchException lse) {
    } catch (LDAPException le) {
    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    
//    for (final SearchResultEntry user : ldapresult.getSearchEntries()) {
//      userId = user.getUserId();
//      // Check to see if the contact needs to be inserted or updated
//      rawContactId = lookupRawContact(resolver, userId);
//      if (rawContactId != 0) {
//        if (!user.isDeleted()) {
//          // update contact
//          updateContact(context, resolver, account, user, rawContactId,
//              batchOperation);
//        } else {
//          // delete contact
//          deleteContact(context, rawContactId, batchOperation);
//        }
//      } else {
//        // add new contact
//        Log.d(TAG, "In addContact");
//        if (!user.isDeleted()) {
//          addContact(context, account, user, batchOperation);
//        }
//      }
//      // A sync adapter should batch operations on multiple contacts,
//      // because it will make a dramatic performance difference.
//      if (batchOperation.size() >= 50) {
//        batchOperation.execute();
//      }
//    }
//    batchOperation.execute();
    // TODO use LDAP Content Provider to search for contacts, which has to be
    // synchronized
    // ArrayList<ContentProviderOperation> operationList = new
    // ArrayList<ContentProviderOperation>();

    // LastFmServer server = AndroidLastFmServerFactory.getServer();
    // try {
    // Friends friends = server.getFriends(account.name, "", "50");
    // for (User user : friends.getFriends()) {
    // if (!localContacts.containsKey(user.getName())) {
    // if (user.getRealName().length() > 0)
    // addContact(account, user.getRealName(), user.getName());
    // else
    // addContact(account, user.getName(), user.getName());
    // } else {
    // updateContactStatus(operationList,
    // localContacts.get(user.getName()), tracks[0]);
    // }
    // }
    // }
    // if (operationList.size() > 0)
    // mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
    // } catch (Exception e1) {
    // e1.printStackTrace();
    // }

  }

  // private static void addContact(Account account, String name, String uuid) {
  // ArrayList<ContentProviderOperation> operationList = new
  // ArrayList<ContentProviderOperation>();
  // // Create our RawContact
  // ContentProviderOperation.Builder builder = ContentProviderOperation
  // .newInsert(RawContacts.CONTENT_URI);
  // builder.withValue(RawContacts.ACCOUNT_NAME, account.name);
  // builder.withValue(RawContacts.SYNC1, uuid);
  // operationList.add(builder.build());
  // // Create a Data record of common type 'StructuredName' for our RawContact
  // builder = ContentProviderOperation
  // .newInsert(ContactsContract.Data.CONTENT_URI);
  // builder.withValueBackReference(
  // ContactsContract.CommonDataKinds.StructuredName.RAW_CONTACT_ID, 0);
  // builder.withValue(ContactsContract.Data.MIMETYPE,
  // ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
  // builder.withValue(
  // ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
  // operationList.add(builder.build());
  //
  // // Create a Data record of custom type
  // // "vnd.android.cursor.item/vnd.fm.last.android.profile" to display a link
  // // to the Last.fm profile
  // builder = ContentProviderOperation
  // .newInsert(ContactsContract.Data.CONTENT_URI);
  // builder.withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0);
  // builder.withValue(ContactsContract.Data.MIMETYPE,
  // "vnd.android.cursor.item/vnd.fm.last.android.profile");
  // builder.withValue(ContactsContract.Data.DATA1, uuid);
  // builder.withValue(ContactsContract.Data.DATA2, "LDAP Profile");
  // builder.withValue(ContactsContract.Data.DATA3, "View profile");
  // operationList.add(builder.build());
  //
  // try {
  // mContentResolver.applyBatch(ContactsContract.AUTHORITY, operationList);
  // } catch (Exception e) {
  // e.printStackTrace();
  // }
  // }

}
