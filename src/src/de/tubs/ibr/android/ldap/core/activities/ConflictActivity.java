package de.tubs.ibr.android.ldap.core.activities;

import java.util.ArrayList;
import java.util.HashMap;
import de.tubs.ibr.android.ldap.R;
import android.app.ListActivity;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RadioButton;
import android.widget.SimpleAdapter;

public class ConflictActivity extends ListActivity {

  static final ArrayList<HashMap<String, String>> list = new ArrayList<HashMap<String, String>>();
  private SimpleAdapter adapter;
  
  private RadioButton rb1;
  private RadioButton rb2;

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.conflict_view);
    adapter = new SimpleAdapter(this, list, R.layout.conflict_row,
        new String[] { "Value1", "Value2" }, new int[] { R.id.value1,
            R.id.value2 }

    );

    setListAdapter(adapter);

    addItem();

    rb1 = (RadioButton) findViewById(R.id.selector1);
    rb2 = (RadioButton) findViewById(R.id.selector2);


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
