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
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
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

  public static void addLDAPContactToAccount(Entry entry, Account account,
      BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
    // Check if the Entry is a Person and can be synchronized
    boolean isSyncable = false;
    String sourceId = entry.getAttributeValue(AttributeMapper.ATTR_UID);
    String ldif = entry.toLDIFString();
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
    Uri contentAsSyncAdapter = ContactsContract.RawContacts.CONTENT_URI
        .buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    Uri dataAsSyncAdapter = ContactsContract.Data.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    batch.add(ContentProviderOperation
        .newInsert(contentAsSyncAdapter)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .withValue(ContactsContract.RawContacts.SOURCE_ID, sourceId)
        .withValue(ContactsContract.RawContacts.SYNC1, SYNC_STATUS_IN_SYNC)
        .withValue(ContactsContract.RawContacts.SYNC2, "")
        .withValue(ContactsContract.RawContacts.SYNC3,
            entry.getDN() + objectClass)
        .withValue(ContactsContract.RawContacts.SYNC4, entry.toLDIFString())
        .build());
    batch.add(ContentProviderOperation
        .newInsert(dataAsSyncAdapter)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
            rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            firstname)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            lastname).build());
    if (workPhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, workPhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homePhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, homePhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME, dataAsSyncAdapter));
    }
    if (mobile != null) {
      batch
          .add(addPhoneNumber(rawContactInsertIndex, mobile,
              ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
              dataAsSyncAdapter));
    }
    if (pager != null) {
      batch
          .add(addPhoneNumber(rawContactInsertIndex, pager,
              ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
              dataAsSyncAdapter));
    }
    if (fax != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, fax,
          ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
          dataAsSyncAdapter));
    }
    if (workEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, workEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, homeEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_HOME, dataAsSyncAdapter));
    }
    if (workAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, workAddress,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, homeAddress,
          ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
          dataAsSyncAdapter));
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
    String homeEMail = entry.getAttributeValue(AttributeMapper.ALTERNATE_MAIL);
    String workAddress = entry
        .getAttributeValue(AttributeMapper.PRIMARY_ADDRESS);
    String homeAddress = entry.getAttributeValue(AttributeMapper.HOME_ADDRESS);
    String firstname = entry.getAttributeValue(AttributeMapper.FIRST_NAME);
    String lastname = entry.getAttributeValue(AttributeMapper.LAST_NAME);
    // List all LDAP ObjectClasses
    Uri contentAsSyncAdapter = ContactsContract.RawContacts.CONTENT_URI
        .buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    Uri dataAsSyncAdapter = ContactsContract.Data.CONTENT_URI.buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    batch.add(ContentProviderOperation.newInsert(contentAsSyncAdapter)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .build());
    batch.add(ContentProviderOperation
        .newInsert(dataAsSyncAdapter)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
            rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            firstname)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            lastname).build());
    if (workPhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, workPhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homePhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, homePhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME, dataAsSyncAdapter));
    }
    if (mobile != null) {
      batch
          .add(addPhoneNumber(rawContactInsertIndex, mobile,
              ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
              dataAsSyncAdapter));
    }
    if (pager != null) {
      batch
          .add(addPhoneNumber(rawContactInsertIndex, pager,
              ContactsContract.CommonDataKinds.Phone.TYPE_PAGER,
              dataAsSyncAdapter));
    }
    if (fax != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, fax,
          ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK,
          dataAsSyncAdapter));
    }
    if (workEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, workEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, homeEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_HOME, dataAsSyncAdapter));
    }
    if (workAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, workAddress,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK, dataAsSyncAdapter));
    }
    if (homeAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, homeAddress,
          ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME,
          dataAsSyncAdapter));
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

    Uri contentAsSyncAdapter = ContactsContract.RawContacts.CONTENT_URI
        .buildUpon()
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
    // .newUpdate(ContactsContract.RawContacts.CONTENT_URI)
    // .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
    // .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
    // .withValue(ContactsContract.RawContacts.SOURCE_ID, sourceId)
    // .withValue(ContactsContract.RawContacts.CONTACT_ID, id).build());
    // batch.add(ContentProviderOperation
    // .newUpdate(ContactsContract.Data.CONTENT_URI)
    // .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, id)
    // .withValue(ContactsContract.Data.MIMETYPE,
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
    return ContentProviderOperation
        .newInsert(dataAsSyncAdapter)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
            rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type).build();
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
    return ContentProviderOperation
        .newUpdate(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, id)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, number)
        .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, type).build();
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
        .withValue(ContactsContract.RawContacts.SYNC1, status)
        .withValue(ContactsContract.RawContacts.SYNC3, dn)
        .withValue(ContactsContract.RawContacts.SYNC4, ldif)
        .withValue(ContactsContract.RawContacts.SOURCE_ID, uuid)
        .withValue(ContactsContract.RawContacts.DIRTY, "0").build();
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
    return ContentProviderOperation
        .newInsert(dataAsSyncAdapter)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
            rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Email.DATA, address)
        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, type).build();
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
    return ContentProviderOperation
        .newUpdate(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, id)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.Email.DATA, address)
        .withValue(ContactsContract.CommonDataKinds.Email.TYPE, type).build();
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
    return ContentProviderOperation
        .newInsert(dataAsSyncAdapter)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
            rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, type)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
            addr.toString()).build();
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
    String dn = getDNofRawContact(rawcontactId, context);
    if (dn == null) {
      dn = serverInstance.getBaseDN();
    }
    Entry ldapentry = new Entry(dn);
    Cursor c = context.getContentResolver().query(
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
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/name")) {
          ldapentry.addAttribute(AttributeMapper.FULL_NAME, c.getString(2));
          ldapentry.addAttribute("displayName", c.getString(2));
          ldapentry.addAttribute(AttributeMapper.FIRST_NAME, c.getString(3));
          ldapentry.addAttribute(AttributeMapper.LAST_NAME, c.getString(4));
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/im")) {
          // TODO Instant Messengers
        }
        if (mimetype
            .equalsIgnoreCase("vnd.android.cursor.item/postal-address_v2")) {
          int type = c.getInt(2);
          switch (type) {
            case StructuredPostal.TYPE_WORK:
              ldapentry.addAttribute(AttributeMapper.PRIMARY_ADDRESS,
                  c.getString(4));
              ldapentry.addAttribute("postOfficeBox", c.getString(5));
              ldapentry.addAttribute("postalCode", c.getString(9));
              break;
            case StructuredPostal.TYPE_HOME:
              ldapentry.addAttribute(AttributeMapper.HOME_ADDRESS,
                  c.getString(4));
            default:
              break;
          }
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/photo")) {
          // TODO Photo import
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/phone_v2")) {
          int type = c.getInt(3);
          switch (type) {
            case Phone.TYPE_HOME:
              ldapentry
                  .addAttribute(AttributeMapper.HOME_PHONE, c.getString(2));
              break;
            case Phone.TYPE_WORK:
              ldapentry.addAttribute(AttributeMapper.PRIMARY_PHONE,
                  c.getString(2));
              break;
            case Phone.TYPE_FAX_WORK:
              ldapentry.addAttribute(AttributeMapper.FAX, c.getString(1));
            case Phone.TYPE_MOBILE:
              ldapentry.addAttribute(AttributeMapper.MOBILE_PHONE,
                  c.getString(2));
            case Phone.TYPE_PAGER:
              ldapentry.addAttribute(AttributeMapper.PAGER, c.getString(2));
            default:
              break;
          }
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/organization")) {
          // TODO Organisation
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/nickname")) {
          // TODO Nickname
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/email_v2")) {
          int type = c.getInt(3);
          switch (type) {
            case Email.TYPE_WORK:
              ldapentry.addAttribute(AttributeMapper.PRIMARY_MAIL,
                  c.getString(2));
              break;
            case Email.TYPE_HOME:
              ldapentry.addAttribute(AttributeMapper.ALTERNATE_MAIL,
                  c.getString(2));
            default:
              break;
          }
        }
      }
    } finally {
      c.close();
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
        int resultnumber = searchResults.getEntryCount();
        if (searchResults.getSearchEntries().size() >= 1) {
          SearchResultEntry entry = searchResults.getSearchEntries().get(0);
          String uuid = entry.getAttributeValue(AttributeMapper.ATTR_UID);
          String ldif = entry.toLDIFString();
          String status = SYNC_STATUS_IN_SYNC;
          String dnAndClasses = entry.getDN();
          for (String objectClass : entry.getAttributeValues("objectClass")) {
            dnAndClasses = dnAndClasses + EOL + objectClass;
          }
          Uri contentAsSyncProvider = ContactsContract.RawContacts.CONTENT_URI
              .buildUpon()
              .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER,
                  "true").build();
          ContentProviderOperation update = updateLocallyAddedToSyncStatus(
              rawcontactId, status, contentAsSyncProvider, ldif, uuid,
              dnAndClasses);
          batchOperation.add(update);
        }
      } else if (!ResultCode.isClientSideResultCode(result.getResultCode())) {
        // TODO Notifiy the User about Server Problem

      }
    } catch (LDAPException e) {
      String error = e.getExceptionMessage();
    } finally {
      if (connection != null) {
        connection.close();
      }
    }

  }

  private static void saveNewLocallyAddedContact(final Bundle b,
      final Context context, final Account account, boolean onlyImportNotSync) {
    String sn = null;
    String cn = null;
    String description = null;
    String title = null;
    String registeredAddress = null;
    String destinationIndicator = null;
    String preferredDeliveryMethod = null;
    String faxNumber = null;
    String telephoneNumber = null;
    String internationalISDNNumber = null;
    String facsimileTelephoneNumber = null;
    String street = null;
    String postOfficeBox = null;
    String postalCode = null;
    String postalAddress = null;
    String physicalDeliveryOfficeName = null;
    String ou = null;
    String st = null;
    String l = null;
    String businessCategory = null;
    String departmentNumber = null;
    String displayName = null;
    String givenName = null;
    String homePhone = null;
    String homePostalAddress = null;
    String initials = null;
    String mail = null;
    String mobile = null;
    String o = null;
    String pager = null;
    String roomNumber = null;
    String uid = null;
    String preferredLanguage = null;
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
        faxNumber = b.getString(key);
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
      }
    }
    if (onlyImportNotSync) {
      uid = null;
      syncstatus = SYNC_STATUS_DO_NOT_SYNC;
    } else {
      syncstatus = SYNC_STATUS_LOCALLY_ADDED;
    }
    // Prepare contact creation request
    //TODO Alle Routinen müssen noch geschrieben werden
    batch.add(ContentProviderOperation
        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .withValue(ContactsContract.RawContacts.SYNC3, dn)
        .withValue(ContactsContract.RawContacts.SYNC1, syncstatus).build());
    batch.add(ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
            displayName)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            givenName)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            sn).build());
    if (telephoneNumber != null && telephoneNumber.length() > 0) {
      batch.add(ContentProviderOperation
          .newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER,
              telephoneNumber)
          .withValue(ContactsContract.CommonDataKinds.Phone.TYPE,
              Phone.TYPE_MAIN).build());
    }
    if (mail != null && mail.length() > 0) {
      batch.add(ContentProviderOperation
          .newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.Email.DATA, mail)
          .withValue(ContactsContract.CommonDataKinds.Email.TYPE,
              Email.TYPE_WORK).build());
    }
    // Ask the Contact provider to create a new contact
    try {
      batch.execute();
    } catch (Exception e) {
      // Display warning
      CharSequence txt = context.getString(R.string.contactCreationFailure);
      int duration = Toast.LENGTH_SHORT;
      Toast toast = Toast.makeText(context.getApplicationContext(), txt,
          duration);
      toast.show();
    }
  }

  public static void saveNewLocallyAddedContactAndSync(final Bundle b,
      final Account account, final Context context) {
    saveNewLocallyAddedContact(b, context, account, false);
  }
}