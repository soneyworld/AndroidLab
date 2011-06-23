package de.tubs.ibr.android.ldap.core;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.StringTokenizer;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.ContactsContract.RawContactsEntity;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldif.LDIFReader;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class ContactManager {
  public static void addLDAPContactToAccount(Entry entry, Account account,
      BatchOperation batch) {
    importLDAPContact(entry, account, batch);
  }

  public static void importLDAPContact(Entry entry, Account account,
      BatchOperation batch) {
    int rawContactInsertIndex = batch.size();
    String ldif = entry.toLDIFString();
    System.out.println(ldif);
    String[] classes = entry.getAttributeValues("objectClass");
    for (String string : classes) {
      if(string.equalsIgnoreCase("inetOrgPerson")){
        System.out.println(string); 
      }
    }
    String sourceId = entry.getAttributeValue(AttributeMapper.ATTR_UID);
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
    batch.add(ContentProviderOperation
        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .withValue(ContactsContract.RawContacts.SOURCE_ID, sourceId).build());
    batch.add(ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID,
            rawContactInsertIndex)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        .build());
    if (workPhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, workPhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK));
    }
    if (homePhone != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, homePhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME));
    }
    if (mobile != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, mobile,
          ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE));
    }
    if (pager != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, pager,
          ContactsContract.CommonDataKinds.Phone.TYPE_PAGER));
    }
    if (fax != null) {
      batch.add(addPhoneNumber(rawContactInsertIndex, fax,
          ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK));
    }
    if (workEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, workEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    }
    if (homeEMail != null) {
      batch.add(addEMailAddress(rawContactInsertIndex, homeEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_HOME));
    }
    if (workAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, workAddress,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    }
    if (homeAddress != null) {
      batch.add(addPostalAddress(rawContactInsertIndex, homeAddress,
          ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME));
    }
  }

  public static void exportContactToLDAP(long id, BatchOperation batch) {

  }

  public static void deleteLDAPContact(long id, BatchOperation batch) {

  }

  public static void updateLDAPContact(int id, Context context,
      Account account, Entry entry, BatchOperation batch) {
//    Uri entityUri = ContentUris.withAppendedId(RawContactsEntity.CONTENT_URI,
//        id);
//    Cursor c = context.getContentResolver().query(
//        entityUri,
//        new String[] { RawContactsEntity.CONTACT_ID, RawContactsEntity.DATA_ID,
//            RawContactsEntity.MIMETYPE, RawContactsEntity.DATA1 }, null, null,
//        null);
//    try {
//      while (c.moveToNext()) {
//        String sourceId = c.getString(0);
//        if (!c.isNull(1)) {
//          String mimeType = c.getString(2);
//          String data = c.getString(3);
//        }
//      }
//    } finally {
//      c.close();
//    }

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
      final int rawContactInsertIndex, final String number, final int type) {
    return ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
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
      final int rawContactInsertIndex, final String address, final int type) {
    return ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
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
      final int rawContactInsertIndex, final String address, final int type) {
    final StringBuilder addr = new StringBuilder();
    final StringTokenizer tokenizer = new StringTokenizer(address, "$");
    while (tokenizer.hasMoreTokens()) {
      addr.append(tokenizer.nextToken().trim());
      if (tokenizer.hasMoreTokens()) {
        addr.append(EOL);
      }
    }
    return ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
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
