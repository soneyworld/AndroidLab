package de.tubs.ibr.android.ldap.core.activities;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.PopUp;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import de.tubs.ibr.android.ldap.provider.*;

public class LDAPTabActivity extends ListActivity implements OnClickListener,
    OnItemClickListener, OnItemLongClickListener, SearchServerInterface {
  // A map of the entry strings to their corresponding names.
  private HashMap<String, SearchResultEntry> entryMap;

  // The list of entries to process.
  private LinkedList<SearchResultEntry> entries;

  // The server instance to search.
  private ServerInstance instance;

  // The search text.
  private String searchText = "";

  // The progress dialog displayed while the search is in progress.
  private volatile ProgressDialog progressDialog = null;

  private ArrayAdapter<String> adapter;

  /**
   * Creates a new instance of this class.
   */
  public LDAPTabActivity() {
    entryMap = new HashMap<String, SearchResultEntry>(0);
    entries = new LinkedList<SearchResultEntry>();
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
  }

  /**
   * Performs all necessary processing when this activity is started or resumed.
   */
  @Override()
  protected void onResume() {
    super.onResume();
    instance = new ServerInstance(this.getIntent().getExtras());
    setContentView(R.layout.ldaptab);
    // Add an on-click listener to the search button.
    final Button searchButton = (Button) findViewById(R.id.layout_search_server_button_search);
    searchButton.setOnClickListener(this);
    setTitle(getString(R.string.list_search_results_activity_title,
        entries.size()));
    entryMap = new HashMap<String, SearchResultEntry>(entries.size());

    final StringBuilder buffer = new StringBuilder();
    final String[] entryStrings = new String[entries.size()];
    for (int i = 0; i < entryStrings.length; i++) {
      final SearchResultEntry e = entries.get(i);
      if (e.hasObjectClass("person")) {
        final String name = e.getAttributeValue("cn");
        if (name == null) {
          entryStrings[i] = e.getDN();
        } else {
          buffer.setLength(0);
          buffer.append(name);

          final String phone = e.getAttributeValue("telephoneNumber");
          if (phone != null) {
            buffer.append(EOL);
            buffer.append(phone);
          }

          final String mail = e.getAttributeValue("mail");
          if (mail != null) {
            buffer.append(EOL);
            buffer.append(mail);
          }

          entryStrings[i] = buffer.toString();
        }
      } else {
        entryStrings[i] = e.getDN();
      }
      entryMap.put(entryStrings[i], e);
    }

    Arrays.sort(entryStrings);

    adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_list_item_1, entryStrings);
    setListAdapter(adapter);
    getListView().setTextFilterEnabled(true);

    getListView().setOnItemClickListener(this);
    getListView().setOnItemLongClickListener(this);
    Filter filter;

    try {
      filter = Filter.create("(cn=*)");
    } catch (LDAPException le2) {
      Toast.makeText(this, R.string.search_server_popup_text_invalid_filter,
          Toast.LENGTH_LONG).show();
      return;
    }

    // Create a thread to process the search.
    final SearchThread searchThread = new SearchThread(this, instance, filter);
    searchThread.start();
  }

  @Override
  public boolean onItemLongClick(AdapterView<?> arg0, View arg1, int arg2,
      long arg3) {
    // TODO Auto-generated method stub
    return false;
  }

  /**
   * Takes any appropriate action after a list item has been clicked.
   * 
   * @param parent
   *          The list in which the item was clicked.
   * @param view
   *          The list item that was clicked.
   * @param position
   *          The position of the item in the list that was clicked.
   * @param id
   *          The ID of the item that was clicked.
   */
  public void onItemClick(final AdapterView<?> parent, final View view,
      final int position, final long id) {

    final TextView item = (TextView) view;
    final SearchResultEntry e = entryMap.get(item.getText().toString());
    if (e != null) {
      final Intent i = new Intent(this, ViewEntry.class);
      i.putExtra(ViewEntry.BUNDLE_FIELD_ENTRY, e);
      startActivity(i);
    }
  }

  @Override
  public void onClick(View v) {
    int id = v.getId();
    switch (id) {
      case R.id.layout_search_server_button_search:
        final EditText textField = (EditText) findViewById(R.id.layout_search_server_field_search_text);
        searchText = textField.getText().toString();
        if (searchText.length() == 0) {
          break;
        }

        // Determine the type of search to perform. and build the filter from
        // it.

        // Make sure that the filter is valid.
        Filter filter;
        try {
          filter = Filter.create(searchText);
        } catch (LDAPException le) {
          try {
            filter = Filter.create("(cn=" + searchText + ')');
          } catch (LDAPException le2) {
            Toast.makeText(this,
                R.string.search_server_popup_text_invalid_filter,
                Toast.LENGTH_SHORT).show();
            break;
          }
        }
        // Create a thread to process the search.
        final SearchThread searchThread = new SearchThread(this, instance,
            filter);
        searchThread.start();

        // Create a progress dialog to display while the search is in progress.
        progressDialog = new ProgressDialog(this);
        progressDialog
            .setTitle(getString(R.string.search_server_progress_searching));
        progressDialog.setIndeterminate(true);
        progressDialog.setCancelable(true);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        progressDialog.show();
        break;
      default:
        break;
    }
  }

  @Override
  public void searchDone(SearchResult result) {
    Looper.prepare();
    final ResultCode resultCode = result.getResultCode();
    if (resultCode != ResultCode.SUCCESS) {
      switch (resultCode.intValue()) {
        case ResultCode.SIZE_LIMIT_EXCEEDED_INT_VALUE:
          Toast.makeText(this,
              R.string.search_server_popup_text_size_limit_exceeded,
              Toast.LENGTH_SHORT).show();
          break;
        case ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE:
          Toast.makeText(this,
              R.string.search_server_popup_text_time_limit_exceeded,
              Toast.LENGTH_SHORT).show();
          break;
        default:
          final Intent i = new Intent(this, PopUp.class);
          i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
              getString(R.string.search_server_popup_title_search_error));
          i.putExtra(
              PopUp.BUNDLE_FIELD_TEXT,
              getString(R.string.search_server_popup_text_search_error, result
                  .getDiagnosticMessage(), result.getResultCode().getName()));
          startActivity(i);
          break;
      }
      if (progressDialog != null)
        progressDialog.dismiss();

      return;
    }

    final int entryCount = result.getEntryCount();
    if (entryCount == 0) {
      entries.clear();
      if (progressDialog != null)
        progressDialog.dismiss();

      Toast.makeText(this,
          R.string.search_server_popup_text_no_entries_returned,
          Toast.LENGTH_SHORT);
    } else {
      entries.clear();
      entries.addAll(result.getSearchEntries());
      if (progressDialog != null)
        progressDialog.dismiss();
    }
  }

}