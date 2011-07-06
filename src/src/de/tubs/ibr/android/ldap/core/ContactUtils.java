package de.tubs.ibr.android.ldap.core;

import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Organization;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.CommonDataKinds.StructuredPostal;
import android.provider.ContactsContract.CommonDataKinds.Website;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

public class ContactUtils {

  static void createStructuredName(String sn, String cn, String initials,
      String title, String givenName, BatchOperation batch, Uri dataUri) {
    createStructuredName(sn, cn, initials, title, givenName, batch, dataUri, 0);
  }

  static void createStructuredName(String sn, String cn, String initials,
      String title, String givenName, BatchOperation batch, Uri dataUri,
      int rawContactIndex) {
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, cn)
        .withValue(StructuredName.GIVEN_NAME, givenName)
        .withValue(StructuredName.FAMILY_NAME, sn)
        .withValue(StructuredName.PREFIX, title).build());
    createInitials(initials, batch, dataUri, rawContactIndex);
  }

  static void createMail(String mail, BatchOperation batch, Uri dataUri) {
    createMail(mail, null, batch, dataUri, 0);
  }

  static void createMail(String mail, String alternateMail,
      BatchOperation batch, Uri dataUri) {
    createMail(mail, alternateMail, batch, dataUri, 0);
  }

  static void createMail(String mail, BatchOperation batch, Uri dataUri,
      int rawContactIndex) {
    createMail(mail, null, batch, dataUri, rawContactIndex);
  }

  static void createMail(String mail, String alternateMail,
      BatchOperation batch, Uri dataUri, int rawContactIndex) {
    if (mail != null && mail.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
          .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
          .withValue(Email.DATA, mail).withValue(Email.TYPE, Email.TYPE_WORK)
          .build());
    }
    if (alternateMail != null && alternateMail.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
          .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
          .withValue(Email.DATA, alternateMail)
          .withValue(Email.TYPE, Email.TYPE_HOME).build());
    }
  }

  static void createRawContact(final Account account, String dn,
      String syncstatus, BatchOperation batch, Uri rawContactUri) {
    createRawContact(account, dn, syncstatus, "", null, null, batch,
        rawContactUri);
  }

  static void createRawContact(final Account account, String dn,
      String syncstatus, String message, String ldif, String sourceId,
      BatchOperation batch, Uri rawContactUri) {
    batch.add(ContentProviderOperation.newInsert(rawContactUri)
        .withValue(RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(RawContacts.ACCOUNT_NAME, account.name)
        .withValue(RawContacts.SOURCE_ID, sourceId)
        .withValue(RawContacts.SYNC1, syncstatus)
        .withValue(RawContacts.SYNC2, message).withValue(RawContacts.SYNC3, dn)
        .withValue(RawContacts.SYNC4, ldif).build());
  }

  static void createDescription(String description, BatchOperation batch,
      Uri dataUri) {
    createDescription(description, batch, dataUri, 0);
  }

  static void createDescription(String description, BatchOperation batch,
      Uri dataUri, int rawContactIndex) {
    if (description != null && description.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
          .withValue(Note.NOTE, description).build());
    }
  }

  static void createInitials(String initials, BatchOperation batch,
      Uri dataUri, int rawContactIndex) {
    if (initials != null && initials.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
          .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
          .withValue(Nickname.NAME, initials)
          .withValue(Nickname.TYPE, Nickname.TYPE_INITIALS).build());
    }
  }

  static void createPhoneNumbers(String telephoneNumber, String homePhone,
      String mobileNumber, String faxNumber, String pagerNumber,
      String telexNumber, String isdnNumber, BatchOperation batch, Uri dataUri,
      int rawContactInsertIndex) {
    if (telephoneNumber != null && telephoneNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, telephoneNumber)
          .withValue(Phone.TYPE, Phone.TYPE_MAIN).build());
    }
    if (homePhone != null && homePhone.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, homePhone)
          .withValue(Phone.TYPE, Phone.TYPE_HOME).build());
    }
    if (mobileNumber != null && mobileNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, mobileNumber)
          .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
    }
    if (faxNumber != null && faxNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, faxNumber)
          .withValue(Phone.TYPE, Phone.TYPE_FAX_WORK).build());
    }
    if (pagerNumber != null && pagerNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, pagerNumber)
          .withValue(Phone.TYPE, Phone.TYPE_PAGER).build());
    }
    if (telexNumber != null && telexNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, telexNumber)
          .withValue(Phone.TYPE, Phone.TYPE_TELEX).build());
    }
    if (isdnNumber != null && isdnNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, isdnNumber)
          .withValue(Phone.TYPE, Phone.TYPE_ISDN).build());
    }
  }

  public static void createSeeAlso(String seeAlso, BatchOperation batch,
      Uri dataUri) {
    createSeeAlso(seeAlso, batch, dataUri, 0);
  }

  public static void createSeeAlso(String seeAlso, BatchOperation batch,
      Uri dataUri, int rawContactInsertIndex) {
    if (seeAlso != null && seeAlso.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE)
          .withValue(Website.URL, seeAlso)
          .withValue(Website.TYPE, Website.TYPE_CUSTOM)
          .withValue(Website.LABEL, "SeeAlso").build());
    }

  }

  public static void createOrganization(String o, String ou,
      String departmentNumber, String l, String roomNumber,
      String preferredLanguage, String physicalDeliveryOfficeName,
      String businessCategory, BatchOperation batch, Uri dataUri) {
    createOrganization(o, ou, departmentNumber, l, roomNumber,
        preferredLanguage, physicalDeliveryOfficeName, businessCategory, batch,
        dataUri, 0);
  }

  public static void createOrganization(String o, String ou,
      String departmentNumber, String l, String roomNumber,
      String preferredLanguage, String physicalDeliveryOfficeName,
      String businessCategory, BatchOperation batch, Uri dataUri,
      int rawContactInsertIndex) {
    if ((o != null && o.length() > 0) || (ou != null && ou.length() > 0)
        || (l != null && l.length() > 0)
        || (businessCategory != null && businessCategory.length() > 0)) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
          .withValue(Organization.COMPANY, o)
          .withValue(Organization.TYPE, Organization.TYPE_WORK)
          .withValue(Organization.DEPARTMENT, ou)
          .withValue(Organization.OFFICE_LOCATION, l)
          .withValue(Organization.JOB_DESCRIPTION, businessCategory).build());
    }
    ContactUtils.createLDAPRow(AttributeMapper.DEPARTMENT_NUMBER,
        departmentNumber, batch, dataUri, rawContactInsertIndex);
    ContactUtils.createLDAPRow(AttributeMapper.ROOM_NUMBER, roomNumber, batch,
        dataUri, rawContactInsertIndex);
    ContactUtils.createLDAPRow(AttributeMapper.PREFERRED_LANGUAGE,
        preferredLanguage, batch, dataUri, rawContactInsertIndex);
    ContactUtils.createLDAPRow(AttributeMapper.PHYSICAL_DELIVERY_OFFICE_NAME,
        physicalDeliveryOfficeName, batch, dataUri, rawContactInsertIndex);
  }

  public static void createAddresses(String destinationIndicator,
      String registeredAddress, String street, String preferredDeliveryMethod,
      String postOfficeBox, String postalCode, String postalAddress,
      String homePostalAddress, String st, BatchOperation batch, Uri dataUri,
      int rawContactInsertIndex) {
    // Create Home Address
    if ((homePostalAddress != null && homePostalAddress.length() > 0)
        || (street != null && street.length() > 0)) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_TYPE)
          .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME)
          .withValue(StructuredPostal.FORMATTED_ADDRESS, homePostalAddress)
          .withValue(StructuredPostal.STREET, street).build());
    }
    // Create Work Address
    if ((st != null && st.length() > 0)
        || (postalAddress != null && postalAddress.length() > 0)
        || (postalCode != null && postalCode.length() > 0)
        || (postOfficeBox != null && postOfficeBox.length() > 0)) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_TYPE)
          .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK)
          .withValue(StructuredPostal.FORMATTED_ADDRESS, postalAddress)
          .withValue(StructuredPostal.POSTCODE, postalCode)
          .withValue(StructuredPostal.POBOX, postOfficeBox)
          .withValue(StructuredPostal.REGION, st).build());
    }
    ContactUtils.createLDAPRow(AttributeMapper.DESTINATION_INDICATOR,
        destinationIndicator, batch, dataUri);
    ContactUtils.createLDAPRow(AttributeMapper.REGISTERED_ADDRESS,
        registeredAddress, batch, dataUri);
    ContactUtils.createLDAPRow(AttributeMapper.PREFERRED_DELIVERY_METHOD,
        preferredDeliveryMethod, batch, dataUri);
  }

  public static void createPhoneNumbers(String telephoneNumber,
      String homePhone, String mobile, String faxNumber, String pagerNumber,
      String telexNumber, String isdnNumber, BatchOperation batch, Uri dataUri) {
    createPhoneNumbers(telephoneNumber, homePhone, mobile, faxNumber,
        pagerNumber, telexNumber, isdnNumber, batch, dataUri, 0);
  }

  public static void createAddresses(String destinationIndicator,
      String registeredAddress, String street, String preferredDeliveryMethod,
      String postOfficeBox, String postalCode, String postalAddress,
      String homePostalAddress, String st, BatchOperation batch, Uri dataUri) {
    createAddresses(destinationIndicator, registeredAddress, street,
        preferredDeliveryMethod, postOfficeBox, postalCode, postalAddress,
        homePostalAddress, st, batch, dataUri, 0);

  }

  public static void createLDAPRow(String key, String value,
      BatchOperation batch, Uri dataUri) {
    createLDAPRow(key, value, batch, dataUri, 0);
  }

  public static void createLDAPRow(String key, String value,
      BatchOperation batch, Uri dataUri, int rawContactInsertIndex) {
    if (key != null && key.length() > 0 && value != null && value.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
          .withValue(Data.MIMETYPE, LDAPRow.CONTENT_TYPE)
          .withValue(LDAPRow.KEY, key).withValue(LDAPRow.VALUE, value).build());
    }
  }

  static void loadLDAPRow(Bundle contact, Cursor c, int keyColumn,
      int valueColumn) {
    contact.putString(c.getString(keyColumn), c.getString(valueColumn));
  }

  static void loadAddress(Bundle contact, Cursor c, int typeColumn,
      int formattedAddressColumn, int streetColumn, int postBoxColumn,
      int regionColumn, int postalCodeColumn) {
    int type = c.getInt(typeColumn);
    switch (type) {
      case StructuredPostal.TYPE_WORK:
        contact.putString(AttributeMapper.POSTAL_ADDRESS,
            c.getString(formattedAddressColumn));
        contact.putString(AttributeMapper.POST_OFFICE_BOX,
            c.getString(postBoxColumn));
        contact.putString(AttributeMapper.POSTAL_CODE,
            c.getString(postalCodeColumn));
        break;
      case StructuredPostal.TYPE_HOME:
        contact.putString(AttributeMapper.HOME_ADDRESS,
            c.getString(formattedAddressColumn));
        contact.putString(AttributeMapper.STREET, c.getString(streetColumn));
        contact.putString(AttributeMapper.STATE, c.getString(regionColumn));
        break;
    }
  }

  static void loadName(Bundle contact, Cursor c, int displayColumn,
      int givenNameColumn, int familyNameColumn, int prefixColumn) {
    contact.putString(AttributeMapper.FULL_NAME, c.getString(displayColumn));
    contact.putString(AttributeMapper.DISPLAYNAME, c.getString(displayColumn));
    contact.putString(AttributeMapper.FIRST_NAME, c.getString(givenNameColumn));
    contact.putString(AttributeMapper.LAST_NAME, c.getString(familyNameColumn));
    contact.putString(AttributeMapper.TITLE, c.getString(prefixColumn));
  }

  @SuppressWarnings("deprecation")
  static void loadMail(Bundle contact, Cursor c, int typeColumn, int dataColumn) {
    int type = c.getInt(typeColumn);
    switch (type) {
      case Email.TYPE_WORK:
        contact
            .putString(AttributeMapper.PRIMARY_MAIL, c.getString(dataColumn));
        break;
      default:
        contact.putString(AttributeMapper.ALTERNATE_MAIL,
            c.getString(dataColumn));
        break;
    }
  }

  static void loadInitials(Bundle contact, Cursor c, int typeColum,
      int nameColumn) {
    int type = c.getInt(typeColum);
    switch (type) {
      case Nickname.TYPE_INITIALS:
        contact.putString(AttributeMapper.INITIALS, c.getString(2));
        break;
    }
  }

  static void loadOrganization(Bundle contact, Cursor c, int oColumn,
      int ouColumn) {
    contact.putString(AttributeMapper.ORGANIZATION, c.getString(oColumn));
    contact.putString(AttributeMapper.ORGANIZATION_UNIT, c.getString(ouColumn));
  }

  static void loadDescription(Bundle contact, Cursor c, int descColumn) {
    contact.putString(AttributeMapper.DESCRIPTION, c.getString(descColumn));
  }

  static void loadPhoneNumber(Bundle contact, Cursor c, int typeColumn,
      int numberColumn) {
    int type = c.getInt(typeColumn);
    switch (type) {
      case Phone.TYPE_HOME:
        contact
            .putString(AttributeMapper.HOME_PHONE, c.getString(numberColumn));
        break;
      case Phone.TYPE_WORK:
        contact.putString(AttributeMapper.PRIMARY_PHONE,
            c.getString(numberColumn));
        break;
      case Phone.TYPE_FAX_WORK:
        contact.putString(AttributeMapper.FAX, c.getString(numberColumn));
        break;
      case Phone.TYPE_MOBILE:
        contact.putString(AttributeMapper.MOBILE_PHONE,
            c.getString(numberColumn));
        break;
      case Phone.TYPE_PAGER:
        contact.putString(AttributeMapper.PAGER, c.getString(numberColumn));
        break;
      case Phone.TYPE_TELEX:
        contact.putString(AttributeMapper.TELEX, c.getString(numberColumn));
        break;
      case Phone.TYPE_ISDN:
        contact.putString(AttributeMapper.ISDN, c.getString(numberColumn));
        break;
    }
  }
}
