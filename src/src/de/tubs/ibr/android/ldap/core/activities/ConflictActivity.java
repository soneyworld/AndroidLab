package de.tubs.ibr.android.ldap.core.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Map.Entry;
import java.util.Set;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.app.ListActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
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
          listitem.put("Value2", "" + contact.getKey());
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

  private Bundle loadLastSyncedContact(Bundle loadedContact) {
    String ldif = loadedContact.getString(ContactManager.LDAP_LDIF_DETAILS_KEY);
    if (ldif == null) {
      return null;
    }
    LDIFReader reader = new LDIFReader(new BufferedReader(
        new StringReader(ldif)));
    com.unboundid.ldap.sdk.Entry lastSyncState = null;
    try {
      lastSyncState = reader.readEntry();
    } catch (LDIFException e) {
      return null;
    } catch (IOException e) {
      return null;
    }
    return ContactManager.createMapableBundle(ContactManager
        .createBundleFromEntry(lastSyncState));
  }

  private Bundle loadRemoteContact(String attributeUUID) {
    SharedPreferences savedRemoteStates = getSharedPreferences("remoteState",
        MODE_PRIVATE);
    String ldif = savedRemoteStates.getString(attributeUUID, null);
    if (ldif == null) {
      return null;
    }
    LDIFReader reader = new LDIFReader(new BufferedReader(
        new StringReader(ldif)));
    com.unboundid.ldap.sdk.Entry lastSyncState = null;
    try {
      lastSyncState = reader.readEntry();
    } catch (LDIFException e) {
      return null;
    } catch (IOException e) {
      return null;
    }
    return ContactManager.createMapableBundle(ContactManager
        .createBundleFromEntry(lastSyncState));
  }

  private void showContact(int rawcontactid) {
    setContentView(R.layout.conflict_view);
    Bundle localcontact = ContactManager.loadContact(rawcontactid, this);
    Bundle remotecontact = loadRemoteContact(localcontact
        .getString(AttributeMapper.ATTR_UID));
    Bundle lastSyncedContact = loadLastSyncedContact(localcontact);
    localcontact = ContactManager.createMapableBundle(localcontact);
    this.setTitle("Conflict of "
        + localcontact.getString(AttributeMapper.FULL_NAME));
    Set<String> conflictKeys = new LinkedHashSet<String>();
    Bundle mergeresult = new Bundle();
    ContactManager.mergeBundle(localcontact, remotecontact, lastSyncedContact,
        mergeresult, conflictKeys);
    adapter = new SimpleAdapter(this, list, R.layout.conflict_row,
        new String[] { "Value1", "Value2", "Key" }, new int[] { R.id.value1,
            R.id.value2, R.id.key });

    setListAdapter(adapter);
    for (String key : conflictKeys) {
      addConflictItem(key, localcontact.getString(key),
          remotecontact.getString(key));
    }
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
        try {

          Intent i = new Intent(getBaseContext(), ConflictActivity.class);
          HashMap<String, String> listitem = (HashMap<String, String>) arg0
              .getItemAtPosition(row);
          if (listitem != null) {
            int rawContactId = Integer.parseInt(listitem.get("Value2"));
            i.putExtra("id", rawContactId);
            startActivityForResult(i, 0);
          }
        } catch (Exception e) {

        }
      }
    });

  }

  private void addConflictItem(String key, String local, String remote) {
    HashMap<String, String> item = new HashMap<String, String>();
    item.put("Value1", local);
    item.put("Value2", remote);
    item.put("Key", key);
    list.add(item);
    adapter.notifyDataSetChanged();
  }

}
