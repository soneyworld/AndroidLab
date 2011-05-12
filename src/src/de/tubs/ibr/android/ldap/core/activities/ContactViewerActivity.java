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
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TextView;

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
      i.putExtra("phone", ((TextView)findViewById(R.id.telefonnr)).getText());
      setResult(Activity.RESULT_OK,i);
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
    setContentView(R.layout.contactviewer);
    long id = getIntent().getExtras().getLong("_id");
    String[] projection = new String[] {Contacts._ID,Contacts.HAS_PHONE_NUMBER};
        Cursor cr = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, Contacts._ID + "= ?", new String[]{String.valueOf(id)}, null);
        if (cr.moveToFirst()) {
          String number = null;
          do {
            projection = new String[] {
                StructuredName.FAMILY_NAME,
                StructuredName.GIVEN_NAME
            };
            Cursor name = managedQuery(ContactsContract.Data.CONTENT_URI, projection, 
                Data.MIMETYPE+"='"+StructuredName.CONTENT_ITEM_TYPE+"' AND "+StructuredName.CONTACT_ID+"= ?", 
                new String[]{String.valueOf(id)},null);
                
            if (name.moveToFirst()) {
              TextView vorname = (TextView) findViewById(R.id.vorname);
              vorname.setText(name.getString(name.getColumnIndex(StructuredName.GIVEN_NAME)));
              TextView nachname = (TextView) findViewById(R.id.nachname);
              nachname.setText(name.getString(name.getColumnIndex(StructuredName.FAMILY_NAME)));
            }
            if (cr.getString(cr.getColumnIndex(Contacts.HAS_PHONE_NUMBER)).equalsIgnoreCase("1")) {
              projection = new String[] {
                  Phone.NUMBER
              };
              
              Cursor phone = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, Phone.CONTACT_ID+"= ?",new String[]{
                  String.valueOf(id)}
                , null);
              phone.moveToFirst();
              number = phone.getString(phone.getColumnIndex(Phone.NUMBER));
              TextView nr = (TextView) findViewById(R.id.telefonnr);
              nr.setText(number);
            }
          } while (cr.moveToNext());
        }
  
  }
}
