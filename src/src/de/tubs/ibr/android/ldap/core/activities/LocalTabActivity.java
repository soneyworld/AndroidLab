package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import android.app.Activity;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;

public class LocalTabActivity extends Activity {
  private ListView mContactList;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.localtab);

    mContactList = (ListView) findViewById(R.id.contactList);

    populateContactList();

  }

  private void populateContactList() {
    // Build adapter with contact entries
    Cursor cursor = getContacts();
    String[] fields = new String[] { ContactsContract.Data.DISPLAY_NAME };
    SimpleCursorAdapter adapter = new SimpleCursorAdapter(this,
        R.layout.contact_entry, cursor, fields,
        new int[] { R.id.contactEntryText });
    mContactList.setAdapter(adapter);
  }

  private Cursor getContacts() {
    // Run query
    Uri uri = ContactsContract.Contacts.CONTENT_URI;
    String[] projection = new String[] { ContactsContract.Contacts._ID,
        ContactsContract.Contacts.DISPLAY_NAME };
    String selection = ContactsContract.Contacts.DISPLAY_NAME;
    String[] selectionArgs = null;
    String sortOrder = ContactsContract.Contacts.DISPLAY_NAME;

    return managedQuery(uri, projection, selection, selectionArgs, sortOrder);
  }
}
