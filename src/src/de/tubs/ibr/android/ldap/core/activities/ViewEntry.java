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

import java.util.LinkedList;
import java.util.StringTokenizer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.Entry;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.core.BatchOperation;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.provider.StringProvider;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import static com.unboundid.util.StaticUtils.*;
import static de.tubs.ibr.android.ldap.sync.AttributeMapper.*;

/**
 * This class provides an Android activity that may be used to view a search
 * result entry and handle the user clicking on various types of attributes.
 */
public final class ViewEntry extends Activity implements StringProvider,
    OnClickListener {
  /**
   * The name of the field used to define the instance to be searched.
   */
  public static final String BUNDLE_FIELD_ENTRY = "VIEW_ENTRY_ENTRY";

  // The entry to display.
  private Entry entry = null;

  private boolean accountfound;

  private Account account;

  private LinkedList<String> alreadyimported = new LinkedList<String>();

  /**
   * Performs all necessary processing when this activity is created.
   * 
   * @param state
   *          The state information for this activity.
   */
  @Override()
  protected void onCreate(final Bundle state) {
    super.onCreate(state);
    final Intent i = getIntent();
    final Bundle extras = i.getExtras();
    restoreState(extras);
  }

  /**
   * Performs all necessary processing when this activity is started or resumed.
   */
  @Override()
  protected void onResume() {
    super.onResume();

    setContentView(R.layout.layout_view_entry);
    setTitle(R.string.activity_label);

    if (hasContactAttributes(entry)) {
      displayUser();
    } else {
      displayGeneric();
    }
  }

  /**
   * Indicates whether the provided entry has any attributes which may be
   * considered contact attributes and as such should be displayed as a person
   * rather than a generic entry.
   * 
   * @param e
   *          The entry for which to make the determination.
   */
  private boolean hasContactAttributes(final Entry e) {
    // The entry must have a full name attribute to be considered a user.
    for (String string : entry.getAttributeValues("objectClass")) {
      if (string.equalsIgnoreCase("inetOrgPerson")) {
        return true;
      }
    }
    return false;
  }

  /**
   * Generates the display for a user entry.
   */
  private void displayUser() {
    final String name = entry.getAttributeValue(FULL_NAME);
    final String title = entry.getAttributeValue(TITLE);
    final String organization = entry.getAttributeValue(ORGANIZATION);

    setTitle("Entry for User " + name);

    AccountManager accManager = AccountManager.get(this);
    Account[] accounts = accManager
        .getAccountsByType(getString(R.string.ACCOUNT_TYPE));
    SharedPreferences mPrefs = PreferenceManager
        .getDefaultSharedPreferences(this);
    String accountname = mPrefs.getString("selectedAccount", "");
    accountfound = false;
    for (Account a : accounts) {
      if (a.name.equals(accountname)) {
        account = a;
        accountfound = true;
        break;
      }
    }

    for (Bundle contact : ContactManager.loadContactList(this).values()) {
      alreadyimported.add(contact.getString(AttributeMapper.ATTR_UID));
    }

    final LinearLayout layout = (LinearLayout) findViewById(R.id.layout_view_entry_panel);

    // Display the name, and optionally the title and/or organization at the top
    // of the pane.
    // final HeaderClickListener hdrListener =
    // new HeaderClickListener(this, entry);

    final LinearLayout headerLayout = new LinearLayout(this);
    headerLayout.setOrientation(LinearLayout.VERTICAL);
    headerLayout.setPadding(0, 5, 0, 5);

    final TextView nameView = new TextView(this);
    nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 24.0f);
    nameView.setText(name);
    nameView.setGravity(Gravity.CENTER);
    if ((title == null) && (organization == null)) {
      nameView.setPadding(0, 10, 0, 20);
    } else {
      nameView.setPadding(0, 10, 0, 0);
    }
    headerLayout.addView(nameView);

    if (title != null) {
      final TextView titleView = new TextView(this);
      titleView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f);
      titleView.setText(title);
      titleView.setGravity(Gravity.CENTER);
      if (organization == null) {
        titleView.setPadding(0, 0, 0, 20);
      } else {
        titleView.setPadding(0, 0, 0, 0);
      }
      headerLayout.addView(titleView);
    }

    if (organization != null) {
      final TextView companyView = new TextView(this);
      companyView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20.0f);
      companyView.setText(organization);
      companyView.setGravity(Gravity.CENTER);
      companyView.setPadding(0, 0, 0, 20);
      headerLayout.addView(companyView);
    }
    // headerLayout.setOnClickListener(hdrListener);
    layout.addView(headerLayout);

    // Display the phone numbers, if appropriate.

    boolean showAddToContacts = true;
    String newuuid = entry.getAttributeValue(AttributeMapper.ATTR_UID);
    for (String uuid : alreadyimported) {
      if (uuid.equalsIgnoreCase(newuuid)) {
        showAddToContacts = false;
      }
    }

    for (final String attrName : getPhoneNumberAttrs()) {
      final Attribute a = entry.getAttribute(attrName);
      if (a != null) {
        for (final String s : a.getValues()) {
          addPhoneNumber(s, getDisplayName(this, attrName), layout);
        }
      }
    }

    // Display the e-mail addresses, if appropriate.
    for (final String attrName : getEMailAttrs()) {
      final Attribute a = entry.getAttribute(attrName);
      if (a != null) {
        for (final String s : a.getValues()) {
          addEMailAddress(s, getDisplayName(this, attrName), layout);
        }
      }
    }

    // Display the postal addresses, if appropriate.
    for (final String attrName : getPostalAddressAttrs()) {
      final Attribute a = entry.getAttribute(attrName);
      if (a != null) {
        for (final String s : a.getValues()) {
          addPostalAddress(s, getDisplayName(this, attrName), layout);
        }
      }
    }

    // Display all remaining attributes.
    for (final Attribute a : entry.getAttributes()) {
      final String attrName = a.getName();
      if (a.hasValue() && isGenericAttr(attrName)) {
        addGenericAttribute(a, layout);
      }
    }

    // If we should provide an "Add to Contacts" button, then do so.
    if (showAddToContacts) {
      final LinearLayout l = new LinearLayout(this);
      l.setOrientation(LinearLayout.HORIZONTAL);
      l.setGravity(Gravity.CENTER);

      final Button addToContactsButton = new Button(this);
      addToContactsButton.setText("Import");
      addToContactsButton.setLayoutParams(new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT));
      addToContactsButton.setOnClickListener(this);
      l.addView(addToContactsButton);

      layout.addView(l);
    }

    boolean showAddAndSyncContact = showAddToContacts;
    // If we should provide an "Add to Contacts" button, then do so.
    if (showAddAndSyncContact) {
      final LinearLayout l = new LinearLayout(this);
      l.setOrientation(LinearLayout.HORIZONTAL);
      l.setGravity(Gravity.CENTER);

      final Button addToContactsButton = new Button(this);
      addToContactsButton.setText("Add and sync");
      addToContactsButton.setLayoutParams(new LinearLayout.LayoutParams(
          ViewGroup.LayoutParams.WRAP_CONTENT,
          ViewGroup.LayoutParams.WRAP_CONTENT));
      addToContactsButton.setOnClickListener(new OnClickListener() {
        @Override
        public void onClick(View v) {
          addContactToAccountAndSync(account);
        }
      });
      l.addView(addToContactsButton);
      layout.addView(l);
    }
  }

  /**
   * Generates the display for a generic entry.
   */
  private void displayGeneric() {
    setTitle("Entry " + entry.getDN());

    final LinearLayout layout = (LinearLayout) findViewById(R.id.layout_view_entry_panel);

    // Add the entry DN.
    // final HeaderClickListener hdrListener =
    // new HeaderClickListener(this, entry);

    final LinearLayout l = new LinearLayout(this);
    l.setPadding(0, 5, 0, 20);
    l.setOrientation(LinearLayout.VERTICAL);

    final TextView dnNameView = new TextView(this);
    dnNameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
    dnNameView.setText(getDisplayName(this, "DN"));
    dnNameView.setGravity(Gravity.LEFT);
    dnNameView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    l.addView(dnNameView);

    final TextView dnValueView = new TextView(this);
    dnValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
    dnValueView.setText(entry.getDN());
    dnValueView.setGravity(Gravity.RIGHT);
    dnValueView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    l.addView(dnValueView);

    // l.setOnClickListener(hdrListener);
    layout.addView(l);

    // Display the attributes .
    for (final Attribute a : entry.getAttributes()) {
      if (a.hasValue()) {
        addGenericAttribute(a, layout);
      }
    }
  }

  /**
   * Adds a phone number to the provided layout.
   * 
   * @param number
   *          The phone number to be added.
   * @param type
   *          The type of phone number to be added.
   * @param layout
   *          The layout to which the number should be added.
   */
  private void addPhoneNumber(final String number, final String type,
      final LinearLayout layout) {

    // final PhoneNumberClickListener onClickListener =
    // new PhoneNumberClickListener(this, number);

    final LinearLayout line = new LinearLayout(this);
    line.setOrientation(LinearLayout.VERTICAL);
    line.setPadding(0, 5, 0, 5);

    final TextView typeView = new TextView(this);
    typeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
    typeView.setText(type);
    typeView.setGravity(Gravity.LEFT);
    typeView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    line.addView(typeView);

    final TextView numberView = new TextView(this);
    numberView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
    numberView.setText(number);
    numberView.setGravity(Gravity.RIGHT);
    numberView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    line.addView(numberView);

    // line.setOnClickListener(onClickListener);
    layout.addView(line);
  }

  /**
   * Adds an e-mail address to the provided layout.
   * 
   * @param address
   *          The e-mail address to be added.
   * @param type
   *          The type of address to be added.
   * @param layout
   *          The layout to which the address should be added.
   */
  private void addEMailAddress(final String address, final String type,
      final LinearLayout layout) {
    // final EMailClickListener onClickListener =
    // new EMailClickListener(this, address);

    final LinearLayout line = new LinearLayout(this);
    line.setOrientation(LinearLayout.VERTICAL);
    line.setPadding(0, 5, 0, 5);

    final TextView typeView = new TextView(this);
    typeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
    typeView.setText(type);
    typeView.setGravity(Gravity.LEFT);
    typeView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    line.addView(typeView);

    final TextView addressView = new TextView(this);
    addressView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
    addressView.setText(address);
    addressView.setGravity(Gravity.RIGHT);
    addressView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    line.addView(addressView);

    // line.setOnClickListener(onClickListener);
    layout.addView(line);
  }

  /**
   * Adds a postal address to the provided layout.
   * 
   * @param address
   *          The postal address to be added.
   * @param type
   *          The type of address to be added.
   * @param layout
   *          The list to which the address should be added.
   */
  private void addPostalAddress(final String address, final String type,
      final LinearLayout layout) {

    final StringBuilder userFriendly = new StringBuilder();
    final StringBuilder mapFriendly = new StringBuilder();
    final StringTokenizer tokenizer = new StringTokenizer(address, "$");
    while (tokenizer.hasMoreTokens()) {
      final String token = tokenizer.nextToken().trim();
      userFriendly.append(token);
      mapFriendly.append(token);
      if (tokenizer.hasMoreTokens()) {
        userFriendly.append(EOL);
        mapFriendly.append(' ');
      }
    }

    // final MapClickListener onClickListener =
    // new MapClickListener(this, mapFriendly.toString());

    final LinearLayout line = new LinearLayout(this);
    line.setOrientation(LinearLayout.VERTICAL);
    line.setPadding(0, 5, 0, 5);

    final TextView typeView = new TextView(this);
    typeView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
    typeView.setText(type);
    typeView.setGravity(Gravity.LEFT);
    typeView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    line.addView(typeView);

    final TextView addressView = new TextView(this);
    addressView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
    addressView.setText(userFriendly.toString());
    addressView.setGravity(Gravity.RIGHT);
    addressView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    line.addView(addressView);

    // line.setOnClickListener(onClickListener);
    layout.addView(line);
  }

  /**
   * Adds information about a generic attribute to the provided layout.
   * 
   * @param attribute
   *          The attribute to be added.
   * @param layout
   *          The layout to which to add the attribute.
   */
  private void addGenericAttribute(final Attribute attribute,
      final LinearLayout layout) {
    final String[] values = attribute.getValues();
    final LinearLayout l = new LinearLayout(this);
    if (values.length == 1) {
      l.setPadding(0, 5, 0, 20);
    } else {
      l.setPadding(0, 5, 0, 5);
    }
    l.setOrientation(LinearLayout.VERTICAL);

    final TextView nameView = new TextView(this);
    nameView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.0f);
    nameView.setText(getDisplayName(this, attribute.getName()));
    nameView.setGravity(Gravity.LEFT);
    nameView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    l.addView(nameView);

    final TextView valueView = new TextView(this);
    valueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16.0f);
    valueView.setText(values[0]);
    valueView.setGravity(Gravity.RIGHT);
    valueView
        .setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.FILL_PARENT,
            ViewGroup.LayoutParams.FILL_PARENT));
    // valueView.setOnClickListener(
    // new GenericAttributeClickListener(this, values[0]));
    l.addView(valueView);

    layout.addView(l);

    for (int i = 1; i < values.length; i++) {
      final TextView additionalValueView = new TextView(this);
      additionalValueView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.0f);
      additionalValueView.setText(values[i]);
      additionalValueView.setGravity(Gravity.RIGHT);

      if (i == (values.length - 1)) {
        additionalValueView.setPadding(0, 5, 0, 20);
      } else {
        additionalValueView.setPadding(0, 5, 0, 5);
      }

      // additionalValueView.setOnClickListener(
      // new GenericAttributeClickListener(this, values[i]));
      layout.addView(additionalValueView);
    }
  }

  /**
   * Performs all necessary processing when the instance state needs to be
   * saved.
   * 
   * @param state
   *          The state information to be saved.
   */
  @Override()
  protected void onSaveInstanceState(final Bundle state) {

    saveState(state);
  }

  /**
   * Performs all necessary processing when the instance state needs to be
   * restored.
   * 
   * @param state
   *          The state information to be restored.
   */
  @Override()
  protected void onRestoreInstanceState(final Bundle state) {

    restoreState(state);
  }

  /**
   * Restores the state of this activity from the provided bundle.
   * 
   * @param state
   *          The bundle containing the state information.
   */
  private void restoreState(final Bundle state) {

    entry = (Entry) state.getSerializable(BUNDLE_FIELD_ENTRY);
  }

  /**
   * Saves the state of this activity to the provided bundle.
   * 
   * @param state
   *          The bundle containing the state information.
   */
  private void saveState(final Bundle state) {

    state.putSerializable(BUNDLE_FIELD_ENTRY, entry);
  }

  @Override
  public void onClick(View v) {

    if (accountfound && account != null) {
      addContactToAccount(account);
    }
  }

  /**
   * @param account
   */
  private void addContactToAccount(Account account) {
    BatchOperation batch = new BatchOperation(this, this.getContentResolver());
    ContactManager.importLDAPContact(entry, account, batch);
    try {
      batch.execute();
      finish();
    } catch (Exception e) {
      Toast.makeText(this, "Error on saving Contact", Toast.LENGTH_SHORT)
          .show();
    }
  }

  /**
   * @param account
   */
  private void addContactToAccountAndSync(Account account) {
    BatchOperation batch = new BatchOperation(this, this.getContentResolver());
    ContactManager.addLDAPContactToAccount(entry, account, batch);
    try {
      batch.execute();
      finish();
    } catch (Exception e) {
      Toast.makeText(this, "Error on saving Contact", Toast.LENGTH_SHORT)
          .show();
    }
  }
}
