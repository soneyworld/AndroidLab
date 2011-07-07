package de.tubs.ibr.android.ldap.core.activities;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.app.Activity;
import android.app.ListActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
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
    LinkedHashMap<Integer, Bundle> contactlist = ContactManager
        .loadContactList(this);
    for (Entry<Integer, Bundle> contact : contactlist.entrySet()) {
      String status = contact.getValue().getString(
          ContactManager.LDAP_SYNC_STATUS_KEY);
      if (status != null && status.length() > 1)
        entries.add(new EntityEntry(contact.getValue().getString(
            AttributeMapper.FULL_NAME), contact.getKey()));
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
    if (resultCode == Activity.RESULT_OK) {
      Intent i = new Intent(Intent.ACTION_CALL, Uri.parse("tel:"
          + data.getExtras().getString("phone")));
      i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      startActivity(i);

    }
  }
}
