package de.tubs.ibr.android.ldap.core.activities;

import java.util.LinkedList;
import de.tubs.ibr.android.ldap.R;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.RawContactsEntity;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.AdapterView.OnItemClickListener;

public class SyncTabActivity extends ListActivity {
  private ArrayAdapter<EntityEntry> adapter;

  private class EntityEntry {
    private final String displayname;
    private final int id;

    public EntityEntry(String displayname, int id) {
      this.displayname = displayname;
      this.id = id;
    }

    public int getId() {
      return this.id;
    }

    @Override
    public String toString() {
      return displayname;
    }
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.localtab);
  }

  @Override
  protected void onResume() {
    super.onResume();

    ListView contacts = (ListView) findViewById(android.R.id.list);
    LinkedList<EntityEntry> entries = new LinkedList<SyncTabActivity.EntityEntry>();
    Uri entityUri = RawContactsEntity.CONTENT_URI;
    Cursor c = getContentResolver().query(
        entityUri,
        new String[] { RawContactsEntity._ID, RawContactsEntity.MIMETYPE,
            RawContactsEntity.DATA1, }, null, null, null);
    try {
      while (c.moveToNext()) {
        int id = c.getInt(0);
        String syncstatus = c.getString(3);
        String displayname;
        if (!c.isNull(1)
            && c.getString(1)
                .equalsIgnoreCase(StructuredName.CONTENT_ITEM_TYPE)) {
          displayname = c.getString(2);
          if (!c.isNull(3) && ((syncstatus.equalsIgnoreCase("locally added")) || (syncstatus.equalsIgnoreCase("change conflict")) || (syncstatus.equalsIgnoreCase("in sync")))) {
            entries.add(new EntityEntry(displayname, id));
          }
        }
      }
    } finally {
      c.close();
    }
    adapter = new ArrayAdapter<EntityEntry>(this,
        android.R.layout.simple_list_item_1, entries);
    contacts.setAdapter(adapter);
    contacts.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int row, long id) {
        Intent i = new Intent(getBaseContext(), EditContactActivity.class);
        i.setAction(Intent.ACTION_EDIT);
        i.putExtra("_id", ((EntityEntry) arg0.getItemAtPosition(row)).getId());
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
