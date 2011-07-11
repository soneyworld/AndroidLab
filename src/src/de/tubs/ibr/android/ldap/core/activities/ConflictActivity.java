package de.tubs.ibr.android.ldap.core.activities;

import java.util.ArrayList;
import java.util.HashMap;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;

public class ConflictActivity extends ListActivity {

  final ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
  private SimpleAdapter adapter;

  private RadioButton rb1;
  private RadioButton rb2;
  private int rawContactId;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    final Intent i = getIntent();
    rawContactId = i.getIntExtra("id", -1);
    setContentView(R.layout.conflict_view);
    // TODO View has to be created
    Bundle localcontact = ContactManager.loadContact(rawContactId, this);
    this.setTitle("Conflict on synchronizing ("+rawContactId+") "+localcontact.getString(AttributeMapper.FULL_NAME));
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

  private Bundle loadLastSyncedContact() {
    // TODO Auto-generated method stub
    return null;
  }

  private Bundle loadRemoteContact() {
    // TODO Auto-generated method stub
    return null;
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
