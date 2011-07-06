package de.tubs.ibr.android.ldap.core;

import static com.unboundid.util.StaticUtils.EOL;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts.Entity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Photo;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import android.widget.Toast;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class ContactManager {

  public enum SyncType {
    LOCALLY_ADDED, DO_NOT_SYNC, IN_SYNC
  }

  public static final String SYNC_STATUS_LOCALLY_ADDED = "locally added";

  public static final String SYNC_STATUS_DO_NOT_SYNC = "";

  public static final String SYNC_STATUS_IN_SYNC = "inSync";

  public static final String SYNC_STATUS_CONFLICT = "conflict";

  public static final String LDAP_SYNC_STATUS_KEY = "LDAP_SYNC_STATUS";

  public static final String LDAP_ERROR_MESSAGE_KEY = "LDAP_ERROR_MESSAGE";

  public static final String LDAP_LDIF_DETAILS_KEY = "LDAP_LDIF_DETAILS";

  public static final String LOCAL_ACCOUNT_TYPE_KEY = "ANDROID_LOCAL_ACCOUNT_TYPE";

  public static final String LOCAL_ACCOUNT_NAME_KEY = "ANDROID_LOCAL_ACCOUNT_NAME";

  public static final String LDAP_SOURCE_ID_KEY = AttributeMapper.ATTR_UID;

  public static void addLDAPContactToAccount(Entry entry, Account account,
      BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
    Bundle contact = createBundleFromEntry(entry);
    // Check if the Entry is a Person and can be synchronized
    boolean isSyncable = false;
    String sourceId = entry.getAttributeValue(AttributeMapper.ATTR_UID);
    for (String string : entry.getAttributeValues("objectClass")) {
      if (string.equalsIgnoreCase("inetOrgPerson") && sourceId != null) {
        isSyncable = true;
      }
    }
    if (!isSyncable) {
      return;
    }
    // List all LDAP ObjectClasses
    String objectClass = "";
    for (String objectClazz : entry.getAttributeValues("objectClass")) {
      objectClass = objectClass + EOL + objectClazz;
    }
    Uri contentAsSyncAdapter = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    Uri dataAsSyncAdapter = Data.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    addSyncFieldsToBundle(contact, sourceId, SYNC_STATUS_IN_SYNC, "",
        entry.getDN() + objectClass, entry.toLDIFString());
    ContactUtils.createFullContact(account, contact, batch,
        contentAsSyncAdapter, dataAsSyncAdapter, rawContactInsertIndex);
    ContactUtils.createLDAPRow(AttributeMapper.UID,
        entry.getAttributeValue(AttributeMapper.UID), batch, dataAsSyncAdapter,
        rawContactInsertIndex);
    // TODO CREATE ORGANIZATION
  }

  private static void addSyncFieldsToBundle(Bundle b, String sourceId,
      String syncstatus, String message, String dn, String ldif) {
    b.putString(AttributeMapper.DN, dn);
    b.putString(ContactManager.LDAP_SYNC_STATUS_KEY, syncstatus);
    b.putString(ContactManager.LDAP_LDIF_DETAILS_KEY, ldif);
    b.putString(AttributeMapper.ATTR_UID, sourceId);
  }

  private static Bundle createBundleFromEntry(Entry entry) {
    Bundle contact = new Bundle();
    for (Attribute attr : entry.getAttributes()) {
      if (AttributeMapper.isContactAttr(attr.getName())) {
        contact.putString(attr.getName(), attr.getValue());
      }
    }
    return contact;
  }

  /**
   * @param entry
   * @param account
   * @param batch
   */
  public static void importLDAPContact(Entry entry, Account account,
      BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
    Bundle contact = createBundleFromEntry(entry);
    // List all LDAP ObjectClasses
    Uri contentAsSyncAdapter = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    Uri dataAsSyncAdapter = Data.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    ContactUtils.createFullContact(account, contact, batch,
        contentAsSyncAdapter, dataAsSyncAdapter, rawContactInsertIndex);

  }

  // public static void exportContactToLDAP(long id, BatchOperation batch) {
  //
  // }

  /**
   * Deletes a local {@link RawContacts} from the {@link Contacts} with the
   * given {@link RawContacts} database id. Calls the delete execution with
   * {@link ContactsContract}.CALLER_IS_SYNCADAPTER flag.
   * 
   * @param id
   * @param batch
   */
  public static void deleteLocalContact(int id, ContentResolver resolver) {
    Uri contentAsSyncAdapter = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    resolver.delete(contentAsSyncAdapter, "_ID=" + id, null);
  }

  /**
   * Tries to delete the Contact on the remote LDAP Server
   * 
   * @param id
   * @param batch
   */
  public static void deleteLDAPContact(final int id, BatchOperation batch,
      final ServerInstance instance, final Context context) {
    boolean remotelyDeleted = false;
    LDAPConnection conn = null;
    try {
      conn = instance.getConnection();
      String dn = loadContact(id, context).getString(AttributeMapper.DN);
      if (dn == null || dn.length() == 0) {
        remotelyDeleted = true;
      } else {
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        LDAPResult deleteResult = conn.delete(deleteRequest);
        if (deleteResult.getMessageID() == ResultCode.SUCCESS_INT_VALUE
            || deleteResult.getMessageID() == ResultCode.NO_SUCH_OBJECT_INT_VALUE) {
          remotelyDeleted = true;
        } else {
          updateLDAPMessage(id, "LDAP Error on deletetion: "
              + deleteResult.getResultCode().intValue() + EOL
              + deleteResult.getResultCode().toString(), batch);
        }
      }
    } catch (LDAPException e) {

    } finally {
      if (conn != null) {
        conn.close();
      }
    }
    if (remotelyDeleted) {
      deleteLocalContact(id, context.getContentResolver());
    }
  }

  public static void updateLDAPContact(int id, Context context,
      Account account, Entry entry, BatchOperation batch) {
    if (entry.getAttribute(AttributeMapper.ATTR_UID) == null) {
      return;
    }
    Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, id);
    Uri entityUri = Uri.withAppendedPath(rawContactUri,
        Entity.CONTENT_DIRECTORY);
    Cursor c = context.getContentResolver().query(
        entityUri,
        new String[] { RawContacts.SOURCE_ID, Entity.DATA_ID, Entity.MIMETYPE,
            Entity.DATA1 }, null, null, null);
    try {
      while (c.moveToNext()) {
        String sourceId = c.getString(0);
        if (!c.isNull(1)) {
          String mimeType = c.getString(2);
          String data = c.getString(3);
        }
      }
    } finally {
      c.close();
    }
  }

  /**
   * Adds the provided phone number to the contact.
   * 
   * @param number
   *          The number to add.
   * @param type
   *          The type of number to add.
   * @param uri
   *          The base URI for the contact.
   * @return a addPhoneNumber Operation
   */
  private static ContentProviderOperation updateLocallyAddedToSyncStatus(
      final int id, final String status, final Uri ContactAsSyncAdapter,
      final String ldif, final String uuid, final String dn) {
    return ContentProviderOperation
        .newUpdate(ContentUris.withAppendedId(ContactAsSyncAdapter, id))
        .withValue(RawContacts.SYNC1, status).withValue(RawContacts.SYNC3, dn)
        .withValue(RawContacts.SYNC4, ldif)
        .withValue(RawContacts.SOURCE_ID, uuid)
        .withValue(RawContacts.DIRTY, "0").build();
  }

  /**
   * Adds a locally added Contact to the LDAP Server, if possible, and updates
   * the status of the local contact if successfully added
   * 
   * @param rawcontactId
   * @param batchOperation
   * @param serverInstance
   * @param context
   */
  public static void addLocalContactToLDAP(int rawcontactId,
      BatchOperation batchOperation, ServerInstance serverInstance,
      Context context) {
    Bundle contact = loadContact(rawcontactId, context);
    String dn = contact.getString(AttributeMapper.DN);
    if (dn == null) {
      dn = serverInstance.getBaseDN();
    }
    Entry ldapentry = new Entry(dn);
    contact.remove(AttributeMapper.DN);
    contact.remove(LDAP_SYNC_STATUS_KEY);
    contact.remove(LDAP_ERROR_MESSAGE_KEY);
    contact.remove(LDAP_LDIF_DETAILS_KEY);
    for (String attr : contact.keySet()) {
      ldapentry.addAttribute(attr, contact.getString(attr));
    }
    ldapentry.addAttribute("objectClass", "inetOrgPerson");
    ldapentry.addAttribute("objectClass", "organizationalPerson");
    ldapentry.addAttribute("objectClass", "person");
    ldapentry.addAttribute("objectClass", "top");
    // String ldif = ldapentry.toLDIFString();
    LDAPConnection connection = null;
    try {
      connection = serverInstance.getConnection();
      LDAPResult result = connection.add(ldapentry);
      String errormessage = result.getResultCode().toString();
      if (result.getResultCode().intValue() == ResultCode.SUCCESS_INT_VALUE) {
        SearchRequest request = new SearchRequest(dn, SearchScope.BASE,
            Filter.create("(cn=*)"), SearchRequest.ALL_OPERATIONAL_ATTRIBUTES,
            SearchRequest.ALL_USER_ATTRIBUTES);
        SearchResult searchResults = connection.search(request);
        if (searchResults.getSearchEntries().size() >= 1) {
          SearchResultEntry entry = searchResults.getSearchEntries().get(0);
          String uuid = entry.getAttributeValue(AttributeMapper.ATTR_UID);
          String ldif = entry.toLDIFString();
          String status = SYNC_STATUS_IN_SYNC;
          String dnAndClasses = entry.getDN();
          for (String objectClass : entry.getAttributeValues("objectClass")) {
            dnAndClasses = dnAndClasses + EOL + objectClass;
          }
          Uri contentAsSyncProvider = RawContacts.CONTENT_URI
              .buildUpon()
              .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
                  "true").build();
          ContentProviderOperation update = updateLocallyAddedToSyncStatus(
              rawcontactId, status, contentAsSyncProvider, ldif, uuid,
              dnAndClasses);
          batchOperation.add(update);
          updateLDAPMessage(rawcontactId, "", batchOperation);
        }
      } else if (!ResultCode.isClientSideResultCode(result.getResultCode())) {
        // TODO Notifiy the User about Server Problem
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context.getApplicationContext(),
            errormessage, duration);
        toast.show();
        updateLDAPMessage(rawcontactId, errormessage, batchOperation);
      }
    } catch (LDAPException e) {
      updateLDAPMessage(rawcontactId, e.getExceptionMessage(), batchOperation);
    } finally {
      if (connection != null) {
        connection.close();
      }
    }

  }

  /**
   * Updates the RawContact Field SYNC 2 to the given message
   * 
   * @param rawcontactId
   * @param errormessage
   * @param batchOperation
   */
  private static void updateLDAPMessage(int rawcontactId, String errormessage,
      BatchOperation batchOperation) {
    Uri rawContact = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
        rawcontactId);
    batchOperation.add(ContentProviderOperation.newUpdate(rawContact)
        .withValue(RawContacts.SYNC2, errormessage).build());
  }

  /**
   * Internal Method to save the given Contact Data to the local Android
   * Contacts and marks them with the right sync status, to initialize an
   * asynchronous synchronization, if it is necessary If BatchOperation is null,
   * the instruction is executed directly, otherwise, it adds the operations to
   * the batch
   * 
   * @param b
   * @param context
   * @param account
   * @param onlyImportNotSync
   */
  private static void saveNewLocallyAddedContact(Bundle b,
      final Context context, final Account account, BatchOperation batch,
      boolean onlyImportNotSync) {
    int rawContactInsertIndex = 0;
    boolean executeDirectly = false;
    if (batch == null) {
      batch = new BatchOperation(context, context.getContentResolver());
      executeDirectly = true;
    } else {
      rawContactInsertIndex = batch.size();
    }
    Uri rawContactUri = null;
    Uri dataUri = null;
    if (onlyImportNotSync) {
      b.putString(LDAP_SYNC_STATUS_KEY, SYNC_STATUS_DO_NOT_SYNC);
      rawContactUri = RawContacts.CONTENT_URI.buildUpon()
          .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
          .build();
      dataUri = Data.CONTENT_URI.buildUpon()
          .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
          .build();
    } else {
      b.putString(LDAP_SYNC_STATUS_KEY, SYNC_STATUS_LOCALLY_ADDED);
      rawContactUri = RawContacts.CONTENT_URI;
      dataUri = Data.CONTENT_URI;
    }
    // Prepare contact creation request
    ContactUtils.createFullContact(account, b, batch, rawContactUri, dataUri,
        rawContactInsertIndex);
    // Adding non mapped rows
    ContactUtils.createLDAPRow(AttributeMapper.UID,
        b.getString(AttributeMapper.ATTR_UID), batch, dataUri,
        rawContactInsertIndex);
    if (executeDirectly) {
      // Ask the Contact provider to create a new contact
      try {
        batch.execute();
      } catch (Exception e) {
        // Display warning
        int duration = Toast.LENGTH_SHORT;
        Toast toast = Toast.makeText(context.getApplicationContext(),
            R.string.contactCreationFailure, duration);
        toast.show();
      }
    }
  }

  /**
   * This Function adds all given LDAP Parameter to the local AndroidContacts
   * and tries to sync them with the LDAP asynchronous
   * 
   * @param b
   *          Bundle with all LDAP Entries included as key value store
   * @param account
   *          to which the Entries should be added
   * @param context
   *          in which the Function is called
   */
  public static void saveNewLocallyAddedContactAndSync(final Bundle b,
      final Account account, final Context context) {
    saveNewLocallyAddedContact(b, context, account, null, false);
  }

  /**
   * This Function adds all given LDAP Parameter to the local AndroidContacts
   * and do NOT synchronize them with the LDAP Server
   * 
   * @param b
   *          Bundle with all LDAP Entries included as key value store
   * @param account
   *          to which the Entries should be added
   * @param context
   *          in which the Function is called
   */
  public static void saveNewLocallyAddedContactAndDoNotSync(final Bundle b,
      final Account account, final Context context) {
    saveNewLocallyAddedContact(b, context, account, null, true);
  }

  /**
   * Loads a Contact from local Android Contacts and puts the result into a
   * Bundle with the {@link AttributeMapper} Keys, if the entries are not null
   * 
   * @param rawcontactId
   * @param account
   * @param context
   * @return
   */
  public static Bundle loadContact(final int rawcontactId, final Context context) {
    Bundle contact = new Bundle();
    Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
        rawcontactId);
    Cursor c = context.getContentResolver()
        .query(
            rawContactUri,
            new String[] { RawContacts._ID, RawContacts.SYNC1,
                RawContacts.SYNC2, RawContacts.SYNC3, RawContacts.SYNC4,
                RawContacts.ACCOUNT_NAME, RawContacts.ACCOUNT_TYPE,
                RawContacts.SOURCE_ID }, null, null, null);
    try {
      loadSyncData(contact, c, 1, 2, 3, 4, 5, 6, 7);
    } catch (Exception e) {
      // Display warning
      int duration = Toast.LENGTH_SHORT;
      Toast toast = Toast.makeText(context.getApplicationContext(),
          R.string.contactLoadingFailure, duration);
      toast.show();
    } finally {
      c.close();
    }
    c = context.getContentResolver().query(
        Data.CONTENT_URI,
        new String[] { RawContactsEntity._ID, RawContactsEntity.MIMETYPE,
            RawContactsEntity.DATA1, RawContactsEntity.DATA2,
            RawContactsEntity.DATA3, RawContactsEntity.DATA4,
            RawContactsEntity.DATA5, RawContactsEntity.DATA6,
            RawContactsEntity.DATA7, RawContactsEntity.DATA8,
            RawContactsEntity.DATA9, RawContactsEntity.DATA10,
            RawContactsEntity.DATA11, RawContactsEntity.DATA12,
            RawContactsEntity.DATA13, RawContactsEntity.DATA14,
            RawContactsEntity.DATA15 },
        Data.RAW_CONTACT_ID + "=" + rawcontactId, null, null);
    try {
      while (c.moveToNext()) {
        String mimetype = c.getString(1);
        // is a name
        if (mimetype.equalsIgnoreCase(StructuredName.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadName(contact, c, 2, 3, 4, 5);
        } else if (mimetype
            .equalsIgnoreCase(StructuredPostal.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadAddress(contact, c, 3, 2, 5, 6, 9, 10);
        } else if (mimetype.equalsIgnoreCase(Photo.CONTENT_ITEM_TYPE)) {
          // TODO Photo import
        } else if (mimetype.equalsIgnoreCase(Note.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadDescription(contact, c, 2);
        } else if (mimetype.equalsIgnoreCase(Phone.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadPhoneNumber(contact, c, 3, 2);
        } else if (mimetype.equalsIgnoreCase(Organization.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadOrganization(contact, c, 2, 6);
        } else if (mimetype.equalsIgnoreCase(Nickname.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadInitials(contact, c, 3, 2);
        } else if (mimetype.equalsIgnoreCase(Email.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadMail(contact, c, 3, 2);
        } else if (mimetype.equalsIgnoreCase(LDAPRow.CONTENT_ITEM_TYPE)) {
          ContactUtils.loadLDAPRow(contact, c, 2, 3);
        }
      }
    } finally {
      c.close();
    }
    return contact;
  }

  private static void loadSyncData(Bundle contact, Cursor c, int sync1,
      int sync2, int sync3, int sync4, int accountname, int accounttype,
      int sourceid) {
    if (!c.isNull(sync1)) {
      String status = c.getString(sync1);
      if (status.length() > 0) {
        if (!status.equals(SYNC_STATUS_DO_NOT_SYNC)) {
          contact.putString(LDAP_SYNC_STATUS_KEY, status);
        }
      }
    }
    if (!c.isNull(sync2)) {
      String errormessage = c.getString(sync2);
      if (errormessage.length() > 0) {
        contact.putString(LDAP_ERROR_MESSAGE_KEY, errormessage);
      }
    }
    if (!c.isNull(sync3)) {
      String dn = c.getString(sync3);
      int endline = dn.indexOf(EOL);
      if (endline > 0) {
        dn = dn.substring(0, endline);
      }
      contact.putString(AttributeMapper.DN, dn);
    }
    if (!c.isNull(sync4)) {
      String ldif = c.getString(sync4);
      if (ldif.length() > 0) {
        contact.putString(LDAP_LDIF_DETAILS_KEY, ldif);
      }
    }
    if (!c.isNull(accountname)) {
      contact.putString(LOCAL_ACCOUNT_NAME_KEY, c.getString(accountname));
    } else {
      contact.putString(LOCAL_ACCOUNT_NAME_KEY, "");
    }
    if (!c.isNull(accounttype)) {
      contact.putString(LOCAL_ACCOUNT_TYPE_KEY, c.getString(accounttype));
    } else {
      contact.putString(LOCAL_ACCOUNT_TYPE_KEY, "");
    }
    if (!c.isNull(sourceid)) {
      contact.putString(LDAP_SOURCE_ID_KEY, c.getString(sourceid));
    } else {
      contact.putString(LDAP_SOURCE_ID_KEY, "");
    }
  }
}