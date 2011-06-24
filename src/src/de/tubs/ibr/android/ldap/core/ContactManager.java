package de.tubs.ibr.android.ldap.core;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.StringTokenizer;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.provider.ContactsContract.RawContacts.Entity;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.RawContactsEntity;
import com.unboundid.ldap.sdk.Entry;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class ContactManager {
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

  public static void importLDAPContact(Entry entry, Account account,
      BatchOperation batch) {

  }

  public static void exportContactToLDAP(long id, BatchOperation batch) {

  }

  public static void deleteLDAPContact(long id, BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
    Uri contentAsSyncAdapter = ContactsContract.RawContacts.CONTENT_URI
        .buildUpon()
        .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
        .build();
    batch.add(ContentProviderOperation.newDelete(contentAsSyncAdapter)
        .withValue(ContactsContract.RawContacts._ID, id).build());
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
}
