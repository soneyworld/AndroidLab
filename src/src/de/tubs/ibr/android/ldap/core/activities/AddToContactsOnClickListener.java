/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2010 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package de.tubs.ibr.android.ldap.core.activities;

import java.util.ArrayList;
import java.util.StringTokenizer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Application;
import android.content.ContentProviderOperation;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.view.View;
import android.view.View.OnClickListener;
import com.unboundid.ldap.sdk.Entry;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import static com.unboundid.util.StaticUtils.*;

/**
 * This class provides an on-click listener that is meant to add a user to the
 * phone's address book when the associated view is clicked.
 */
final class AddToContactsOnClickListener extends Application implements
    OnClickListener {
  // The information about the person to add.
  private final String fax;
  private final String homeAddress;
  private final String homeEMail;
  private final String homePhone;
  private final String mobile;
  private final String name;
  private final String pager;
  private final String workAddress;
  private final String workEMail;
  private final String workPhone;
  private final AccountManager accManager;

  /**
   * Creates a new phone number on-click listener that will dial the provided
   * telephone number when the associated view is clicked.
   * 
   * @param activity
   *          The activity that created this on-click listener.
   * @param entry
   *          The entry for the user to add.
   */
  AddToContactsOnClickListener(final Entry entry, AccountManager accManager) {
    this.accManager = accManager;
    name = entry.getAttributeValue(AttributeMapper.ATTR_FULL_NAME);
    workPhone = entry.getAttributeValue(AttributeMapper.ATTR_PRIMARY_PHONE);
    homePhone = entry.getAttributeValue(AttributeMapper.ATTR_HOME_PHONE);
    mobile = entry.getAttributeValue(AttributeMapper.ATTR_MOBILE_PHONE);
    pager = entry.getAttributeValue(AttributeMapper.ATTR_PAGER);
    fax = entry.getAttributeValue(AttributeMapper.ATTR_FAX);
    workEMail = entry.getAttributeValue(AttributeMapper.ATTR_PRIMARY_MAIL);
    homeEMail = entry.getAttributeValue(AttributeMapper.ATTR_ALTERNATE_MAIL);
    workAddress = entry.getAttributeValue(AttributeMapper.ATTR_PRIMARY_ADDRESS);
    homeAddress = entry.getAttributeValue(AttributeMapper.ATTR_HOME_ADDRESS);
  }

  /**
   * Indicates that the associated view was clicked and that the associated
   * entry should be added to the contacts.
   * 
   * @param view
   *          The view that was clicked.
   */
  public void onClick(final View view) {
    Account[] accounts = accManager.getAccountsByType(view.getContext()
        .getString(R.string.ACCOUNT_TYPE));
    SharedPreferences mPrefs = PreferenceManager
        .getDefaultSharedPreferences(view.getContext());
    String accountname = mPrefs.getString("selectedAccount", "");
    boolean accountfound = false;
    for (Account a : accounts) {
      if (a.name.equals(accountname)) {
        addContactToAccount(a);
        accountfound = true;
        break;
      }
    }
    if (!accountfound && accounts.length > 0) {
      addContactToAccount(accounts[0]);
    }
  }

  /**
   * @param account
   */
  private void addContactToAccount(Account account) {
    // Prepare contact creation request
    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    ops.add(ContentProviderOperation
        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, account.type)
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, account.name)
        .build());
    ops.add(ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
        .build());
    if (workPhone != null) {
      ops.add(addPhoneNumber(workPhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_WORK));
    }
    if (homePhone != null) {
      ops.add(addPhoneNumber(homePhone,
          ContactsContract.CommonDataKinds.Phone.TYPE_HOME));
    }
    if (mobile != null) {
      ops.add(addPhoneNumber(mobile,
          ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE));
    }
    if (pager != null) {
      ops.add(addPhoneNumber(pager,
          ContactsContract.CommonDataKinds.Phone.TYPE_PAGER));
    }
    if (fax != null) {
      ops.add(addPhoneNumber(fax,
          ContactsContract.CommonDataKinds.Phone.TYPE_FAX_WORK));
    }
    if (workEMail != null) {
      ops.add(addEMailAddress(workEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    }
    if (homeEMail != null) {
      ops.add(addEMailAddress(homeEMail,
          ContactsContract.CommonDataKinds.Email.TYPE_HOME));
    }
    if (workAddress != null) {
      ops.add(addPostalAddress(workAddress,
          ContactsContract.CommonDataKinds.Email.TYPE_WORK));
    }
    if (homeAddress != null) {
      ops.add(addPostalAddress(homeAddress,
          ContactsContract.CommonDataKinds.StructuredPostal.TYPE_HOME));
    }
    // Ask the Contact provider to create a new contact
    try {
      getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
    } catch (Exception e) {
      e.printStackTrace();
    }
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
  private ContentProviderOperation addPhoneNumber(final String number,
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
  private ContentProviderOperation addEMailAddress(final String address,
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
  private ContentProviderOperation addPostalAddress(final String address,
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
