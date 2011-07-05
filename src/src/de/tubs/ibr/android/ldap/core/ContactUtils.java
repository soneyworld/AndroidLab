package de.tubs.ibr.android.ldap.core;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.net.Uri;
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
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, cn)
        .withValue(StructuredName.GIVEN_NAME, givenName)
        .withValue(StructuredName.FAMILY_NAME, sn)
        .withValue(StructuredName.PREFIX, title).build());
    createInitials(initials, batch, dataUri);
  }

  static void createMail(String mail, BatchOperation batch, Uri dataUri) {
    if (mail != null && mail.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
          .withValue(Email.DATA, mail).withValue(Email.TYPE, Email.TYPE_WORK)
          .build());
    }
  }

  static void createRawContact(final Account account, String dn,
      String syncstatus, BatchOperation batch, Uri rawContactUri) {
    batch.add(ContentProviderOperation.newInsert(rawContactUri)
        .withValue(RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(RawContacts.ACCOUNT_NAME, account.name)
        .withValue(RawContacts.SYNC3, dn)
        .withValue(RawContacts.SYNC1, syncstatus).build());
  }

  static void createDescription(String description, BatchOperation batch,
      Uri dataUri) {
    if (description != null && description.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
          .withValue(Note.NOTE, description).build());
    }
  }

  static void createInitials(String initials, BatchOperation batch, Uri dataUri) {
    if (initials != null && initials.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
          .withValue(Nickname.NAME, initials)
          .withValue(Nickname.TYPE, Nickname.TYPE_INITIALS).build());
    }
  }

  static void createPhoneNumbers(String telephoneNumber, String homePhone,
      String mobileNumber, String faxNumber, String pagerNumber,
      String telexNumber, String isdnNumber, BatchOperation batch, Uri dataUri) {
    if (telephoneNumber != null && telephoneNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, telephoneNumber)
          .withValue(Phone.TYPE, Phone.TYPE_MAIN).build());
    }
    if (homePhone != null && homePhone.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, homePhone)
          .withValue(Phone.TYPE, Phone.TYPE_HOME).build());
    }
    if (mobileNumber != null && mobileNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, mobileNumber)
          .withValue(Phone.TYPE, Phone.TYPE_MOBILE).build());
    }
    if (faxNumber != null && faxNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, faxNumber)
          .withValue(Phone.TYPE, Phone.TYPE_FAX_WORK).build());
    }
    if (pagerNumber != null && pagerNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, pagerNumber)
          .withValue(Phone.TYPE, Phone.TYPE_PAGER).build());
    }
    if (telexNumber != null && telexNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, telexNumber)
          .withValue(Phone.TYPE, Phone.TYPE_TELEX).build());
    }
    if (isdnNumber != null && isdnNumber.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
          .withValue(Phone.NUMBER, isdnNumber)
          .withValue(Phone.TYPE, Phone.TYPE_ISDN).build());
    }
  }

  public static void createSeeAlso(String seeAlso, BatchOperation batch,
      Uri dataUri) {
    if (seeAlso != null && seeAlso.length() > 0) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
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
    if ((o != null && o.length() > 0) || (ou != null && ou.length() > 0)
        || (l != null && l.length() > 0)
        || (businessCategory != null && businessCategory.length() > 0)) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, Organization.CONTENT_ITEM_TYPE)
          .withValue(Organization.COMPANY, o)
          .withValue(Organization.TYPE, Organization.TYPE_WORK)
          .withValue(Organization.DEPARTMENT, ou)
          .withValue(Organization.OFFICE_LOCATION, l)
          .withValue(Organization.JOB_DESCRIPTION, businessCategory).build());
    }
  }

  public static void createAddresses(String destinationIndicator,
      String registeredAddress, String street, String preferredDeliveryMethod,
      String postOfficeBox, String postalCode, String postalAddress,
      String homePostalAddress, String st, BatchOperation batch, Uri dataUri) {
    // Create Home Address
    if ((homePostalAddress != null && homePostalAddress.length() > 0)
        || (street != null && street.length() > 0)) {
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
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
      // Adding Address
      batch.add(ContentProviderOperation.newInsert(dataUri)
          .withValueBackReference(Data.RAW_CONTACT_ID, 0)
          .withValue(Data.MIMETYPE, StructuredPostal.CONTENT_TYPE)
          .withValue(StructuredPostal.TYPE, StructuredPostal.TYPE_WORK)
          .withValue(StructuredPostal.FORMATTED_ADDRESS, postalAddress)
          .withValue(StructuredPostal.POSTCODE, postalCode)
          .withValue(StructuredPostal.POBOX, postOfficeBox)
          .withValue(StructuredPostal.REGION, st).build());
    }
  }
}
