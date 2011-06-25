package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.core.activities.ContactViewerActivity;
import de.tubs.ibr.android.ldap.R;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.provider.ContactsContract.Contacts;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class SyncTabActivity extends ListActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.synctab);
  }
  @Override
  protected void onResume() {
    super.onResume();
    ListView contacts = (ListView) findViewById(android.R.id.list);
    String[] projection = new String[] {Contacts.DISPLAY_NAME, Contacts._ID};
      Cursor cr = getContentResolver().query(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
      startManagingCursor(cr);
      int id [] = new int[] {android.R.id.text1};
    contacts.setAdapter(new SimpleCursorAdapter(this,android.R.layout.simple_list_item_1,cr,projection,id));
    contacts.setOnItemClickListener(new OnItemClickListener() {

    @Override
    public void onItemClick(AdapterView<?> arg0, View arg1, int row,
        long id) {
      Intent i = new Intent(getBaseContext(),ContactViewerActivity.class);
      i.putExtra("_id", id);
      startActivityForResult(i, 0);
    }
  });
  }
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    super.onActivityResult(requestCode, resultCode, data);
    if (resultCode==Activity.RESULT_OK) {
      Intent i = new Intent(Intent.ACTION_CALL,Uri.parse("tel:"+data.getExtras().getString("phone")));
      i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(i);
      
    }
  }
}
