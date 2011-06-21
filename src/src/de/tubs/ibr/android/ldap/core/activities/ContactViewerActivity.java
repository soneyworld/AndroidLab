package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

public class ContactViewerActivity extends Activity {

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater mi = new MenuInflater(this);
    mi.inflate(R.menu.contactviewmenu, menu);
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.OK:
        Intent i = new Intent();
        i.putExtra("phone",
            ((EditText) findViewById(R.id.telefonnrLocal)).getText());
        setResult(Activity.RESULT_OK, i);
        finish();
        break;
      case R.id.BACK:
        setResult(Activity.RESULT_CANCELED);
        finish();
        break;
      default:
        break;
    }
    return super.onOptionsItemSelected(item);
  }

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.contactviewerlocal);

    Spinner addrtype = (Spinner) findViewById(R.id.telefonnrLocalTypeSpinner);
    Spinner phonetype = (Spinner) findViewById(R.id.emailLocalTypeSpinner);
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

    long id = getIntent().getExtras().getLong("_id");
    String[] projection = new String[] { Contacts._ID,
        Contacts.HAS_PHONE_NUMBER };
    Cursor cr = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection,
        Contacts._ID + "= ?", new String[] { String.valueOf(id) }, null);
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
          EditText vorname = (EditText) findViewById(R.id.firstnameLocal);
          vorname.setText(name.getString(name
              .getColumnIndex(StructuredName.GIVEN_NAME)));
          EditText nachname = (EditText) findViewById(R.id.lastnameLocal);
          nachname.setText(name.getString(name
              .getColumnIndex(StructuredName.FAMILY_NAME)));
        }

        projection = new String[] { Email.DATA1, Email.DATA2 };
        Cursor mail = managedQuery(
            ContactsContract.CommonDataKinds.Email.CONTENT_URI, projection,
            Email.CONTACT_ID + "= ?", new String[] { String.valueOf(id) }, null);
        if (mail.moveToFirst()) {
          address = mail.getString(mail.getColumnIndex(Email.DATA1));
          addresstype = mail.getInt(mail.getColumnIndex(Email.DATA2));
          EditText addr = (EditText) findViewById(R.id.emailLocal);
          addr.setText(address);
          
          if (addresstype == 1) {
            addrtype.setSelection(0);
          }
          else if (addresstype == 2) {
            addrtype.setSelection(1);
          }
          else if (addresstype == 4) {
            addrtype.setSelection(2);
          }
          else {
            addrtype.setSelection(3);
          }

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
          EditText nr = (EditText) findViewById(R.id.telefonnrLocal);
          nr.setText(number);
          
          if (numbertype == 1) {
            phonetype.setSelection(0);
          }
          else if (numbertype == 3) {
            phonetype.setSelection(1);
          }
          else if (numbertype == 2) {
            phonetype.setSelection(2);
          }
          else {
            phonetype.setSelection(3);
          }
        }
      } while (cr.moveToNext());
    }

  }
}
