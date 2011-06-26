package de.tubs.ibr.android.ldap.sync;

//import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
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
import de.tubs.ibr.android.ldap.core.ContactManager;
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

  /**
   * Performs a sync on the Contacts saved on the local Phone comparing the
   * state on the LDAP Server
   * 
   * @param context
   * @param account
   * @param extras
   * @param authority
   * @param provider
   * @param syncResult
   * @throws OperationCanceledException
   */
  protected static void performSync(Context context, Account account,
      Bundle extras, String authority, ContentProviderClient provider,
      SyncResult syncResult) throws OperationCanceledException {
    // Contacts which has been deleted by the user are only marked as deleted,
    // but not really deleted, so they have to be deleted on the LDAP Server.
    // All Entries listed in this List, will be deleted on the LDAP and locally
    LinkedList<Integer> markedToBeDeleted = new LinkedList<Integer>();
    LinkedList<Integer> shouldBeAdded = new LinkedList<Integer>();
    HashMap<String, Integer> localContacts = new HashMap<String, Integer>();

    mContentResolver = context.getContentResolver();
    // Load the local synced contacts
    Uri rawContactUri = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(RawContacts.ACCOUNT_NAME, account.name)
        .appendQueryParameter(RawContacts.ACCOUNT_TYPE, account.type).build();
    Cursor c1 = mContentResolver.query(rawContactUri, new String[] {
        BaseColumns._ID, RawContacts.SOURCE_ID, RawContacts.DELETED,
        RawContacts.SYNC1 }, null, null, null);
    while (c1.moveToNext()) {
      final String sourceid = c1.getString(1);
      // If there is a Source ID in the database, this entry is in sync
      if (sourceid != null && !sourceid.equalsIgnoreCase("")) {
        localContacts.put(c1.getString(1), c1.getInt(0));
      } else {
        // if there is no source ID in the database and the status is
        // "locally added", this contact has to be added and synced with the
        // LDAP Directory
        String status = c1.getString(3);
        if (status != null && status.equalsIgnoreCase("locally added")) {
          shouldBeAdded.add(c1.getInt(0));
        }
      }
      // If this entry is marked as to be deleted, it should be cleaned up later
      if (c1.getInt(2) == 1) {
        markedToBeDeleted.add(c1.getInt(0));
      }
    }
    // Now search for all entries inside the Database
    boolean error = false;
    long userId;
    String ldapuid;
    int rawContactId = 0;
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
          SearchScope.SUB, filter, SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
          SearchRequest.ALL_USER_ATTRIBUTES);
      request.setSizeLimit(1000000);
      request.setTimeLimitSeconds(300);
      ldapresult = conn.search(request);
    } catch (LDAPSearchException lse) {
      error = true;
    } catch (LDAPException le) {
      error = true;
    } finally {
      if (conn != null) {
        conn.close();
      }
      if (error) {
        return;
      }
    }
    // Alle LDAP Kontakte überprüfen, ob diese lokal importiert werden müssen,
    // oder ob es bereits welche gibt und diese aktualisiert werden müssen.
    for (final SearchResultEntry user : ldapresult.getSearchEntries()) {
      ldapuid = user.getAttributeValue(AttributeMapper.ATTR_UID);
      // Check to see if the contact needs to be inserted or updated
      if (localContacts.containsKey(ldapuid)) {
        rawContactId = localContacts.get(ldapuid);
        // update contact
        ContactManager.updateLDAPContact(rawContactId, context, account, user,
            batchOperation);
        localContacts.remove(ldapuid);
      } else {
        // add remote contact to local
        ContactManager.addLDAPContactToAccount(user, account, batchOperation);
      }
    }
    // Falls es noch lokale Kontakte gibt, die entfernt nicht mehr vorhanden
    // sind, sollen diese aus dem Kontaktbuch gelöscht werden.
    if (!localContacts.isEmpty()) {
      for (Entry<String, Integer> deletelocal : localContacts.entrySet()) {
        ContactManager.deleteLocalContact(deletelocal.getValue(),
            context.getContentResolver());
        // If it is also locally marked to be deleted, the above call have done
        // it, or it is not necessary
        markedToBeDeleted.remove(deletelocal.getValue());
      }
    }
    // Try to deleted marked Entries for local Entries marked to be deleted
    for (Integer i : markedToBeDeleted) {
      ContactManager.deleteLDAPContact(i, batchOperation, new ServerInstance(
          accountManager, account), context);
    }
    // Try to add a local Contact to LDAP
    for (Integer i: shouldBeAdded){
      ContactManager.addLocalContactToLDAP(i, batchOperation, new ServerInstance(accountManager,account), context);
    }
    // A sync adapter should batch operations on multiple contacts,
    // because it will make a dramatic performance difference.
    batchOperation.execute();

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
