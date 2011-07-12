package de.tubs.ibr.android.ldap.core.activities;

import static com.unboundid.util.StaticUtils.EOL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResultEntry;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.PopUp;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
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
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class LDAPTabActivity extends ListActivity implements OnClickListener,
    OnItemClickListener, OnItemLongClickListener {
  // A map of the entry strings to their corresponding names.
  private HashMap<String, SearchResultEntry> entryMap;

  // The list of entries to process.
  private LinkedList<SearchResultEntry> entries;
  private String[] entryStrings;
  // The server instance to search.
  private ServerInstance instance;

  // The search text.
  private String searchText = "";

  // The progress dialog displayed while the search is in progress.
  private volatile ProgressDialog progressDialog = null;

  private ArrayAdapter<String> adapter;

  private SearchService.SearchBinder mBinder;

  private final Handler messageHandler = new Handler();

  private Context mContext;

  private Filter filter;

  /**
   * Creates a new instance of this class.
   */
  public LDAPTabActivity() {
    entryMap = new HashMap<String, SearchResultEntry>(0);
    entries = new LinkedList<SearchResultEntry>();
  }

  class ShowResultsRunnable extends SearchResultRunnable {
    public Context c;

    public ShowResultsRunnable(Context c) {
      this.c = c;
    }

    @Override
    public void run() {
      // Looper.prepare();
      final ResultCode resultCode = result.getResultCode();
      if (resultCode != ResultCode.SUCCESS) {
        switch (resultCode.intValue()) {
          case ResultCode.SIZE_LIMIT_EXCEEDED_INT_VALUE:
            Toast.makeText(c,
                R.string.search_server_popup_text_size_limit_exceeded,
                Toast.LENGTH_SHORT).show();
            showEntries();
            break;
          case ResultCode.TIME_LIMIT_EXCEEDED_INT_VALUE:
            Toast.makeText(c,
                R.string.search_server_popup_text_time_limit_exceeded,
                Toast.LENGTH_SHORT).show();
            break;
          default:
            final Intent i = new Intent(c, PopUp.class);
            i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
                getString(R.string.search_server_popup_title_search_error));
            i.putExtra(
                PopUp.BUNDLE_FIELD_TEXT,
                getString(R.string.search_server_popup_text_search_error,
                    result.getDiagnosticMessage(), result.getResultCode()
                        .getName()));
            startActivity(i);
            break;
        }
        if (progressDialog != null)
          progressDialog.dismiss();

        return;
      }

      showEntries();

    }

    private void showEntries() {
      final int entryCount = result.getEntryCount();
      if (entryCount == 0) {
        entries.clear();
        if (progressDialog != null)
          progressDialog.dismiss();

        Toast.makeText(c,
            R.string.search_server_popup_text_no_entries_returned,
            Toast.LENGTH_SHORT);
      } else {
        entries.clear();
        entries.addAll(result.getSearchEntries());
        entryMap = new HashMap<String, SearchResultEntry>(entries.size());

        final StringBuilder buffer = new StringBuilder();
        entryStrings = new String[entries.size()];
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

        adapter = new ArrayAdapter<String>(c,
            android.R.layout.simple_list_item_1, entryStrings);
        setListAdapter(adapter);

        if (progressDialog != null)
          progressDialog.dismiss();
      }
    }

  }

  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mBinder = (SearchService.SearchBinder) service;
      mBinder.setActivityCallBackHandler(messageHandler);
      mBinder.setRunnable(new ShowResultsRunnable(mContext));
      mBinder.search(instance, filter);
    }
  };

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
    AccountManager accManager = AccountManager.get(this);
    Account[] accounts = accManager
        .getAccountsByType(getString(R.string.ACCOUNT_TYPE));
    SharedPreferences mPrefs = PreferenceManager
        .getDefaultSharedPreferences(this);
    String accountname = mPrefs.getString("selectedAccount", "");
    for (Account a : accounts) {
      if (a.name.equals(accountname)) {
        instance = new ServerInstance(accManager, a);
        break;
      }
    }
    if (instance == null) {
      instance = new ServerInstance(this.getIntent().getExtras());
    }
    setContentView(R.layout.ldaptab);
    mContext = this;
    // Add an on-click listener to the search button.
    final Button searchButton = (Button) findViewById(R.id.layout_search_server_button_search);
    searchButton.setOnClickListener(this);
    setTitle(getString(R.string.list_search_results_activity_title,
        entries.size()));
    entryMap = new HashMap<String, SearchResultEntry>(entries.size());

    final StringBuilder buffer = new StringBuilder();
    entryStrings = new String[entries.size()];
    for (int i = 0; i < entryStrings.length; i++) {
      final SearchResultEntry e = entries.get(i);
      if (e.hasObjectClass("person")) {
        final String name = e.getAttributeValue(AttributeMapper.FULL_NAME);
        if (name == null) {
          entryStrings[i] = e.getDN();
        } else {
          buffer.setLength(0);
          buffer.append(name);

          final String phone = e
              .getAttributeValue(AttributeMapper.PRIMARY_PHONE);
          if (phone != null) {
            buffer.append(EOL);
            buffer.append(phone);
          }

          final String mail = e.getAttributeValue(AttributeMapper.PRIMARY_MAIL);
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

    try {
      filter = Filter.create("(cn=*)");
    } catch (LDAPException le2) {
    }

    // Bind to SearchService
    final Intent intent = new Intent(this, SearchService.class);
    getApplicationContext().bindService(intent, mServiceConnection,
        Context.BIND_AUTO_CREATE);

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
        try {
          filter = Filter.create(searchText);
        } catch (LDAPException le) {
          try {
            // If no searchtext is available, search for all entries
            if (searchText.length() == 0) {
              searchText = "*";
            }
            filter = Filter.create("(cn=" + searchText + ')');
          } catch (LDAPException le2) {
            Toast.makeText(this,
                R.string.search_server_popup_text_invalid_filter,
                Toast.LENGTH_SHORT).show();
            break;
          }
        }
        // Search by SearchService
        if (mBinder != null && mBinder.isBinderAlive()) {
          mBinder.search(instance, filter);

          // Create a progress dialog to display while the search is in
          // progress.
          progressDialog = new ProgressDialog(this);
          progressDialog
              .setTitle(getString(R.string.search_server_progress_searching));
          progressDialog.setIndeterminate(true);
          progressDialog.setCancelable(true);
          progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
          progressDialog.show();
        }
        break;
      default:
        break;
    }
  }
}