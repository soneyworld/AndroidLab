package de.tubs.ibr.android.ldap.core;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.StringTokenizer;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.provider.ContactsContract;
import com.unboundid.ldap.sdk.Entry;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class ContactManager {
  public static void addLDAPContactToAccount(Entry entry, Account account, BatchOperation batch){
    
  }
  public static void importLDAPContact(Entry entry, Account account, BatchOperation batch){
    String name = entry.getAttributeValue(AttributeMapper.ATTR_FULL_NAME);
    String workPhone = entry.getAttributeValue(AttributeMapper.ATTR_PRIMARY_PHONE);
    String homePhone = entry.getAttributeValue(AttributeMapper.ATTR_HOME_PHONE);
    String mobile = entry.getAttributeValue(AttributeMapper.ATTR_MOBILE_PHONE);
    String pager = entry.getAttributeValue(AttributeMapper.ATTR_PAGER);
    String fax = entry.getAttributeValue(AttributeMapper.ATTR_FAX);
    String workEMail = entry.getAttributeValue(AttributeMapper.ATTR_PRIMARY_MAIL);
    String homeEMail = entry.getAttributeValue(AttributeMapper.ATTR_ALTERNATE_MAIL);
    String workAddress = entry.getAttributeValue(AttributeMapper.ATTR_PRIMARY_ADDRESS);
    String homeAddress = entry.getAttributeValue(AttributeMapper.ATTR_HOME_ADDRESS);
    String sourceId = entry.getAttributeValue(AttributeMapper.ATTR_UID);
    batch.add(ContentProviderOperation
        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .withValue(ContactsContract.RawContacts.SOURCE_ID, sourceId)
        .build());
    batch.add(ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        .build());
    if (workPhone != null) {
      batch.add(addPhoneNumber(workPhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK));
    }
    if (homePhone != null) {
      batch.add(addPhoneNumber(homePhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME));
    }
    if (mobile != null) {
      batch.add(addPhoneNumber(mobile,
          ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE));
    }
    if (pager != null) {
      batch.add(addPhoneNumber(pager,
          ContactsContract.CommonDataKinds.Phone.TYPE_PAGER));
    }
    if (fax != null) {
      batch.add(addPhoneNumber(fax,
          ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK));
    }
    if (workEMail != null) {
      batch.add(addEMailAddress(workEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    }
    if (homeEMail != null) {
      batch.add(addEMailAddress(homeEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_HOME));
    }
    if (workAddress != null) {
      batch.add(addPostalAddress(workAddress,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    }
    if (homeAddress != null) {
      batch.add(addPostalAddress(homeAddress,
          ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME));
    }
  }
  public static void exportContactToLDAP(long id, BatchOperation batch){
    
  }
  public static void deleteLDAPContact(BatchOperation batch){
    
  }
  public static void updateLDAPContact(BatchOperation batch){
    
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
  private static ContentProviderOperation addPhoneNumber(final String number,
      final int type) {
    return ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
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
  private static ContentProviderOperation addEMailAddress(final String address,
      final int type) {
    return ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
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
  private static ContentProviderOperation addPostalAddress(final String address,
      final int type) {
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
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredPostal.CONTENT_ITEM_TYPE)
        .withValue(ContactsContract.CommonDataKinds.StructuredPostal.TYPE, type)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredPostal.FORMATTED_ADDRESS,
            addr.toString()).build();
  }
}
