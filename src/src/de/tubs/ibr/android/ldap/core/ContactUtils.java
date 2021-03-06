package de.tubs.ibr.android.ldap.core;

import java.util.Map;
import java.util.Set;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.content.ContentProviderOperation.Builder;
import android.content.ContentUris;
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

  // static void createStructuredName(Bundle b, BatchOperation batch, Uri
  // dataUri) {
  // createStructuredName(b, batch, dataUri, 0);
  // }

  static void createStructuredName(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactIndex, boolean withBackReference) {
    boolean insert = false;
    Builder contact;
    if (withBackReference) {
      contact = ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
          .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
    } else {
      contact = ContentProviderOperation.newInsert(dataUri)
          .withValue(Data.RAW_CONTACT_ID, rawContactIndex)
          .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE);
    }
    String s = b.getString(AttributeMapper.FULL_NAME);
    if (s != null) {
      contact = contact.withValue(StructuredName.DISPLAY_NAME, s);
      insert = true;
    }
    s = b.getString(AttributeMapper.FIRST_NAME);
    if (s != null) {
      contact = contact.withValue(StructuredName.GIVEN_NAME, s);
      insert = true;
    }
    s = b.getString(AttributeMapper.LAST_NAME);
    if (s != null) {
      contact = contact.withValue(StructuredName.FAMILY_NAME, s);
      insert = true;
    }
    s = b.getString(AttributeMapper.TITLE);
    if (s != null) {
      contact = contact.withValue(StructuredName.PREFIX, s);
      insert = true;
    }
    if (insert) {
      batch.add(contact.build());
    }
    createInitials(b, batch, dataUri, rawContactIndex);
  }

  // static void createMail(Bundle b, BatchOperation batch, Uri dataUri) {
  // createMail(b, batch, dataUri, 0);
  // }

  static void createMail(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactIndex, boolean withBackReference) {
    String mail = b.getString(AttributeMapper.PRIMARY_MAIL);
    @SuppressWarnings("deprecation")
    String alternateMail = b.getString(AttributeMapper.ALTERNATE_MAIL);
    if (mail != null && mail.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
            .withValue(Email.DATA, mail).withValue(Email.TYPE, Email.TYPE_WORK)
            .build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactIndex)
            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
            .withValue(Email.DATA, mail).withValue(Email.TYPE, Email.TYPE_WORK)
            .build());
      }
    }
    if (alternateMail != null && alternateMail.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
            .withValue(Email.DATA, alternateMail)
            .withValue(Email.TYPE, Email.TYPE_HOME).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactIndex)
            .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
            .withValue(Email.DATA, alternateMail)
            .withValue(Email.TYPE, Email.TYPE_HOME).build());
      }
    }
  }

  static void createRawContact(final Account account, Bundle b,
      BatchOperation batch, Uri rawContactUri) {
    batch
        .add(ContentProviderOperation
            .newInsert(rawContactUri)
            .withValue(RawContacts.ACCOUNT_TYPE, account.type)
            .withValue(RawContacts.ACCOUNT_NAME, account.name)
            .withValue(RawContacts.SOURCE_ID,
                b.getString(AttributeMapper.ATTR_UID))
            .withValue(RawContacts.SYNC1,
                b.getString(ContactManager.LDAP_SYNC_STATUS_KEY))
            .withValue(RawContacts.SYNC2,
                b.getString(ContactManager.LDAP_ERROR_MESSAGE_KEY))
            .withValue(RawContacts.SYNC3, b.getString(AttributeMapper.DN))
            .withValue(RawContacts.SYNC4,
                b.getString(ContactManager.LDAP_LDIF_DETAILS_KEY)).build());
  }

  static void createFullContact(final Account account, Bundle b,
      BatchOperation batch, Uri rawContactUri, Uri dataUri) {
    int rawContactIndex = batch.size();
    createRawContact(account, b, batch, rawContactUri);
    insertDataForContact(b, batch, dataUri, rawContactIndex, true);
  }

  static void insertDataForContact(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactIndex, boolean withBackReference) {
    createStructuredName(b, batch, dataUri, rawContactIndex, withBackReference);
    createDescription(b, batch, dataUri, rawContactIndex, withBackReference);
    createPhoneNumbers(b, batch, dataUri, rawContactIndex, withBackReference);
    createMail(b, batch, dataUri, rawContactIndex, withBackReference);
    createAddresses(b, batch, dataUri, rawContactIndex, withBackReference);
    createSeeAlso(b, batch, dataUri, rawContactIndex, withBackReference);
    createOrganization(b, batch, dataUri, rawContactIndex, withBackReference);
    createLDAPRow(AttributeMapper.UID, b.getString(AttributeMapper.UID), batch,
        dataUri, rawContactIndex, withBackReference);
  }

  // static void createDescription(Bundle b, BatchOperation batch, Uri dataUri)
  // {
  // createDescription(b, batch, dataUri, 0);
  // }

  static void createDescription(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactIndex, boolean withBackReference) {
    String description = b.getString(AttributeMapper.DESCRIPTION);
    if (description != null && description.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
            .withValue(Note.NOTE, description).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, 0)
            .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
            .withValue(Note.NOTE, description).build());
      }
    }
  }

  static void createInitials(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactIndex) {
    String initials = b.getString(AttributeMapper.INITIALS);
    if (initials != null && initials.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, rawContactIndex)
          .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
          .withValue(Nickname.NAME, initials)
          .withValue(Nickname.TYPE, Nickname.TYPE_INITIALS).build());
    }
  }

  static void createPhoneNumbers(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactInsertIndex, boolean withBackReference) {
    String telephoneNumber = b.getString(AttributeMapper.PRIMARY_PHONE);
    if (telephoneNumber != null && telephoneNumber.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, telephoneNumber)
            .withValue(Phone.TYPE, Phone.TYPE_MAIN).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, telephoneNumber)
            .withValue(Phone.TYPE, Phone.TYPE_MAIN).build());
      }
    }
    String homePhone = b.getString(AttributeMapper.HOME_PHONE);
    if (homePhone != null && homePhone.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, homePhone)
            .withValue(Phone.TYPE, Phone.TYPE_HOME).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, homePhone)
            .withValue(Phone.TYPE, Phone.TYPE_HOME).build());
      }
    }
    String mobileNumber = b.getString(AttributeMapper.MOBILE_PHONE);
    if (mobileNumber != null && mobileNumber.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, mobileNumber)
            .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, mobileNumber)
            .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
      }
    }
    String faxNumber = b.getString(AttributeMapper.FAX);
    if (faxNumber != null && faxNumber.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, faxNumber)
            .withValue(Phone.TYPE, Phone.TYPE_FAX_WORK).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, faxNumber)
            .withValue(Phone.TYPE, Phone.TYPE_FAX_WORK).build());
      }
    }
    String pagerNumber = b.getString(AttributeMapper.PAGER);
    if (pagerNumber != null && pagerNumber.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, pagerNumber)
            .withValue(Phone.TYPE, Phone.TYPE_PAGER).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, pagerNumber)
            .withValue(Phone.TYPE, Phone.TYPE_PAGER).build());
      }
    }
    String telexNumber = b.getString(AttributeMapper.TELEX);
    if (telexNumber != null && telexNumber.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, telexNumber)
            .withValue(Phone.TYPE, Phone.TYPE_TELEX).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, telexNumber)
            .withValue(Phone.TYPE, Phone.TYPE_TELEX).build());
      }
    }
    String isdnNumber = b.getString(AttributeMapper.ISDN);
    if (isdnNumber != null && isdnNumber.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, isdnNumber)
            .withValue(Phone.TYPE, Phone.TYPE_ISDN).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
            .withValue(Phone.NUMBER, isdnNumber)
            .withValue(Phone.TYPE, Phone.TYPE_ISDN).build());
      }
    }
  }

  // public static void createSeeAlso(Bundle b, BatchOperation batch, Uri
  // dataUri) {
  // createSeeAlso(b, batch, dataUri, 0);
  // }

  public static void createSeeAlso(Bundle b, BatchOperation batch, Uri dataUri,
      int rawContactInsertIndex, boolean withBackReference) {
    String seeAlso = b.getString(AttributeMapper.SEE_ALSO);
    if (seeAlso != null && seeAlso.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE)
            .withValue(Website.URL, seeAlso)
            .withValue(Website.TYPE, Website.TYPE_CUSTOM)
            .withValue(Website.LABEL, AttributeMapper.SEE_ALSO).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Website.CONTENT_ITEM_TYPE)
            .withValue(Website.URL, seeAlso)
            .withValue(Website.TYPE, Website.TYPE_CUSTOM)
            .withValue(Website.LABEL, AttributeMapper.SEE_ALSO).build());
      }
    }
  }

  // public static void createOrganization(Bundle b, BatchOperation batch,
  // Uri dataUri) {
  // createOrganization(b, batch, dataUri, 0);
  // }

  public static void createOrganization(Bundle b, BatchOperation batch,
      Uri dataUri, int rawContactInsertIndex, boolean withBackReference) {
    String o = b.getString(AttributeMapper.ORGANIZATION);
    String ou = b.getString(AttributeMapper.ORGANIZATION_UNIT);
    String departmentNumber = b.getString(AttributeMapper.DEPARTMENT_NUMBER);
    String l = b.getString(AttributeMapper.LOCALITY);
    String roomNumber = b.getString(AttributeMapper.ROOM_NUMBER);
    String preferredLanguage = b.getString(AttributeMapper.PREFERRED_LANGUAGE);
    String physicalDeliveryOfficeName = b
        .getString(AttributeMapper.PHYSICAL_DELIVERY_OFFICE_NAME);
    String businessCategory = b.getString(AttributeMapper.BUSINESS_CATEGORY);
    if ((o != null && o.length() > 0) || (ou != null && ou.length() > 0)
        || (l != null && l.length() > 0)
        || (businessCategory != null && businessCategory.length() > 0)) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
            .withValue(Organization.COMPANY, o)
            .withValue(Organization.TYPE, Organization.TYPE_WORK)
            .withValue(Organization.DEPARTMENT, ou)
            .withValue(Organization.OFFICE_LOCATION, l)
            .withValue(Organization.JOB_DESCRIPTION, businessCategory).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
            .withValue(Organization.COMPANY, o)
            .withValue(Organization.TYPE, Organization.TYPE_WORK)
            .withValue(Organization.DEPARTMENT, ou)
            .withValue(Organization.OFFICE_LOCATION, l)
            .withValue(Organization.JOB_DESCRIPTION, businessCategory).build());
      }
    }
    ContactUtils.createLDAPRow(AttributeMapper.DEPARTMENT_NUMBER,
        departmentNumber, batch, dataUri, rawContactInsertIndex,
        withBackReference);
    ContactUtils.createLDAPRow(AttributeMapper.ROOM_NUMBER, roomNumber, batch,
        dataUri, rawContactInsertIndex, withBackReference);
    ContactUtils.createLDAPRow(AttributeMapper.PREFERRED_LANGUAGE,
        preferredLanguage, batch, dataUri, rawContactInsertIndex,
        withBackReference);
    ContactUtils.createLDAPRow(AttributeMapper.PHYSICAL_DELIVERY_OFFICE_NAME,
        physicalDeliveryOfficeName, batch, dataUri, rawContactInsertIndex,
        withBackReference);
  }

  public static void createAddresses(Bundle b, BatchOperation batch,
      Uri dataUri, int rawContactInsertIndex, boolean withBackReference) {
    // Create Home Address
    String destinationIndicator = b
        .getString(AttributeMapper.DESTINATION_INDICATOR);
    String registeredAddress = b.getString(AttributeMapper.REGISTERED_ADDRESS);
    String street = b.getString(AttributeMapper.STREET);
    String preferredDeliveryMethod = b
        .getString(AttributeMapper.PREFERRED_DELIVERY_METHOD);
    String postOfficeBox = b.getString(AttributeMapper.POST_OFFICE_BOX);
    String postalCode = b.getString(AttributeMapper.POSTAL_CODE);
    String postalAddress = b.getString(AttributeMapper.POSTAL_ADDRESS);
    String homePostalAddress = b.getString(AttributeMapper.HOME_ADDRESS);
    String st = b.getString(AttributeMapper.STATE);
    if ((homePostalAddress != null && homePostalAddress.length() > 0)
        || (street != null && street.length() > 0)) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
            .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME)
            .withValue(StructuredPostal.FORMATTED_ADDRESS, homePostalAddress)
            .withValue(StructuredPostal.STREET, street).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
            .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_HOME)
            .withValue(StructuredPostal.FORMATTED_ADDRESS, homePostalAddress)
            .withValue(StructuredPostal.STREET, street).build());
      }
    }
    // Create Work Address
    if ((st != null && st.length() > 0)
        || (postalAddress != null && postalAddress.length() > 0)
        || (postalCode != null && postalCode.length() > 0)
        || (postOfficeBox != null && postOfficeBox.length() > 0)) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
            .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK)
            .withValue(StructuredPostal.FORMATTED_ADDRESS, postalAddress)
            .withValue(StructuredPostal.POSTCODE, postalCode)
            .withValue(StructuredPostal.POBOX, postOfficeBox)
            .withValue(StructuredPostal.REGION, st).build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_ITEM_TYPE)
            .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK)
            .withValue(StructuredPostal.FORMATTED_ADDRESS, postalAddress)
            .withValue(StructuredPostal.POSTCODE, postalCode)
            .withValue(StructuredPostal.POBOX, postOfficeBox)
            .withValue(StructuredPostal.REGION, st).build());
      }
    }
    ContactUtils.createLDAPRow(AttributeMapper.DESTINATION_INDICATOR,
        destinationIndicator, batch, dataUri, rawContactInsertIndex,
        withBackReference);
    ContactUtils.createLDAPRow(AttributeMapper.REGISTERED_ADDRESS,
        registeredAddress, batch, dataUri, rawContactInsertIndex,
        withBackReference);
    ContactUtils.createLDAPRow(AttributeMapper.PREFERRED_DELIVERY_METHOD,
        preferredDeliveryMethod, batch, dataUri, rawContactInsertIndex,
        withBackReference);
  }

  //
  // public static void createPhoneNumbers(Bundle b, BatchOperation batch,
  // Uri dataUri) {
  // createPhoneNumbers(b, batch, dataUri, 0);
  // }

  // public static void createAddresses(Bundle b, BatchOperation batch, Uri
  // dataUri) {
  // createAddresses(b, batch, dataUri, 0);
  //
  // }

  // public static void createLDAPRow(String key, String value,
  // BatchOperation batch, Uri dataUri) {
  // createLDAPRow(key, value, batch, dataUri, 0);
  // }

  public static void createLDAPRow(String key, String value,
      BatchOperation batch, Uri dataUri, int rawContactInsertIndex,
      boolean withBackReference) {
    if (key != null && key.length() > 0 && value != null && value.length() > 0) {
      if (withBackReference) {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValueBackReference(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, LDAPRow.CONTENT_ITEM_TYPE)
            .withValue(LDAPRow.KEY, key).withValue(LDAPRow.VALUE, value)
            .build());
      } else {
        batch.add(ContentProviderOperation.newInsert(dataUri)
            .withValue(Data.RAW_CONTACT_ID, rawContactInsertIndex)
            .withValue(Data.MIMETYPE, LDAPRow.CONTENT_ITEM_TYPE)
            .withValue(LDAPRow.KEY, key).withValue(LDAPRow.VALUE, value)
            .build());
      }
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
        contact.putString(AttributeMapper.STATE, c.getString(regionColumn));
        break;
      case StructuredPostal.TYPE_HOME:
        contact.putString(AttributeMapper.HOME_ADDRESS,
            c.getString(formattedAddressColumn));
        contact.putString(AttributeMapper.STREET, c.getString(streetColumn));

        break;
    }
  }

  @SuppressWarnings("deprecation")
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

  static void loadOrganization(Bundle contact, Cursor c, int typeColumn,
      int oColumn, int ouColumn, int lColumn, int categoryColumn) {
    int type = c.getInt(typeColumn);
    if (type == Organization.TYPE_WORK) {
      contact.putString(AttributeMapper.ORGANIZATION, c.getString(oColumn));
      contact.putString(AttributeMapper.ORGANIZATION_UNIT,
          c.getString(ouColumn));
      contact.putString(AttributeMapper.LOCALITY, c.getString(lColumn));
      contact.putString(AttributeMapper.BUSINESS_CATEGORY,
          c.getString(categoryColumn));
    }
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
      case Phone.TYPE_MAIN:
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

  static void loadSeeAlso(Bundle b, Cursor c, int typeColumn, int labelColumn,
      int urlColumn) {
    int type = c.getInt(typeColumn);
    if (type == Website.TYPE_CUSTOM) {
      String label = c.getString(labelColumn);
      String seeAlso = c.getString(urlColumn);
      if (label != null && label.equalsIgnoreCase(AttributeMapper.SEE_ALSO)) {
        b.putString(AttributeMapper.SEE_ALSO, seeAlso);
      }
    }
  }

  public static void createUpdateBatch(Set<String> insertKeys,
      Set<String> deleteKeys, Map<String, String> updateMap,
      BatchOperation batch, Bundle newcontact, Bundle oldcontact, Uri dataUri,
      int rawcontactId) {
    // DATABASE FIX
    for (String string : AttributeMapper.getNameSubAttrs()) {
      if (updateMap.containsKey(string)) {
        for (String s : AttributeMapper.getNameSubAttrs()) {
          updateMap.put(s, newcontact.getString(s));
        }
        break;
      }
    }
    for (String string : AttributeMapper.getPostalHomeAddressAttrs()) {
      if (updateMap.containsKey(string)) {
        for (String s : AttributeMapper.getPostalHomeAddressAttrs()) {
          updateMap.put(s, newcontact.getString(s));
        }
        break;
      }
    }
    for (String string : AttributeMapper.getPostalWorkAddressAttrs()) {
      if (updateMap.containsKey(string)) {
        for (String s : AttributeMapper.getPostalWorkAddressAttrs()) {
          updateMap.put(s, newcontact.getString(s));
        }
        break;
      }
    }
    for (String string : AttributeMapper.getOrganizationSubAttrs()) {
      if (updateMap.containsKey(string)) {
        for (String s : AttributeMapper.getOrganizationSubAttrs()) {
          updateMap.put(s, newcontact.getString(s));
        }
        break;
      }
    }
    // END DATABASE FIX
    // Cleaning up the insertSet and the deleteSet with dependencies to
    // updateMap
    Set<String> updateSet = updateMap.keySet();
    for (String key : updateSet) {
      if (AttributeMapper.isNameSubAttr(key)) {
        insertKeys.removeAll(AttributeMapper.getNameSubAttrs());
        deleteKeys.removeAll(AttributeMapper.getNameSubAttrs());
      } else if (AttributeMapper.isPostalHomeAddressAttr(key)) {
        insertKeys.removeAll(AttributeMapper.getPostalHomeAddressAttrs());
        deleteKeys.removeAll(AttributeMapper.getPostalHomeAddressAttrs());
      } else if (AttributeMapper.isPostalWorkAddressAttr(key)) {
        insertKeys.removeAll(AttributeMapper.getPostalWorkAddressAttrs());
        deleteKeys.removeAll(AttributeMapper.getPostalWorkAddressAttrs());
      } else if (AttributeMapper.isOrganizationSubAttr(key)) {
        insertKeys.removeAll(AttributeMapper.getOrganizationSubAttrs());
        deleteKeys.removeAll(AttributeMapper.getOrganizationSubAttrs());
      }
    }
    Bundle action = new Bundle();
    for (String insert : insertKeys) {
      action.putString(insert, newcontact.getString(insert));
    }
    insertDataForContact(action, batch, dataUri, rawcontactId, false);
    action.clear();
    for (String delete : deleteKeys) {
      action.putString(delete, oldcontact.getString(delete));
    }
    deleteDataForContact(action, batch, dataUri, rawcontactId);
    action.clear();
    for (String modify : updateSet) {
      action.putString(modify, newcontact.getString(modify));
    }
    updateDataForContact(action, batch, dataUri, rawcontactId);
  }

  private static void updateDataForContact(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    updateStructuredName(action, batch, dataUri, rawcontactId);
    updateDescription(action, batch, dataUri, rawcontactId);
    updateMail(action, batch, dataUri, rawcontactId);
    updateLDAPRows(action, batch, dataUri, rawcontactId);
    updatePhoneNumbers(action, batch, dataUri, rawcontactId);
    updateSeeAlso(action, batch, dataUri, rawcontactId);
    updateOrganization(action, batch, dataUri, rawcontactId);
    updatePostalAdresses(action, batch, dataUri, rawcontactId);
  }

  private static void updatePostalAdresses(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    boolean updatehome = false;
    boolean updatework = false;
    Builder home = ContentProviderOperation.newUpdate(dataUri).withSelection(
        Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
            + StructuredPostal.CONTENT_ITEM_TYPE + "' AND "
            + StructuredPostal.TYPE + "='" + StructuredPostal.TYPE_HOME + "'",
        new String[] { String.valueOf(rawcontactId) });
    Builder work = ContentProviderOperation.newUpdate(dataUri).withSelection(
        Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
            + StructuredPostal.CONTENT_ITEM_TYPE + "' AND "
            + StructuredPostal.TYPE + "='" + StructuredPostal.TYPE_WORK + "'",
        new String[] { String.valueOf(rawcontactId) });
    String s = action.getString(AttributeMapper.HOME_ADDRESS);
    if (action.containsKey(AttributeMapper.HOME_ADDRESS)) {
      home = home.withValue(StructuredPostal.FORMATTED_ADDRESS, s);
      updatehome = true;
    }
    s = action.getString(AttributeMapper.STREET);
    if (action.containsKey(AttributeMapper.STREET)) {
      home = home.withValue(StructuredPostal.STREET, s);
      updatehome = true;
    }
    s = action.getString(AttributeMapper.POSTAL_ADDRESS);
    if (action.containsKey(AttributeMapper.POSTAL_ADDRESS)) {
      work = work.withValue(StructuredPostal.FORMATTED_ADDRESS, s);
      updatework = true;
    }
    s = action.getString(AttributeMapper.POSTAL_CODE);
    if (action.containsKey(AttributeMapper.POSTAL_CODE)) {
      work = work.withValue(StructuredPostal.POSTCODE, s);
      updatework = true;
    }
    s = action.getString(AttributeMapper.POST_OFFICE_BOX);
    if (action.containsKey(AttributeMapper.POST_OFFICE_BOX)) {
      work = work.withValue(StructuredPostal.POBOX, s);
      updatework = true;
    }
    s = action.getString(AttributeMapper.STATE);
    if (action.containsKey(AttributeMapper.STATE)) {
      work = work.withValue(StructuredPostal.REGION, s);
      updatework = true;
    }
    if (updatehome)
      batch.add(home.build());
    if (updatework)
      batch.add(work.build());
  }

  private static void updateOrganization(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    boolean update = false;
    Builder orga = ContentProviderOperation.newUpdate(dataUri).withSelection(
        Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
            + Organization.CONTENT_ITEM_TYPE + "' AND " + Organization.TYPE
            + "='" + Organization.TYPE_WORK + "'",
        new String[] { String.valueOf(rawcontactId) });
    String s = action.getString(AttributeMapper.ORGANIZATION);
    if (action.containsKey(AttributeMapper.ORGANIZATION)) {
      orga.withValue(Organization.COMPANY, s);
      update = true;
    }
    s = action.getString(AttributeMapper.ORGANIZATION_UNIT);
    if (action.containsKey(AttributeMapper.ORGANIZATION_UNIT)) {
      orga.withValue(Organization.DEPARTMENT, s);
      update = true;
    }
    s = action.getString(AttributeMapper.BUSINESS_CATEGORY);
    if (action.containsKey(AttributeMapper.BUSINESS_CATEGORY)) {
      orga.withValue(Organization.JOB_DESCRIPTION, s);
      update = true;
    }
    s = action.getString(AttributeMapper.LOCALITY);
    if (action.containsKey(AttributeMapper.LOCALITY)) {
      orga.withValue(Organization.OFFICE_LOCATION, s);
      update = true;
    }
    if (update) {
      batch.add(orga.build());
    }
  }

  private static void updateSeeAlso(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    String seeAlso = action.getString(AttributeMapper.SEE_ALSO);
    if (action.containsKey(AttributeMapper.SEE_ALSO)) {
      batch.add(ContentProviderOperation
          .newUpdate(dataUri)
          .withSelection(
              Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                  + Website.CONTENT_ITEM_TYPE + "' AND " + Website.TYPE + "='"
                  + Website.TYPE_CUSTOM + "' AND " + Website.LABEL + "='"
                  + AttributeMapper.SEE_ALSO + "'",
              new String[] { String.valueOf(rawcontactId) })
          .withValue(Website.URL, seeAlso).build());
    }
  }

  private static void updatePhoneNumbers(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    for (String key : action.keySet()) {
      String number = action.getString(key);
      if (key.equalsIgnoreCase(AttributeMapper.HOME_PHONE)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_HOME + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      } else if (key.equalsIgnoreCase(AttributeMapper.PRIMARY_PHONE)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_MAIN + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      } else if (key.equalsIgnoreCase(AttributeMapper.FAX)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_FAX_WORK + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      } else if (key.equalsIgnoreCase(AttributeMapper.MOBILE_PHONE)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_MOBILE + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      } else if (key.equalsIgnoreCase(AttributeMapper.PAGER)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_PAGER + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      } else if (key.equalsIgnoreCase(AttributeMapper.TELEX)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_TELEX + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      } else if (key.equalsIgnoreCase(AttributeMapper.ISDN)) {
        batch.add(ContentProviderOperation
            .newUpdate(dataUri)
            .withSelection(
                Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                    + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.TYPE + "='"
                    + Phone.TYPE_ISDN + "'",
                new String[] { String.valueOf(rawcontactId) })
            .withValue(Phone.NUMBER, number).build());
      }
    }
  }

  private static void updateLDAPRows(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    for (String key : action.keySet()) {
      if (AttributeMapper.isRowAttr(key)) {
        String value = action.getString(key);
        batch
            .add(ContentProviderOperation
                .newUpdate(dataUri)
                .withSelection(
                    Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                        + LDAPRow.CONTENT_ITEM_TYPE + "' AND " + LDAPRow.KEY
                        + "=?",
                    new String[] { String.valueOf(rawcontactId), key })
                .withValue(LDAPRow.VALUE, value).build());
      }
    }
  }

  @SuppressWarnings("deprecation")
  private static void updateMail(Bundle action, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    Builder contact = ContentProviderOperation.newUpdate(dataUri);
    Builder alternativecontact = ContentProviderOperation.newUpdate(dataUri);
    String s = action.getString(AttributeMapper.PRIMARY_MAIL);
    if (action.containsKey(AttributeMapper.PRIMARY_MAIL)) {
      batch.add(contact
          .withSelection(
              Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                  + Email.CONTENT_ITEM_TYPE + "' AND " + Email.TYPE + "='"
                  + Email.TYPE_WORK + "'",
              new String[] { String.valueOf(rawcontactId) })
          .withValue(Email.DATA, s).build());
    }
    s = action.getString(AttributeMapper.ALTERNATE_MAIL);
    if (action.containsKey(AttributeMapper.ALTERNATE_MAIL)) {
      batch.add(alternativecontact
          .withSelection(
              Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "=? AND "
                  + Email.TYPE + "='" + Email.TYPE_HOME + "'",
              new String[] { String.valueOf(rawcontactId),
                  Email.CONTENT_ITEM_TYPE }).withValue(Email.DATA, s).build());
    }
  }

  private static void updateDescription(Bundle action, BatchOperation batch,
      Uri dataUri, int rawContactId) {
    String desc = action.getString(AttributeMapper.DESCRIPTION);
    if (action.containsKey(AttributeMapper.DESCRIPTION)) {
      batch.add(ContentProviderOperation
          .newUpdate(dataUri)
          .withSelection(
              Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                  + Note.CONTENT_ITEM_TYPE + "'",
              new String[] { String.valueOf(rawContactId) })
          .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
          .withValue(Note.NOTE, desc).build());
    }
  }

  private static void updateStructuredName(Bundle b, BatchOperation batch,
      Uri dataUri, int rawContactId) {
    boolean update = false;
    Builder builder = ContentProviderOperation.newUpdate(dataUri)
        .withSelection(
            Data.RAW_CONTACT_ID + "='" + String.valueOf(rawContactId) + "'"
                + " AND " + Data.MIMETYPE + "='"
                + StructuredName.CONTENT_ITEM_TYPE + "'", null);
    String s = b.getString(AttributeMapper.TITLE);
    if (b.containsKey(AttributeMapper.TITLE)) {
      builder.withValue(StructuredName.PREFIX, s);
      update = true;
    }
    s = b.getString(AttributeMapper.FULL_NAME);
    if (b.containsKey(AttributeMapper.FULL_NAME)) {
      builder.withValue(StructuredName.DISPLAY_NAME, s);
      update = true;
    }
    s = b.getString(AttributeMapper.FIRST_NAME);
    if (b.containsKey(AttributeMapper.FIRST_NAME)) {
      builder.withValue(StructuredName.GIVEN_NAME, s);
      update = true;
    }
    s = b.getString(AttributeMapper.LAST_NAME);
    if (b.containsKey(AttributeMapper.LAST_NAME)) {
      builder.withValue(StructuredName.FAMILY_NAME, s);
      update = true;
    }
    if (update)
      batch.add(builder.build());
    updateInitials(b, batch, dataUri, rawContactId);
  }

  public static void updateInitials(Bundle b, BatchOperation batch,
      Uri dataUri, int rawContactId) {
    String initials = b.getString(AttributeMapper.INITIALS);
    if (b.containsKey(AttributeMapper.INITIALS)) {
      batch.add(ContentProviderOperation
          .newUpdate(dataUri)
          .withSelection(
              Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE + "='"
                  + Nickname.CONTENT_ITEM_TYPE + "' AND " + Nickname.TYPE
                  + "='" + Nickname.TYPE_INITIALS + "'",
              new String[] { String.valueOf(rawContactId) })
          .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
          .withValue(Nickname.NAME, initials).build());
    }
  }

  @SuppressWarnings("deprecation")
  private static void deleteDataForContact(Bundle b, BatchOperation batch,
      Uri dataUri, int rawcontactId) {
    for (String key : b.keySet()) {
      if (AttributeMapper.isRowAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + LDAPRow.CONTENT_ITEM_TYPE + "' AND " + LDAPRow.KEY + "=?",
            new String[] { String.valueOf(rawcontactId), key }, batch, dataUri);
      } else if (AttributeMapper.isNameSubAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + StructuredName.CONTENT_ITEM_TYPE + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (key.equalsIgnoreCase(AttributeMapper.INITIALS)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + Nickname.CONTENT_ITEM_TYPE + "' AND " + Nickname.TYPE
            + "='" + Nickname.TYPE_INITIALS + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (AttributeMapper.isEMailAttr(key)) {
        if (key.equalsIgnoreCase(AttributeMapper.PRIMARY_MAIL)) {
          deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
              + "='" + Email.CONTENT_ITEM_TYPE + "' AND " + Email.TYPE + "='"
              + Email.TYPE_WORK + "'",
              new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
        } else if (key.equalsIgnoreCase(AttributeMapper.ALTERNATE_MAIL)) {
          deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
              + "='" + Email.CONTENT_ITEM_TYPE + "' AND " + Email.TYPE + "='"
              + Email.TYPE_HOME + "'",
              new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
        }
      } else if (AttributeMapper.isDescriptionAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + Note.CONTENT_ITEM_TYPE + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (AttributeMapper.isPhoneNumberAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + Phone.CONTENT_ITEM_TYPE + "' AND " + Phone.NUMBER + "='"
            + b.getString(key) + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (AttributeMapper.isWebAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + Website.CONTENT_ITEM_TYPE + "' AND " + Website.URL + "='"
            + b.getString(key) + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (AttributeMapper.isOrganizationSubAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND " + Data.MIMETYPE
            + "='" + Organization.CONTENT_ITEM_TYPE + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (AttributeMapper.isPostalHomeAddressAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND "
            + StructuredPostal.MIMETYPE + "='"
            + StructuredPostal.CONTENT_ITEM_TYPE + "' AND "
            + StructuredPostal.TYPE + "='" + StructuredPostal.TYPE_HOME + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      } else if (AttributeMapper.isPostalWorkAddressAttr(key)) {
        deleteDataRow(Data.RAW_CONTACT_ID + "=?" + " AND "
            + StructuredPostal.MIMETYPE + "='"
            + StructuredPostal.CONTENT_ITEM_TYPE + "' AND "
            + StructuredPostal.TYPE + "='" + StructuredPostal.TYPE_WORK + "'",
            new String[] { String.valueOf(rawcontactId) }, batch, dataUri);
      }
    }
  }

  private static void deleteDataRow(String selection, String selectionArgs[],
      BatchOperation batch, Uri dataUri) {
    // DEBUG If the count is !=1 there is a fault in the routine
    batch.add(ContentProviderOperation.newDelete(dataUri).withExpectedCount(1)
        .withSelection(selection, selectionArgs).build());
  }

  /**
   * Updates the status of the given RawContact with the new values and marks
   * the contact as cleaned up
   * 
   * @param id
   * @param status
   * @param rawContactUri
   * @param ldif
   * @param uuid
   * @param dn
   * @return
   */
  static ContentProviderOperation updateLocallyAddedToSyncStatus(final int id,
      final String status, final Uri rawContactUri, final String ldif,
      final String uuid, final String dn) {
    return updateRawContactStatus(id, status, rawContactUri, ldif, "", uuid,
        false, dn);
  }

  /**
   * Updates the given RawContact with the new values.
   * 
   * @param rawContactId
   * @param status
   * @param rawContactUri
   * @param ldif
   * @param message
   * @param uuid
   * @param dirty
   * @param dn
   * @return
   */
  public static ContentProviderOperation updateRawContactStatus(
      final int rawContactId, final String status, final Uri rawContactUri,
      final String ldif, final String message, final String uuid,
      final boolean dirty, final String dn) {
    if (dirty) {
      return ContentProviderOperation
          .newUpdate(ContentUris.withAppendedId(rawContactUri, rawContactId))
          .withValue(RawContacts.SYNC1, status)
          .withValue(RawContacts.SYNC2, message)
          .withValue(RawContacts.SYNC3, dn).withValue(RawContacts.SYNC4, ldif)
          .withValue(RawContacts.SOURCE_ID, uuid)
          .withValue(RawContacts.DIRTY, "1").build();
    } else {
      return ContentProviderOperation
          .newUpdate(ContentUris.withAppendedId(rawContactUri, rawContactId))
          .withValue(RawContacts.SYNC1, status)
          .withValue(RawContacts.SYNC2, message)
          .withValue(RawContacts.SYNC3, dn).withValue(RawContacts.SYNC4, ldif)
          .withValue(RawContacts.SOURCE_ID, uuid)
          .withValue(RawContacts.DIRTY, "0").build();
    }
  }
}
