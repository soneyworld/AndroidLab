package de.tubs.ibr.android.ldap.core;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.StringTokenizer;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
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
import com.unboundid.ldap.sdk.DeleteRequest;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.migrate.ldapjdk.LDAPConstraints;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import de.tubs.ibr.android.ldap.provider.LDAPService;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class ContactManager {
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
    String name = entry.getAttributeValue(AttributeMapper.ATTR_FULL_NAME);
    String workPhone = entry
        .getAttributeValue(AttributeMapper.ATTR_PRIMARY_PHONE);
    String homePhone = entry.getAttributeValue(AttributeMapper.ATTR_HOME_PHONE);
    String mobile = entry.getAttributeValue(AttributeMapper.ATTR_MOBILE_PHONE);
    String pager = entry.getAttributeValue(AttributeMapper.ATTR_PAGER);
    String fax = entry.getAttributeValue(AttributeMapper.ATTR_FAX);
    String workEMail = entry
        .getAttributeValue(AttributeMapper.ATTR_PRIMARY_MAIL);
    String homeEMail = entry
        .getAttributeValue(AttributeMapper.ATTR_ALTERNATE_MAIL);
    String workAddress = entry
        .getAttributeValue(AttributeMapper.ATTR_PRIMARY_ADDRESS);
    String homeAddress = entry
        .getAttributeValue(AttributeMapper.ATTR_HOME_ADDRESS);
    // List all LDAP ObjectClasses
    String objectClass = "";
    for (String objectClazz : entry.getAttributeValues("objectClass")) {
      objectClass = objectClass + "\n" + objectClazz;
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
        .withValue(ContactsContract.RawContacts.SYNC1, "inSync")
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
        .build());
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
    String name = entry.getAttributeValue(AttributeMapper.ATTR_FULL_NAME);
    String workPhone = entry
        .getAttributeValue(AttributeMapper.ATTR_PRIMARY_PHONE);
    String homePhone = entry.getAttributeValue(AttributeMapper.ATTR_HOME_PHONE);
    String mobile = entry.getAttributeValue(AttributeMapper.ATTR_MOBILE_PHONE);
    String pager = entry.getAttributeValue(AttributeMapper.ATTR_PAGER);
    String fax = entry.getAttributeValue(AttributeMapper.ATTR_FAX);
    String workEMail = entry
        .getAttributeValue(AttributeMapper.ATTR_PRIMARY_MAIL);
    String homeEMail = entry
        .getAttributeValue(AttributeMapper.ATTR_ALTERNATE_MAIL);
    String workAddress = entry
        .getAttributeValue(AttributeMapper.ATTR_PRIMARY_ADDRESS);
    String homeAddress = entry
        .getAttributeValue(AttributeMapper.ATTR_HOME_ADDRESS);
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
        .build());
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
      DeleteRequest deleteRequest = new DeleteRequest(getDNofRawContact(id,
          context));
      LDAPResult deleteResult = conn.delete(deleteRequest);
      if (deleteRequest.getLastMessageID() == ResultCode.SUCCESS_INT_VALUE
          || deleteRequest.getLastMessageID() == ResultCode.NO_SUCH_OBJECT_INT_VALUE) {
        remotelyDeleted = true;
      } else {
        // Write error Message and result Code to local RawContact!
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
          String temp = c.getString(1);
          int endline = temp.indexOf("\n");
          if (endline > 0) {
            dn = temp.substring(0, endline);
          } else {
            dn = temp;
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

  public static void addLocalContactToLDAP(int rawcontactId,
      BatchOperation batchOperation, ServerInstance serverInstance,
      Context context) {
    String dn = getDNofRawContact(rawcontactId, context);
    if (dn == null) {
      dn = serverInstance.getBaseDN();
    }
    Entry ldapentry = new Entry(dn);
    Uri entityUri = ContentUris.withAppendedId(RawContactsEntity.CONTENT_URI,
        rawcontactId);
    Cursor c = context.getContentResolver().query(
        Data.CONTENT_URI,
        new String[] { RawContactsEntity.DATA_ID, RawContactsEntity.MIMETYPE,
            RawContactsEntity.DATA1, RawContactsEntity.DATA2,
            RawContactsEntity.DATA3, RawContactsEntity.DATA4,
            RawContactsEntity.DATA5, RawContactsEntity.DATA6,
            RawContactsEntity.DATA7, RawContactsEntity.DATA8,
            RawContactsEntity.DATA9, RawContactsEntity.DATA10,
            RawContactsEntity.DATA11, RawContactsEntity.DATA12,
            RawContactsEntity.DATA13, RawContactsEntity.DATA14,
            RawContactsEntity.DATA15 },
        Data.RAW_CONTACT_ID + "=" + rawcontactId,
        new String[] { String.valueOf(rawcontactId) }, null);
    try {
      while (c.moveToNext()) {
        String mimetype = c.getString(2);
        // is a name
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/name")) {
          ldapentry
              .addAttribute(AttributeMapper.ATTR_FULL_NAME, c.getString(1));
          ldapentry.addAttribute("displayName", c.getString(1));
          ldapentry
              .addAttribute(AttributeMapper.ATTR_LAST_NAME, c.getString(2));
          ldapentry.addAttribute(AttributeMapper.ATTR_FIRST_NAME,
              c.getString(3));
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/im")) {

        }
        if (mimetype
            .equalsIgnoreCase("vnd.android.cursor.item/postal-address_v2")) {
          int type = c.getInt(2);
          switch (type) {
            case StructuredPostal.TYPE_WORK:
              ldapentry.addAttribute(AttributeMapper.ATTR_PRIMARY_ADDRESS,
                  c.getString(4));
              ldapentry.addAttribute("postOfficeBox", c.getString(5));
              ldapentry.addAttribute("postalCode", c.getString(9));
              break;
            case StructuredPostal.TYPE_HOME:
              ldapentry.addAttribute(AttributeMapper.ATTR_HOME_ADDRESS,
                  c.getString(4));
            default:
              break;
          }
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/photo")) {
          
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/phone_v2")) {
          int type = c.getInt(2);
          switch (type) {
            case Phone.TYPE_HOME:
              ldapentry.addAttribute(AttributeMapper.ATTR_HOME_PHONE,
                  c.getString(1));
              break;
            case Phone.TYPE_WORK:
              ldapentry.addAttribute(AttributeMapper.ATTR_PRIMARY_PHONE,
                  c.getString(1));
              break;
            case Phone.TYPE_FAX_WORK:
              ldapentry.addAttribute(AttributeMapper.ATTR_FAX, c.getString(1));
            case Phone.TYPE_MOBILE:
              ldapentry.addAttribute(AttributeMapper.ATTR_MOBILE_PHONE,
                  c.getString(1));
            case Phone.TYPE_PAGER:
              ldapentry
                  .addAttribute(AttributeMapper.ATTR_PAGER, c.getString(1));
            default:
              break;
          }
        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/organization")) {

        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/nickname")) {

        }
        if (mimetype.equalsIgnoreCase("vnd.android.cursor.item/email_v2")) {
          int type = c.getInt(2);
          switch (type) {
            case Email.TYPE_WORK:
              ldapentry.addAttribute(AttributeMapper.ATTR_PRIMARY_MAIL,
                  c.getString(1));
              break;
            case Email.TYPE_HOME:
              ldapentry.addAttribute(AttributeMapper.ATTR_ALTERNATE_MAIL,
                  c.getString(1));
            default:
              break;
          }
        }
      }
    } finally {
      c.close();
    }
  }
}
