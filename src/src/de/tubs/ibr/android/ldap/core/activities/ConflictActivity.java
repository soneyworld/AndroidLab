package de.tubs.ibr.android.ldap.core.activities;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map.Entry;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.app.ListActivity;
import android.content.ContentUris;
import android.content.Intent;
import android.os.Bundle;
import android.provider.ContactsContract.RawContacts;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;
import android.widget.AdapterView.OnItemClickListener;

public class ConflictActivity extends ListActivity {
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

  public static final int CONFLICT_LIST = 0;
  final ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
  final ArrayList<HashMap<String, String>> entryList = new ArrayList<HashMap<String, String>>();
  private SimpleAdapter adapter;
  private LinkedList<EntityEntry> entries = new LinkedList<EntityEntry>();

  private RadioButton rb1;
  private RadioButton rb2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Intent i = getIntent();
    int rawContactId = i.getIntExtra("id", -1);
    if (rawContactId != -1) {
      showContact(rawContactId);
    } else {
      LinkedHashMap<Integer, Bundle> contactlist = ContactManager
          .loadContactList(this);
      for (Entry<Integer, Bundle> contact : contactlist.entrySet()) {
        if (contact.getValue().getString(ContactManager.LDAP_SYNC_STATUS_KEY)
            .startsWith(ContactManager.SYNC_STATUS_CONFLICT)) {
          entries.add(new EntityEntry(contact.getValue().getString(
              AttributeMapper.FULL_NAME), contact.getKey()));
          HashMap<String, String> listitem = new HashMap<String, String>();
          listitem.put("Value1",
              contact.getValue().getString(AttributeMapper.FULL_NAME));
          entryList.add(listitem);
        }
      }
      if (entries.size() == 1) {
        showContact(entries.getFirst().id);
      } else if (entries.size() > 1) {
        showList();
      }
    }
  }

  private Bundle loadLastSyncedContact() {
    // TODO Auto-generated method stub
    return null;
  }

  private Bundle loadRemoteContact() {
    // TODO Auto-generated method stub
    return null;
  }

  private void showContact(int rawcontactid) {
    setContentView(R.layout.conflict_view);
    Bundle localcontact = ContactManager.loadContact(rawcontactid, this);
    this.setTitle("Conflict of "
        + localcontact.getString(AttributeMapper.FULL_NAME));

    Bundle remotecontact = loadRemoteContact();
    Bundle lastSyncedContact = loadLastSyncedContact();
    adapter = new SimpleAdapter(this, list, R.layout.conflict_row,
        new String[] { "Value1", "Value2" }, new int[] { R.id.value1,
            R.id.value2 }

    );

    setListAdapter(adapter);

    addItem();

    rb1 = (RadioButton) findViewById(R.id.selector1);
    rb2 = (RadioButton) findViewById(R.id.selector2);

  }

  private void showList() {
    setContentView(R.layout.layout_conflict_list);
    ListView contacts = (ListView) findViewById(android.R.id.list);
    adapter = new SimpleAdapter(this, entryList, R.layout.list_conflict_row,
        new String[] { "Value1" }, new int[] { R.id.value1 });
    contacts.setAdapter(adapter);
    contacts.setOnItemClickListener(new OnItemClickListener() {

      @Override
      public void onItemClick(AdapterView<?> arg0, View arg1, int row, long id) {
        Intent i = new Intent(getBaseContext(), ConflictActivity.class);
        i.getExtras().putInt("id",((EntityEntry) arg0.getItemAtPosition(row)).getId());
        startActivityForResult(i, 0);
      }
    });

  }

  private void addItem() {
    long ts = System.currentTimeMillis();
    int lastDigit = (int) (ts % 10);
    HashMap<String, String> item = new HashMap<String, String>();
    item.put("Value1", Long.toString(ts));
    item.put("Value2", "lastDigit: " + Integer.toString(lastDigit));
    list.add(item);
    adapter.notifyDataSetChanged();
  }

}
