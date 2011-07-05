package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.RawContacts;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

public class ContactViewerActivity extends Activity {

  String firstname;
  String lastname;
  String phonenumber;
  Object phonetype;
  String mailaddress;
  Object mailtype;
  String syncstatus;
  String src_id;
  private static final String ACTION_EDIT = "android.intent.action.EDIT";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.layout_editcontact_view);

    Spinner acctype = (Spinner) findViewById(R.id.accountSpinner);
    TextView acctextview = (TextView) findViewById(R.id.targetAccountTextView);
    acctype.setVisibility(Spinner.GONE);
    acctextview.setVisibility(TextView.GONE);

    final EditText mUserIdEditText = (EditText) findViewById(R.id.userIdEditText);
    final TextView mUserIdTextView = (TextView) findViewById(R.id.userIdTextView);
    Spinner addrtype = (Spinner) findViewById(R.id.contactPhoneTypeSpinner);
    Spinner phonetype = (Spinner) findViewById(R.id.contactEmailTypeSpinner);
    Button saveChanges = (Button) findViewById(R.id.contactSaveButton);
    Button exportContact = (Button) findViewById(R.id.contactExportButton);
    CheckBox syncCheckBox = (CheckBox) findViewById(R.id.syncCheckBox);

    syncCheckBox.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        // Perform action on clicks, depending on whether it's now checked
        if (((CheckBox) v).isChecked()) {
          mUserIdEditText.setEnabled(true);
          mUserIdTextView.setEnabled(true);
        } else {
          mUserIdEditText.setText("");
          mUserIdEditText.setEnabled(false);
          mUserIdTextView.setEnabled(false);
        }
      }
    });

    ArrayAdapter<CharSequence> adapteraddr = ArrayAdapter.createFromResource(
        this, R.array.emailTypeItems, android.R.layout.simple_spinner_item);
    adapteraddr
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    addrtype.setAdapter(adapteraddr);

    ArrayAdapter<CharSequence> adapterphone = ArrayAdapter.createFromResource(
        this, R.array.telefonnrTypeItems, android.R.layout.simple_spinner_item);
    adapterphone
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    phonetype.setAdapter(adapterphone);
    // TODO Fallunterscheidung einführen, ob diese Activity überhaupt zuständig
    // ist, für den Intent, ansonsten den default Intent werfen
    final long id;
    // Falls der Intent vom Android Kontaktbuch kam, dann ist die ID, die des
    // RawContacts
    if (getIntent().getAction().equalsIgnoreCase(ACTION_EDIT)) {
      id = Integer.parseInt(getIntent().getData().getLastPathSegment());
    } else {
      id = getIntent().getExtras().getLong("_id");
    }

    String[] projection = new String[] { Contacts._ID,
        Contacts.HAS_PHONE_NUMBER };
    Cursor cr = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection,
        Contacts._ID + "= ?", new String[] { String.valueOf(id) }, null);

    saveChanges.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        onSaveChangesButtonClicked(id);
      }
    });

    if (cr.moveToFirst()) {
      String number = null;
      int numbertype;
      String address = null;
      int addresstype;

      do {
        projection = new String[] { StructuredName.FAMILY_NAME,
            StructuredName.GIVEN_NAME };
        Cursor name = managedQuery(ContactsContract.Data.CONTENT_URI,
            projection, Data.MIMETYPE + "='" + StructuredName.CONTENT_ITEM_TYPE
                + "' AND " + StructuredName.CONTACT_ID + "= ?",
            new String[] { String.valueOf(id) }, null);

        if (name.moveToFirst()) {
          EditText vorname = (EditText) findViewById(R.id.contactFirstnameEditText);
          vorname.setText(name.getString(name
              .getColumnIndex(StructuredName.GIVEN_NAME)));
          EditText nachname = (EditText) findViewById(R.id.contactNameEditText);
          nachname.setText(name.getString(name
              .getColumnIndex(StructuredName.FAMILY_NAME)));
        }

        projection = new String[] { RawContacts.SOURCE_ID };

        Cursor source_id = managedQuery(
            ContactsContract.RawContacts.CONTENT_URI, projection,
            RawContacts._ID + "=" + id, null, null);
        if (source_id.moveToFirst()) {
          src_id = source_id.getString(source_id
              .getColumnIndex(RawContacts.SOURCE_ID));
          mUserIdEditText.setText(src_id);
          Log.v("ContactViewerActivity", "SOURCE_ID: " + src_id);
        }

        Log.v("ContactViewerActivity", "SOURCE_ID: " + src_id);

        projection = new String[] { RawContacts.SYNC1 };
        Cursor sync = managedQuery(ContactsContract.RawContacts.CONTENT_URI,
            projection, Data.CONTACT_ID + "= ?",
            new String[] { String.valueOf(id) }, null);
        if (sync.moveToFirst()) {
          syncstatus = sync.getString(0);
          Log.v("ContactViewerActivity", "Syncstatus: " + syncstatus);
          if (syncstatus != null
              && syncstatus.equalsIgnoreCase("locally added")) {
            syncCheckBox.setChecked(true);
            mUserIdEditText.setEnabled(true);
            mUserIdTextView.setEnabled(true);
            mUserIdEditText.setText(src_id);
          } else {
            syncCheckBox.setChecked(false);
            mUserIdEditText.setText("");
            mUserIdEditText.setEnabled(false);
            mUserIdTextView.setEnabled(false);
          }
        }

        projection = new String[] { Email.DATA1, Email.DATA2 };
        Cursor mail = managedQuery(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection,
            Email.CONTACT_ID + "= ?", new String[] { String.valueOf(id) }, null);
        if (mail.moveToFirst()) {
          address = mail.getString(mail.getColumnIndex(Email.DATA1));
          addresstype = mail.getInt(mail.getColumnIndex(Email.DATA2));
          EditText addr = (EditText) findViewById(R.id.contactEmailEditText);
          addr.setText(address);

          if (addresstype == Email.TYPE_HOME) {
            addrtype.setSelection(0);
          } else if (addresstype == Email.TYPE_WORK) {
            addrtype.setSelection(1);
          } else if (addresstype == Email.TYPE_MOBILE) {
            addrtype.setSelection(2);
          } else if (addresstype == Email.TYPE_OTHER) {
            addrtype.setSelection(3);
          } else
            addrtype.setSelection(3);

        }
        if (cr.getString(cr.getColumnIndex(Contacts.HAS_PHONE_NUMBER))
            .equalsIgnoreCase("1")) {
          projection = new String[] { Phone.NUMBER, Phone.DATA2 };

          Cursor phone = managedQuery(
              ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection,
              Phone.CONTACT_ID + "= ?", new String[] { String.valueOf(id) },
              null);
          phone.moveToFirst();
          number = phone.getString(phone.getColumnIndex(Phone.NUMBER));
          numbertype = phone.getInt(phone.getColumnIndex(Phone.DATA2));
          EditText nr = (EditText) findViewById(R.id.contactPhoneEditText);
          nr.setText(number);

          if (numbertype == Phone.TYPE_HOME) {
            phonetype.setSelection(0);
          } else if (numbertype == Phone.TYPE_WORK) {
            phonetype.setSelection(1);
          } else if (numbertype == Phone.TYPE_MOBILE) {
            phonetype.setSelection(2);
          } else if (numbertype == Phone.TYPE_OTHER) {
            phonetype.setSelection(3);
          } else
            phonetype.setSelection(3);
        }
      } while (cr.moveToNext());
    }

  }

  private void onSaveChangesButtonClicked(long id) {
    Log.v("TAG", "Save button clicked");

    Uri myPerson = ContentUris.withAppendedId(
        ContactsContract.Contacts.CONTENT_URI, id);

    ContentValues values = new ContentValues();

    EditText firstnameField = (EditText) findViewById(R.id.contactFirstnameEditText);
    EditText lastnameField = (EditText) findViewById(R.id.contactNameEditText);
    EditText phonenumberField = (EditText) findViewById(R.id.contactPhoneEditText);
    Spinner phonetypeField = (Spinner) findViewById(R.id.contactPhoneTypeSpinner);
    EditText mailaddressField = (EditText) findViewById(R.id.contactEmailEditText);
    Spinner mailtypeField = (Spinner) findViewById(R.id.contactEmailTypeSpinner);

    firstname = firstnameField.getText().toString();
    lastname = lastnameField.getText().toString();
    phonenumber = phonenumberField.getText().toString();
    phonetype = phonetypeField.getSelectedItem();
    mailaddress = mailaddressField.getText().toString();
    mailtype = mailtypeField.getSelectedItem();

    values.put(StructuredName.GIVEN_NAME, firstname);
    values.put(StructuredName.FAMILY_NAME, lastname);
    values.put(Phone.NUMBER, phonenumber);
    values.put(Phone.DATA2, (String) phonetype);
    values.put(Email.DATA1, mailaddress);
    values.put(Email.DATA2, (String) mailtype);

    getContentResolver().update(myPerson, values, "_ID =" + id, null);

    // TODO
  }
}
