package de.tubs.ibr.android.ldap.core;

import android.accounts.Account;
import android.content.ContentProviderOperation;
import android.net.Uri;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Nickname;
import android.provider.ContactsContract.CommonDataKinds.Note;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;

public class ContactUtils {

  static void createStructuredName(String sn, String cn, String title,
      String givenName, BatchOperation batch, Uri dataUri) {
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, StructuredName.CONTENT_ITEM_TYPE)
        .withValue(StructuredName.DISPLAY_NAME, cn)
        .withValue(StructuredName.GIVEN_NAME, givenName)
        .withValue(StructuredName.FAMILY_NAME, sn)
        .withValue(StructuredName.PREFIX, title).build());
  }

  static void createMail(String mail, BatchOperation batch, Uri dataUri) {
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, Email.CONTENT_ITEM_TYPE)
        .withValue(Email.DATA, mail).withValue(Email.TYPE, Email.TYPE_WORK)
        .build());
  }

  static void createRawContact(final Account account, String dn,
      String syncstatus, BatchOperation batch, Uri rawContactUri) {
    batch.add(ContentProviderOperation.newInsert(rawContactUri)
        .withValue(RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(RawContacts.ACCOUNT_NAME, account.name)
        .withValue(RawContacts.SYNC3, dn)
        .withValue(RawContacts.SYNC1, syncstatus).build());
  }

  static void createDescription(String description,
      BatchOperation batch, Uri dataUri) {
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, Note.CONTENT_ITEM_TYPE)
        .withValue(Note.NOTE, description).build());
  }

  static void createInitials(String initials, BatchOperation batch,
      Uri dataUri) {
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, Nickname.CONTENT_ITEM_TYPE)
        .withValue(Nickname.NAME, initials)
        .withValue(Nickname.TYPE, Nickname.TYPE_INITIALS).build());
  }

  static void createPhone(String telephoneNumber, BatchOperation batch,
      Uri dataUri) {
    batch.add(ContentProviderOperation.newInsert(dataUri)
        .withValueBackReference(Data.RAW_CONTACT_ID, 0)
        .withValue(Data.MIMETYPE, Phone.CONTENT_ITEM_TYPE)
        .withValue(Phone.NUMBER, telephoneNumber)
        .withValue(Phone.TYPE, Phone.TYPE_MAIN).build());
  }

}
