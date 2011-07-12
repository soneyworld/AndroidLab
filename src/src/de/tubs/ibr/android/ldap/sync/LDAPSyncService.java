package de.tubs.ibr.android.ldap.sync;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import de.tubs.ibr.android.ldap.core.BatchOperation;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.core.ContactUtils;
import de.tubs.ibr.android.ldap.core.activities.ConflictActivity;
import android.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.BaseColumns;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

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

  private static boolean updateLDAPContact(int id, Context context,
      Account account, Entry entry, BatchOperation batch,
      ServerInstance instance) {
    boolean isConflictFree = true;
    if (entry.getAttribute(AttributeMapper.ATTR_UID) == null) {
      return isConflictFree;
    }
    Bundle localcontact = ContactManager.loadContact(id, context);
    SyncStatus status = SyncStatus.IN_SYNC;
    String statusmsg = localcontact
        .getString(ContactManager.LDAP_SYNC_STATUS_KEY);
    String dirty = localcontact
        .getString(ContactManager.LOCAL_ACCOUNT_DIRTY_KEY);
    if (statusmsg != null && statusmsg.length() > 0) {
      if (statusmsg.startsWith(ContactManager.SYNC_STATUS_CONFLICT)) {
        status = SyncStatus.CONFLICT;
      } else if (statusmsg.equalsIgnoreCase(ContactManager.SYNC_STATUS_IN_SYNC)) {
        if (dirty != null) {
          if (dirty.equalsIgnoreCase("1")) {
            status = SyncStatus.LOCALLY_MODIFIED;
          } else if (dirty.equalsIgnoreCase("0")) {
            status = SyncStatus.IN_SYNC;
          }
        } else if (statusmsg
            .startsWith(ContactManager.SYNC_STATUS_MARKED_AS_SOLVED)) {
          status = SyncStatus.MARK_AS_SOLVED;
        } else {
          return isConflictFree;
        }
      }
    }
    String ldif = localcontact.getString(ContactManager.LDAP_LDIF_DETAILS_KEY);
    if (ldif == null) {
      throw new IllegalArgumentException("LDIF File is NULL!!!");
    }
    LDIFReader reader = new LDIFReader(new BufferedReader(
        new StringReader(ldif)));
    Entry lastSyncState = null;
    try {
      lastSyncState = reader.readEntry();
    } catch (LDIFException e) {
      e.printStackTrace();
    } catch (IOException e) {
      e.printStackTrace();
    }
    Bundle actualremotestate = ContactManager
        .createMapableBundle(ContactManager.createBundleFromEntry(entry));
    Bundle lastsyncstate = null;
    if (lastSyncState != null) {
      lastsyncstate = ContactManager.createMapableBundle(ContactManager
          .createBundleFromEntry(lastSyncState));
    } else {
      lastsyncstate = new Bundle();
    }
    if (status == SyncStatus.IN_SYNC) {
      // Check if ldap has new Data
      saveLocalUpdate(id, entry.toLDIFString(), batch, actualremotestate,
          lastsyncstate);
    } else if (status == SyncStatus.LOCALLY_MODIFIED) {
      // Check if ldap has new Data and merge is possible
      Bundle actuallocalstate = ContactManager
          .createMapableBundle(localcontact);
      Bundle mergedstate = new Bundle();
      if (ContactManager.mergeBundle(actuallocalstate, actualremotestate,
          lastsyncstate, mergedstate, null)) {
        // Save merge state to server
        try {
          Entry updatedEntry = updateLDAPServerEntry(entry, mergedstate,
              instance);
          // Save merged state to local
          saveLocalUpdate(id, updatedEntry.toLDIFString(), batch, mergedstate,
              lastsyncstate);
        } catch (Exception e) {
          batch.add(ContentProviderOperation
              .newUpdate(
                  ContentUris.withAppendedId(getRawContactAsSyncAdapter(), id))
              .withValue(RawContacts.SYNC2, e.getMessage()).build());
        }

      } else {
        // Save as conflict
        batch.add(ContentProviderOperation
            .newUpdate(
                ContentUris.withAppendedId(getRawContactAsSyncAdapter(), id))
            .withValue(RawContacts.SYNC1, ContactManager.SYNC_STATUS_CONFLICT)
            .build());
        // Notify user
        isConflictFree = false;
      }
    } else if (status == SyncStatus.CONFLICT) {
      // try to solve conflict
      Bundle actuallocalstate = ContactManager
          .createMapableBundle(localcontact);
      Bundle mergedstate = new Bundle();
      if (ContactManager.mergeBundle(actuallocalstate, actualremotestate,
          lastsyncstate, mergedstate, null)) {
        // Merged update to server
        try {
          Entry updatedEntry = updateLDAPServerEntry(entry, mergedstate,
              instance);
          // Save merged state to local
          if (updatedEntry != null)
            saveLocalUpdate(id, updatedEntry.toLDIFString(), batch,
                mergedstate, lastsyncstate);
          // Notifications should be deleted
          isConflictFree = true;
        } catch (Exception e) {
          // Saving excption to local contact
          batch.add(ContentProviderOperation
              .newUpdate(
                  ContentUris.withAppendedId(getRawContactAsSyncAdapter(), id))
              .withValue(RawContacts.SYNC2, e.getMessage()).build());
        }
      } else {
        isConflictFree = false;
      }
    } else if (status == SyncStatus.MARK_AS_SOLVED) {
      // TODO Handle this
    }
    return isConflictFree;
  }

  private static Entry updateLDAPServerEntry(Entry oldstate,
      Bundle mergedstate, ServerInstance serverInstance) throws Exception {
    LDAPConnection connection = null;
    String dn = oldstate.getDN();
    Entry resultState = null;
    Bundle oldcontact = ContactManager.createMapableBundle(ContactManager
        .createBundleFromEntry(oldstate));
    try {
      connection = serverInstance.getConnection();
      List<Modification> modlist = new LinkedList<Modification>();
      Set<String> keyset = new LinkedHashSet<String>();
      keyset.addAll(oldcontact.keySet());
      keyset.addAll(mergedstate.keySet());
      for (String key : keyset) {
        String old = oldcontact.getString(key);
        String merged = mergedstate.getString(key);
        if (old != null && merged != null) {
          if (!old.equals(merged))
            modlist
                .add(new Modification(ModificationType.REPLACE, key, merged));
        } else if (merged != null) {
          modlist.add(new Modification(ModificationType.ADD, key, merged));
        } else if (old != null) {
          modlist.add(new Modification(ModificationType.DELETE, key));
        }
      }
      if (modlist.size() > 0) {
        LDAPResult result = connection.modify(dn, modlist);
        String errormessage = result.getResultCode().toString();
        if (result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE) {
          SearchRequest request = new SearchRequest(dn, SearchScope.BASE,
              Filter.create("(cn=*)"),
              SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
              SearchRequest.ALL_USER_ATTRIBUTES);
          SearchResult searchResults = connection.search(request);
          if (searchResults.getSearchEntries().size() >= 1) {
            resultState = searchResults.getSearchEntries().get(0);
          } else {
            throw new Exception("Error on updating LDAP entry \"" + dn
                + "\": Couldn't find resulting entry!");
          }
        } else if (!ResultCode.isClientSideResultCode(result.getResultCode())) {
          // TODO Notifiy the User about Server Problem
          throw new Exception(errormessage);
        }
      } else {
        SearchRequest request = new SearchRequest(dn, SearchScope.BASE,
            Filter.create("(cn=*)"), SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
            SearchRequest.ALL_USER_ATTRIBUTES);
        SearchResult searchResults = connection.search(request);
        if (searchResults.getSearchEntries().size() >= 1) {
          resultState = searchResults.getSearchEntries().get(0);
        } else {
          throw new Exception("Error on updating LDAP entry \"" + dn
              + "\": Couldn't find resulting entry!");
        }
      }
    } finally {
      if (connection != null) {
        connection.close();
      }
    }
    return resultState;
  }

  /**
   * @param id
   *          is the rawcontact id
   * @param ldif
   *          is the ldif of the actual synced state
   * @param batch
   * @param actualremotestate
   * @param lastsyncstate
   */
  private static void saveLocalUpdate(int id, String ldif,
      BatchOperation batch, Bundle actualremotestate, Bundle lastsyncstate) {
    Map<String, String> updateMap = new HashMap<String, String>();
    Set<String> insertKeys = new LinkedHashSet<String>();
    Set<String> deleteKeys = new LinkedHashSet<String>();
    for (String oldkey : lastsyncstate.keySet()) {
      if (actualremotestate.containsKey(oldkey)) {
        String oldobj = (String) lastsyncstate.get(oldkey);
        String newobj = (String) actualremotestate.get(oldkey);
        if (!(oldobj != null && oldobj.equals(newobj))) {
          updateMap.put(oldkey, newobj);
        }
      } else {
        deleteKeys.add(oldkey);
      }
    }
    for (String newkey : actualremotestate.keySet()) {
      if (!lastsyncstate.containsKey(newkey)) {
        insertKeys.add(newkey);
      }
    }
    if (insertKeys.size() != 0 || deleteKeys.size() != 0
        || updateMap.size() != 0) {
      ContactUtils.createUpdateBatch(insertKeys, deleteKeys, updateMap, batch,
          actualremotestate, lastsyncstate, getDataAsSyncAdapter(), id);
      batch.add(ContentProviderOperation
          .newUpdate(
              ContentUris.withAppendedId(getRawContactAsSyncAdapter(), id))
          .withValue(RawContacts.SYNC1, ContactManager.SYNC_STATUS_IN_SYNC)
          .withValue(RawContacts.SYNC2, "").withValue(RawContacts.SYNC4, ldif)
          .withValue(RawContacts.DIRTY, "0").build());
    }
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
  public static void performSync(Context context, Account account,
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
        if (status != null
            && status
                .equalsIgnoreCase(ContactManager.SYNC_STATUS_LOCALLY_ADDED)
            && c1.getInt(2) != 1) {
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
    String ldapuid;
    int rawContactId = 0;
    final ContentResolver resolver = context.getContentResolver();
    final BatchOperation batchOperation = new BatchOperation(context, resolver);
    LDAPConnection conn = null;
    AccountManager accountManager = AccountManager.get(context);
    ServerInstance instance = new ServerInstance(accountManager, account);
    List<SearchResultEntry> ldapresult = null;
    int conflictCount = 0;
    try {
      conn = instance.getConnection();
      // If a filter was saved for this account, respect him, otherwise use
      // default filter
      String f = instance.getFilter();
      if (f == null || f.length() == 0) {
        f = "(cn=*)";
      }
      final Filter filter = Filter.create(f);
      final SearchRequest request = new SearchRequest(instance.getBaseDN(),
          SearchScope.SUB, filter, SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
          SearchRequest.ALL_USER_ATTRIBUTES);
      request.setSizeLimit(1000000);
      request.setTimeLimitSeconds(300);
      if (instance.isSyncAllContacts()) {
        ldapresult = conn.search(request).getSearchEntries();
      } else {
        ldapresult = scanImportedContacts(conn, context);
      }
    } catch (LDAPSearchException lse) {
      // If the
      if (lse.getResultCode().isConnectionUsable()
          && !lse.getResultCode().isClientSideResultCode()) {
        try {
          if (instance.isSyncAllContacts()) {
            ldapresult = scanImportedContacts(conn, context);
          }
        } catch (LDAPException e) {
          error = true;
        }
      } else {
        error = true;
      }
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
    for (final SearchResultEntry user : ldapresult) {
      ldapuid = user.getAttributeValue(AttributeMapper.ATTR_UID);
      // Check to see if the contact needs to be inserted or updated
      if (localContacts.containsKey(ldapuid)) {
        rawContactId = localContacts.get(ldapuid);
        // update contact
        if (!LDAPSyncService.updateLDAPContact(rawContactId, context, account,
            user, batchOperation, instance)) {
          conflictCount++;
        }
        localContacts.remove(ldapuid);
        saveLDIFtoPreferences(ldapuid, user, context);
      } else {
        // add remote contact to local
        ContactManager.addLDAPContactToAccount(user, account, batchOperation);
      }
      saveLDIFtoPreferences(ldapuid, user, context);
    }
    // Falls es noch lokale Kontakte gibt, die entfernt nicht mehr vorhanden
    // sind, sollen diese aus dem Kontaktbuch gelöscht werden.
    if (!localContacts.isEmpty()) {
      for (java.util.Map.Entry<String, Integer> deletelocal : localContacts
          .entrySet()) {
        if (instance.isSyncAllContacts()) {
          ContactManager.deleteLocalContact(deletelocal.getValue(),
              context.getContentResolver());
          // If it is also locally marked to be deleted, the above call have
          // done
          // it, or it is not necessary
          markedToBeDeleted.remove(deletelocal.getValue());
        }
      }
    }
    // Try to deleted marked Entries for local Entries marked to be deleted
    for (Integer i : markedToBeDeleted) {
      if (!instance.isSyncAllContacts()) {
        Bundle contact = ContactManager.loadContact(i, context);
        String status = contact.getString(ContactManager.LDAP_SYNC_STATUS_KEY);
        if (status != null
            && !status.equalsIgnoreCase(ContactManager.SYNC_STATUS_DO_NOT_SYNC)) {
          ContactManager.deleteLDAPContact(i, batchOperation,
              new ServerInstance(accountManager, account), context);
        } else {
          ContactManager.deleteLocalContact(i, resolver);
        }
      } else {
        ContactManager.deleteLDAPContact(i, batchOperation, new ServerInstance(
            accountManager, account), context);
      }
    }
    // Try to add a local Contact to LDAP
    for (Integer i : shouldBeAdded) {
      ContactManager.addLocalContactToLDAP(i, batchOperation,
          new ServerInstance(accountManager, account), context);
    }
    // A sync adapter should batch operations on multiple contacts,
    // because it will make a dramatic performance difference.
    batchOperation.execute();
    if (conflictCount > 0) {
      final NotificationManager nm = (NotificationManager) context
          .getSystemService(Context.NOTIFICATION_SERVICE);
      String text = "Conflict happend on sync";
      Notification notification = new Notification(
          R.drawable.stat_notify_error, text, System.currentTimeMillis());
      final Intent notifyIntent = new Intent(context, ConflictActivity.class);
      notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      final PendingIntent startIntent = PendingIntent.getActivity(context, 0,
          notifyIntent, 0);
      if (conflictCount > 1) {
        notification.setLatestEventInfo(context, "Sync conflicts", "solve "
            + conflictCount + " conflicts", startIntent);
      } else {
        notification.setLatestEventInfo(context, "Sync conflict",
            "solve conflict", startIntent);
      }
      nm.notify(ConflictActivity.CONFLICT_LIST, notification);
    } else {
      final NotificationManager nm = (NotificationManager) context
          .getSystemService(Context.NOTIFICATION_SERVICE);
      nm.cancel(ConflictActivity.CONFLICT_LIST);
    }
  }

  private static void saveLDIFtoPreferences(String attributeUUID,
      SearchResultEntry user, Context c) {
    SharedPreferences settings = c.getSharedPreferences("remoteState",
        MODE_PRIVATE);
    Editor editor = settings.edit();
    editor.putString(attributeUUID, user.toLDIFString());
    editor.commit();
  }

  private static List<SearchResultEntry> scanImportedContacts(
      LDAPConnection connection, Context context) throws LDAPException {
    List<SearchResultEntry> result = new LinkedList<SearchResultEntry>();
    LinkedHashMap<Integer, Bundle> localcontactoverview = ContactManager
        .loadContactList(context);
    for (Bundle localcontact : localcontactoverview.values()) {
      String syncstatus = localcontact
          .getString(ContactManager.LDAP_SYNC_STATUS_KEY);
      if (syncstatus != null
          && !syncstatus
              .equalsIgnoreCase(ContactManager.SYNC_STATUS_DO_NOT_SYNC)) {
        String dn = localcontact.getString(AttributeMapper.DN);
        if (dn != null) {
          SearchRequest request = new SearchRequest(dn, SearchScope.BASE,
              Filter.create("(cn=*)"),
              SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
              SearchRequest.ALL_USER_ATTRIBUTES);
          SearchResult searchResults = connection.search(request);
          if (searchResults.getSearchEntries().size() == 1)
            result.add(searchResults.getSearchEntries().get(0));
        }
      }
    }
    return result;
  }

  private static Uri getDataAsSyncAdapter() {
    Uri data = Data.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    return data;
  }

  private static Uri getRawContactAsSyncAdapter() {
    Uri data = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    return data;
  }

  // private static Entry createLDAPEntryFromBundle(Bundle contact, String dn) {
  // Entry ldapentry = new Entry(dn);
  // for (String attr : contact.keySet()) {
  // String value = contact.getString(attr);
  // if (value != null && AttributeMapper.isContactAttr(attr)) {
  // ldapentry.addAttribute(attr, value);
  // }
  // }
  // ldapentry.addAttribute("objectClass", "inetOrgPerson");
  // ldapentry.addAttribute("objectClass", "organizationalPerson");
  // ldapentry.addAttribute("objectClass", "person");
  // ldapentry.addAttribute("objectClass", "top");
  // return ldapentry;
  // }
}
