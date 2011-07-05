package de.tubs.ibr.android.ldap.core;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.Set;
import java.util.StringTokenizer;
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
import android.provider.ContactsContract.CommonDataKinds.Im;
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
import android.test.IsolatedContext;
import android.widget.Toast;
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

  private static final String SYNC_STATUS_LOCALLY_ADDED = "locally added";

  private static final String SYNC_STATUS_DO_NOT_SYNC = "";

  private static final String SYNC_STATUS_IN_SYNC = "inSync";

  private static final String SYNC_STATUS_CONFLICT = "conflict";

  public static final String LDAP_SYNC_STATUS_KEY = "LDAP_SYNC_STATUS";

  public static final String LDAP_ERROR_MESSAGE_KEY = "LDAP_ERROR_MESSAGE";

  public static final String LDAP_LDIF_DETAILS_KEY = "LDAP_LDIF_DETAILS";

  public static void addLDAPContactToAccount(Entry entry, Account account,
      BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
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
    String name = entry.getAttributeValue(AttributeMapper.FULL_NAME);
    String workPhone = entry.getAttributeValue(AttributeMapper.PRIMARY_PHONE);
    String homePhone = entry.getAttributeValue(AttributeMapper.HOME_PHONE);
    String mobile = entry.getAttributeValue(AttributeMapper.MOBILE_PHONE);
    String pager = entry.getAttributeValue(AttributeMapper.PAGER);
    String fax = entry.getAttributeValue(AttributeMapper.FAX);
    String workEMail = entry.getAttributeValue(AttributeMapper.PRIMARY_MAIL);
    @SuppressWarnings("deprecation")
    String homeEMail = entry.getAttributeValue(AttributeMapper.ALTERNATE_MAIL);
    String workAddress = entry
        .getAttributeValue(AttributeMapper.PRIMARY_ADDRESS);
    String homeAddress = entry.getAttributeValue(AttributeMapper.HOME_ADDRESS);
    String firstname = entry.getAttributeValue(AttributeMapper.FIRST_NAME);
    String lastname = entry.getAttributeValue(AttributeMapper.LAST_NAME);
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
    batch.add(ContentProviderOperation.newInsert(contentAsSyncAdapter)
        .withValue(RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(RawContacts.ACCOUNT_NAME, account.name)
        .withValue(RawContacts.SOURCE_ID, sourceId)
        .withValue(RawContacts.SYNC1, SYNC_STATUS_IN_SYNC)
        .withValue(RawContacts.SYNC2, "")
        .withValue(RawContacts.SYNC3, entry.getDN() + objectClass)
        .withValue(RawContacts.SYNC4, entry.toLDIFString()).build());
    batch.add(ContentProviderOperation.newInsert(dataAsSyncAdapter)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, name)
        .withValue(StructuredName.GIVEN_NAME, firstname)
        .withValue(StructuredName.FAMILY_NAME, lastname).build());
    if (workPhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, workPhone,
          Phone.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homePhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, homePhone,
          Phone.TYPE_HOME, dataAsSyncAdapter));
    }
    if (mobile != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, mobile,
          Phone.TYPE_MOBILE, dataAsSyncAdapter));
    }
    if (pager != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, pager, Phone.TYPE_PAGER,
          dataAsSyncAdapter));
    }
    if (fax != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, fax, Phone.TYPE_FAX_WORK,
          dataAsSyncAdapter));
    }
    if (workEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, workEMail,
          Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, homeEMail,
          Email.TYPE_HOME, dataAsSyncAdapter));
    }
    if (workAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, workAddress,
          Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, homeAddress,
          StructuredPostal.TYPE_HOME, dataAsSyncAdapter));
    }
  }

  /**
   * @param entry
   * @param account
   * @param batch
   */
  public static void importLDAPContact(Entry entry, Account account,
      BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
    // Check if the Entry is a Person and can be synchronized
    String name = entry.getAttributeValue(AttributeMapper.FULL_NAME);
    String workPhone = entry.getAttributeValue(AttributeMapper.PRIMARY_PHONE);
    String homePhone = entry.getAttributeValue(AttributeMapper.HOME_PHONE);
    String mobile = entry.getAttributeValue(AttributeMapper.MOBILE_PHONE);
    String pager = entry.getAttributeValue(AttributeMapper.PAGER);
    String fax = entry.getAttributeValue(AttributeMapper.FAX);
    String workEMail = entry.getAttributeValue(AttributeMapper.PRIMARY_MAIL);
    @SuppressWarnings("deprecation")
    String homeEMail = entry.getAttributeValue(AttributeMapper.ALTERNATE_MAIL);
    String workAddress = entry
        .getAttributeValue(AttributeMapper.PRIMARY_ADDRESS);
    String homeAddress = entry.getAttributeValue(AttributeMapper.HOME_ADDRESS);
    String firstname = entry.getAttributeValue(AttributeMapper.FIRST_NAME);
    String lastname = entry.getAttributeValue(AttributeMapper.LAST_NAME);
    // List all LDAP ObjectClasses
    Uri contentAsSyncAdapter = RawContacts.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    Uri dataAsSyncAdapter = Data.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    batch.add(ContentProviderOperation.newInsert(contentAsSyncAdapter)
        .withValue(RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(RawContacts.ACCOUNT_NAME, account.name).build());
    batch.add(ContentProviderOperation.newInsert(dataAsSyncAdapter)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, name)
        .withValue(StructuredName.GIVEN_NAME, firstname)
        .withValue(StructuredName.FAMILY_NAME, lastname).build());
    if (workPhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, workPhone,
          Phone.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homePhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, homePhone,
          Phone.TYPE_HOME, dataAsSyncAdapter));
    }
    if (mobile != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, mobile,
          Phone.TYPE_MOBILE, dataAsSyncAdapter));
    }
    if (pager != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, pager, Phone.TYPE_PAGER,
          dataAsSyncAdapter));
    }
    if (fax != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, fax, Phone.TYPE_FAX_WORK,
          dataAsSyncAdapter));
    }
    if (workEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, workEMail,
          Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, homeEMail,
          Email.TYPE_HOME, dataAsSyncAdapter));
    }
    if (workAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, workAddress,
          Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, homeAddress,
          StructuredPostal.TYPE_HOME, dataAsSyncAdapter));
    }
  }

  public static void exportContactToLDAP(long id, BatchOperation batch) {

  }

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
      String dn = getDNofRawContact(id, context);
      if (dn == null || dn.length() == 0) {
        remotelyDeleted = true;
      } else {
        DeleteRequest deleteRequest = new DeleteRequest(dn);
        LDAPResult deleteResult = conn.delete(deleteRequest);
        if (deleteResult.getMessageID() == ResultCode.SUCCESS_INT_VALUE
            || deleteResult.getMessageID() == ResultCode.NO_SUCH_OBJECT_INT_VALUE) {
          remotelyDeleted = true;
        } else {
          // Write error Message and result Code to local RawContact!
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

    // String name = entry.getAttributeValue(AttributeMapper.ATTR_FULL_NAME);
    // String workPhone = entry
    // .getAttributeValue(AttributeMapper.ATTR_PRIMARY_PHONE);
    // String homePhone =
    // entry.getAttributeValue(AttributeMapper.ATTR_HOME_PHONE);
    // String mobile =
    // entry.getAttributeValue(AttributeMapper.ATTR_MOBILE_PHONE);
    // String pager = entry.getAttributeValue(AttributeMapper.ATTR_PAGER);
    // String fax = entry.getAttributeValue(AttributeMapper.ATTR_FAX);
    // String workEMail = entry
    // .getAttributeValue(AttributeMapper.ATTR_PRIMARY_MAIL);
    // String homeEMail = entry
    // .getAttributeValue(AttributeMapper.ATTR_ALTERNATE_MAIL);
    // String workAddress = entry
    // .getAttributeValue(AttributeMapper.ATTR_PRIMARY_ADDRESS);
    // String homeAddress = entry
    // .getAttributeValue(AttributeMapper.ATTR_HOME_ADDRESS);
    // String sourceId = entry.getAttributeValue(AttributeMapper.ATTR_UID);
    // batch.add(ContentProviderOperation
    // .newUpdate(RawContacts.CONTENT_URI)
    // .withValue(RawContacts.ACCOUNT_TYPE, account.type)
    // .withValue(RawContacts.ACCOUNT_NAME, account.name)
    // .withValue(RawContacts.SOURCE_ID, sourceId)
    // .withValue(RawContacts.CONTACT_ID, id).build());
    // batch.add(ContentProviderOperation
    // .newUpdate(Data.CONTENT_URI)
    // .withValueBackReference(Data.RAW_CONTACT_ID, id)
    // .withValue(Data.MIMETYPE,
    // ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
    // .withValue(
    // ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
    // .build());
    // if (workPhone != null) {
    // batch.add(addPhoneNumber(workPhone,
    // ContactsContract.CommonDataKinds.Phone.TYPE_WORK));
    // }
    // if (homePhone != null) {
    // batch.add(addPhoneNumber(homePhone,
    // ContactsContract.CommonDataKinds.Phone.TYPE_HOME));
    // }
    // if (mobile != null) {
    // batch.add(addPhoneNumber(mobile,
    // ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE));
    // }
    // if (pager != null) {
    // batch.add(addPhoneNumber(pager,
    // ContactsContract.CommonDataKinds.Phone.TYPE_PAGER));
    // }
    // if (fax != null) {
    // batch.add(addPhoneNumber(fax,
    // ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK));
    // }
    // if (workEMail != null) {
    // batch.add(addEMailAddress(workEMail,
    // ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    // }
    // if (homeEMail != null) {
    // batch.add(addEMailAddress(homeEMail,
    // ContactsContract.CommonDataKinds.Email.TYPE_HOME));
    // }
    // if (workAddress != null) {
    // batch.add(addPostalAddress(workAddress,
    // ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    // }
    // if (homeAddress != null) {
    // batch.add(addPostalAddress(homeAddress,
    // ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME));
    // }
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
  private static ContentProviderOperation addPhoneNumber(
      final int rawContactInsertIndex, final String number, final int type,
      final Uri dataAsSyncAdapter) {
    return ContentProviderOperation.newInsert(dataAsSyncAdapter)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        .withValue(Phone.NUMBER, number).withValue(Phone.TYPE, type).build();
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
  private static ContentProviderOperation updatePhoneNumber(final int id,
      final String number, final int type) {
    return ContentProviderOperation.newUpdate(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, id)
        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        .withValue(Phone.NUMBER, number).withValue(Phone.TYPE, type).build();
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
   * Adds the provided e-mail address to the contact.
   * 
   * @param address
   *          The e-mail address to add.
   * @param type
   *          The type of address to add.
   * @param uri
   *          The base URI for the contact.
   * @return a addEMailAdress Operation
   */
  private static ContentProviderOperation addEMailAddress(
      final int rawContactInsertIndex, final String address, final int type,
      final Uri dataAsSyncAdapter) {
    return ContentProviderOperation.newInsert(dataAsSyncAdapter)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
        .withValue(Email.DATA, address).withValue(Email.TYPE, type).build();
  }

  /**
   * Adds the provided e-mail address to the contact.
   * 
   * @param address
   *          The e-mail address to add.
   * @param type
   *          The type of address to add.
   * @param uri
   *          The base URI for the contact.
   * @return a addEMailAdress Operation
   */
  private static ContentProviderOperation updateEMailAddress(final int id,
      final String address, final int type) {
    return ContentProviderOperation.newUpdate(Data.CONTENT_URI)
        .withValueBackReference(Data.RAW_CONTACT_ID, id)
        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
        .withValue(Email.DATA, address).withValue(Email.TYPE, type).build();
  }

  /**
   * Adds the provided postal address to the contact.
   * 
   * @param address
   *          The postal address to add.
   * @param type
   *          The type of address to add.
   * @return a addPostalAdress Operation
   */
  private static ContentProviderOperation addPostalAddress(
      final int rawContactInsertIndex, final String address, final int type,
      final Uri dataAsSyncAdapter) {
    final StringBuilder addr = new StringBuilder();
    final StringTokenizer tokenizer = new StringTokenizer(address, "$");
    while (tokenizer.hasMoreTokens()) {
      addr.append(tokenizer.nextToken().trim());
      if (tokenizer.hasMoreTokens()) {
        addr.append(EOL);
      }
    }
    return ContentProviderOperation.newInsert(dataAsSyncAdapter)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
        .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
        .withValue(StructuredPostal.TYPE, type)
        .withValue(StructuredPostal.FORMATTED_ADDRESS, addr.toString()).build();
  }

  /**
   * Returns the LDAP DN of the given {@link RawContacts} id
   * 
   * @param id
   * @param context
   * @return
   */
  private static String getDNofRawContact(final int id, Context context) {
    String dn = null;
    Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI, id);
    Cursor c = context.getContentResolver().query(rawContactUri,
        new String[] { RawContacts._ID, RawContacts.SYNC3 }, null, null, null);
    try {
      while (c.moveToNext()) {
        int Id = c.getInt(0);
        if (Id == id) {
          if (!c.isNull(1)) {
            String temp = c.getString(1);
            int endline = temp.indexOf(EOL);
            if (endline > 0) {
              dn = temp.substring(0, endline);
            } else {
              dn = temp;
            }
          }
          break;
        }
      }
    } finally {
      c.close();
    }
    return dn;
  }

  private static String buildLDIFFromContact(int id, Context context) {
    String dn = getDNofRawContact(id, context);

    return "";
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
   * asynchronous synchronization, if it is necessary
   * 
   * @param b
   * @param context
   * @param account
   * @param onlyImportNotSync
   */
  private static void saveNewLocallyAddedContact(final Bundle b,
      final Context context, final Account account, boolean onlyImportNotSync) {
    // Name
    String sn = null;
    String cn = null;
    String initials = null;
    String title = null;
    String displayName = null;
    String givenName = null;
    // Description
    String description = null;
    // Numbers
    String telephoneNumber = null;
    String homePhone = null;
    String mobile = null;
    String facsimileTelephoneNumber = null;
    String pager = null;
    String telexNumber = null;
    String internationalISDNNumber = null;
    // Mail
    String mail = null;
    // Addresses
    String destinationIndicator = null;
    String registeredAddress = null;
    String street = null;
    String preferredDeliveryMethod = null;
    String postOfficeBox = null;
    String postalCode = null;
    String postalAddress = null;
    String homePostalAddress = null;
    String st = null;
    // Website
    String seeAlso = null;
    // Organization
    String o = null;
    String ou = null;
    String l = null;
    String preferredLanguage = null;
    String departmentNumber = null;
    String physicalDeliveryOfficeName = null;
    String roomNumber = null;
    String businessCategory = null;
    // Others
    String uid = null;

    String dn = null;
    String syncstatus = null;
    BatchOperation batch = new BatchOperation(context,
        context.getContentResolver());
    Set<String> keys = b.keySet();
    for (String key : keys) {
      if (key.equals(AttributeMapper.FIRST_NAME)) {
        givenName = b.getString(key);
      } else if (key.equals(AttributeMapper.LAST_NAME)) {
        sn = b.getString(key);
      } else if (key.equals(AttributeMapper.DN)) {
        dn = b.getString(key);
      } else if (key.equals(AttributeMapper.UID)) {
        uid = b.getString(key);
      } else if (key.equals(AttributeMapper.FAX)) {
        facsimileTelephoneNumber = b.getString(key);
      } else if (key.equals(AttributeMapper.DESCRIPTION)) {
        description = b.getString(key);
      } else if (key.equals(AttributeMapper.TITLE)) {
        title = b.getString(key);
      } else if (key.equals(AttributeMapper.REGISTERED_ADDRESS)) {
        registeredAddress = b.getString(key);
      } else if (key.equals(AttributeMapper.DESTINATION_INDICATOR)) {
        destinationIndicator = b.getString(key);
      } else if (key.equals(AttributeMapper.PREFERRED_DELIVERY_METHOD)) {
        preferredDeliveryMethod = b.getString(key);
      } else if (key.equals(AttributeMapper.PRIMARY_PHONE)) {
        telephoneNumber = b.getString(key);
      } else if (key.equals(AttributeMapper.INTERNATIONAL_ISDN_NUMBER)) {
        internationalISDNNumber = b.getString(key);
      } else if (key.equals(AttributeMapper.STREET)) {
        street = b.getString(key);
      } else if (key.equals(AttributeMapper.POST_OFFICE_BOX)) {
        postOfficeBox = b.getString(key);
      } else if (key.equals(AttributeMapper.POSTAL_CODE)) {
        postalCode = b.getString(key);
      } else if (key.equals(AttributeMapper.POSTAL_ADDRESS)) {
        postalAddress = b.getString(key);
      } else if (key.equals(AttributeMapper.PHYSICAL_DELIVERY_OFFICE_NAME)) {
        physicalDeliveryOfficeName = b.getString(key);
      } else if (key.equals(AttributeMapper.ORGANIZATION_UNIT)) {
        ou = b.getString(key);
      } else if (key.equals(AttributeMapper.STATE)) {
        st = b.getString(key);
      } else if (key.equals(AttributeMapper.LOCALITY)) {
        l = b.getString(key);
      } else if (key.equals(AttributeMapper.BUSINESS_CATEGORY)) {
        businessCategory = b.getString(key);
      } else if (key.equals(AttributeMapper.DEPARTMENT_NUMBER)) {
        departmentNumber = b.getString(key);
      } else if (key.equals(AttributeMapper.FULL_NAME)
          || key.equals(AttributeMapper.DISPLAYNAME)) {
        displayName = b.getString(key);
        cn = b.getString(key);
      } else if (key.equals(AttributeMapper.HOME_PHONE)) {
        homePhone = b.getString(key);
      } else if (key.equals(AttributeMapper.HOME_ADDRESS)) {
        homePostalAddress = b.getString(key);
      } else if (key.equals(AttributeMapper.INITIALS)) {
        initials = b.getString(key);
      } else if (key.equals(AttributeMapper.PRIMARY_MAIL)) {
        mail = b.getString(key);
      } else if (key.equals(AttributeMapper.MOBILE_PHONE)) {
        mobile = b.getString(key);
      } else if (key.equals(AttributeMapper.ORGANIZATION)) {
        o = b.getString(key);
      } else if (key.equals(AttributeMapper.PAGER)) {
        pager = b.getString(key);
      } else if (key.equals(AttributeMapper.ROOM_NUMBER)) {
        roomNumber = b.getString(key);
      } else if (key.equals(AttributeMapper.PREFERRED_LANGUAGE)) {
        preferredLanguage = b.getString(key);
      } else if (key.equals(AttributeMapper.TELEX)) {
        telexNumber = b.getString(key);
      } else if (key.equals(AttributeMapper.SEE_ALSO)) {
        seeAlso = b.getString(key);
      }
    }
    Uri rawContactUri = null;
    Uri dataUri = null;
    if (onlyImportNotSync) {
      uid = null;
      syncstatus = SYNC_STATUS_DO_NOT_SYNC;
      rawContactUri = RawContacts.CONTENT_URI.buildUpon()
          .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
          .build();
      dataUri = Data.CONTENT_URI.buildUpon()
          .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
          .build();
    } else {
      syncstatus = SYNC_STATUS_LOCALLY_ADDED;
      rawContactUri = RawContacts.CONTENT_URI;
      dataUri = Data.CONTENT_URI;
    }
    // Prepare contact creation request
    ContactUtils
        .createRawContact(account, dn, syncstatus, batch, rawContactUri);
    // Adding all Name values
    ContactUtils.createStructuredName(sn, cn, initials, title, givenName,
        batch, dataUri);
    // Adding Description
    ContactUtils.createDescription(description, batch, dataUri);
    // Adding Numbers
    ContactUtils.createPhoneNumbers(telephoneNumber, homePhone, mobile,
        facsimileTelephoneNumber, pager, telexNumber, internationalISDNNumber,
        batch, dataUri);
    // Adding Mail
    ContactUtils.createMail(mail, batch, dataUri);
    // Adding Addresses
    ContactUtils.createAddresses(destinationIndicator, registeredAddress,
        street, preferredDeliveryMethod, postOfficeBox, postalCode,
        postalAddress, homePostalAddress, st, batch, dataUri);
    // Adding SeeAlso as website
    ContactUtils.createSeeAlso(seeAlso, batch, dataUri);
    // Adding Organization
    ContactUtils.createOrganization(o, ou, departmentNumber, l, roomNumber,
        preferredLanguage, physicalDeliveryOfficeName, businessCategory, batch,
        dataUri);
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
    saveNewLocallyAddedContact(b, context, account, false);
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
    saveNewLocallyAddedContact(b, context, account, true);
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
    String registeredAddress = null;
    String destinationIndicator = null;
    String preferredDeliveryMethod = null;
    String telephoneNumber = null;
    String internationalISDNNumber = null;
    String facsimileTelephoneNumber = null;
    String street = null;
    String postOfficeBox = null;
    String postalAddress = null;
    String physicalDeliveryOfficeName = null;
    String st = null;
    String l = null;
    String businessCategory = null;
    String departmentNumber = null;
    String homePhone = null;
    String homePostalAddress = null;
    String initials = null;
    String mobile = null;
    String pager = null;
    String roomNumber = null;
    String uid = null;
    String preferredLanguage = null;
    Uri rawContactUri = ContentUris.withAppendedId(RawContacts.CONTENT_URI,
        rawcontactId);
    Cursor c = context.getContentResolver().query(
        rawContactUri,
        new String[] { RawContacts._ID, RawContacts.SYNC1, RawContacts.SYNC2,
            RawContacts.SYNC3, RawContacts.SYNC4 }, null, null, null);
    try {
      if (!c.isNull(1)) {
        String status = c.getString(1);
        if (status.length() > 0) {
          if (!status.equals(SYNC_STATUS_DO_NOT_SYNC)) {
            contact.putString(LDAP_SYNC_STATUS_KEY, status);
          }
        }
      }
      if (!c.isNull(2)) {
        String errormessage = c.getString(2);
        if (errormessage.length() > 0) {
          contact.putString(LDAP_ERROR_MESSAGE_KEY, errormessage);
        }
      }
      if (!c.isNull(3)) {
        String dn = c.getString(3);
        int endline = dn.indexOf(EOL);
        if (endline > 0) {
          dn = dn.substring(0, endline);
        }
        contact.putString(AttributeMapper.DN, dn);
      }
      if (!c.isNull(4)) {
        String ldif = c.getString(4);
        if (ldif.length() > 0) {
          contact.putString(LDAP_LDIF_DETAILS_KEY, ldif);
        }
      }
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
          contact.putString(AttributeMapper.FULL_NAME, c.getString(2));
          contact.putString(AttributeMapper.DISPLAYNAME, c.getString(2));
          contact.putString(AttributeMapper.FIRST_NAME, c.getString(3));
          contact.putString(AttributeMapper.LAST_NAME, c.getString(4));
          contact.putString(AttributeMapper.TITLE, c.getString(5));
        } else if (mimetype.equalsIgnoreCase(Im.CONTENT_ITEM_TYPE)) {
          // TODO Instant Messengers
        } else if (mimetype
            .equalsIgnoreCase(StructuredPostal.CONTENT_ITEM_TYPE)) {
          int type = c.getInt(3);
          switch (type) {
            case StructuredPostal.TYPE_WORK:
              contact
                  .putString(AttributeMapper.PRIMARY_ADDRESS, c.getString(2));
              contact
                  .putString(AttributeMapper.POST_OFFICE_BOX, c.getString(6));
              contact.putString(AttributeMapper.POSTAL_CODE, c.getString(10));
              break;
            case StructuredPostal.TYPE_HOME:
              contact.putString(AttributeMapper.HOME_ADDRESS, c.getString(2));
              contact.putString(AttributeMapper.STREET, c.getString(5));
              contact.putString(AttributeMapper.STATE, c.getString(9));
              break;
          }
        } else if (mimetype.equalsIgnoreCase(Photo.CONTENT_ITEM_TYPE)) {
          // TODO Photo import
        } else if (mimetype.equalsIgnoreCase(Note.CONTENT_ITEM_TYPE)) {
          contact.putString(AttributeMapper.DESCRIPTION, c.getString(2));
        } else if (mimetype.equalsIgnoreCase(Phone.CONTENT_ITEM_TYPE)) {
          int type = c.getInt(3);
          switch (type) {
            case Phone.TYPE_HOME:
              contact.putString(AttributeMapper.HOME_PHONE, c.getString(2));
              break;
            case Phone.TYPE_WORK:
              contact.putString(AttributeMapper.PRIMARY_PHONE, c.getString(2));
              break;
            case Phone.TYPE_FAX_WORK:
              contact.putString(AttributeMapper.FAX, c.getString(2));
              break;
            case Phone.TYPE_MOBILE:
              contact.putString(AttributeMapper.MOBILE_PHONE, c.getString(2));
              break;
            case Phone.TYPE_PAGER:
              contact.putString(AttributeMapper.PAGER, c.getString(2));
              break;
            case Phone.TYPE_TELEX:
              contact.putString(AttributeMapper.TELEX, c.getString(2));
              break;
            case Phone.TYPE_ISDN:
              contact.putString(AttributeMapper.ISDN, c.getString(2));
              break;
          }
        } else if (mimetype.equalsIgnoreCase(Organization.CONTENT_ITEM_TYPE)) {
          contact.putString(AttributeMapper.ORGANIZATION, c.getString(2));
          contact.putString(AttributeMapper.ORGANIZATION_UNIT, c.getString(6));
        } else if (mimetype.equalsIgnoreCase(Nickname.CONTENT_ITEM_TYPE)) {
          int type = c.getInt(3);
          switch (type) {
            case Nickname.TYPE_INITIALS:
              contact.putString(AttributeMapper.INITIALS, c.getString(2));
              break;
          }
        } else if (mimetype.equalsIgnoreCase(Email.CONTENT_ITEM_TYPE)) {
          int type = c.getInt(3);
          switch (type) {
            case Email.TYPE_WORK:
              contact.putString(AttributeMapper.PRIMARY_MAIL, c.getString(2));
              break;
            default:
              contact.putString(AttributeMapper.ALTERNATE_MAIL, c.getString(2));
              break;
          }
        }
      }
    } finally {
      c.close();
    }
    return contact;
  }
}