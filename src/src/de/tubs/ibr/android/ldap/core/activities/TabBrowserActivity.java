package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.AddServer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TabBrowserActivity extends TabActivity implements
    OnSharedPreferenceChangeListener {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AccountManager accManager = AccountManager.get(this);
    Account[] accArray = accManager.getAccountsByType(this
        .getString(R.string.ACCOUNT_TYPE));

    if (accArray.length < 1) {
      Intent intent = new Intent(this, AddServer.class);
      intent.putExtra("INTENT_EXTRA_TILE", "Create new account!");
      startActivityForResult(intent, AddServer.INTENT_REQUEST_NEWACCOUNT);
    } else {
      showTabView();
      SharedPreferences mPrefs = PreferenceManager
          .getDefaultSharedPreferences(this);
      mPrefs.registerOnSharedPreferenceChangeListener(this);
    }

  }

  private void showTabView() {
    setContentView(R.layout.tab);

    /** TabHost will have Tabs */
    TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

    TabSpec localTabSpec = tabHost.newTabSpec("tid1");
    localTabSpec.setIndicator(("Local"), getResources().getDrawable(R.drawable.home_icon)); 
    localTabSpec.setContent(new Intent(this, LocalTabActivity.class));
    tabHost.addTab(localTabSpec);
    
    TabSpec syncTabSpec = tabHost.newTabSpec("tid1");
    syncTabSpec.setIndicator(("Sync"), getResources().getDrawable(android.R.drawable.ic_popup_sync)); 
    syncTabSpec.setContent(new Intent(this, SyncTabActivity.class));
    tabHost.addTab(syncTabSpec);

    TabSpec ldapTabSpec = tabHost.newTabSpec("tid1");

    AccountManager accManager = AccountManager.get(this);
    Account[] accounts = accManager.getAccountsByType(this
        .getString(R.string.ACCOUNT_TYPE));
    SharedPreferences mPrefs = PreferenceManager
        .getDefaultSharedPreferences(this);
    String accountname = mPrefs.getString("selectedAccount", "");
    boolean isFoundOnPref = false;
    for (Account a : accounts) {
      if (a.name.equals(accountname)) {
        addSelectedAccountTab(a, accManager, ldapTabSpec, tabHost);
        isFoundOnPref = true;
        break;
      }
    }
    if (!isFoundOnPref)
      addSelectedAccountTab(accounts[0], accManager, ldapTabSpec, tabHost);
  }

  private void addSelectedAccountTab(Account a, AccountManager accManager,
      TabSpec ldapTabSpec, TabHost tabHost) {
    final Intent ldapintent = new Intent(this, LDAPTabActivity.class);
    ldapintent.putExtra("id", a.name);
    ldapintent.putExtra("host", accManager.getUserData(a, "host"));
    try {
      ldapintent.putExtra("port",
          Integer.parseInt(accManager.getUserData(a, "port")));
    } catch (Exception e) {
      ldapintent.putExtra("port", 389);
    }
    try {
      ldapintent.putExtra("useSSL",
          Boolean.parseBoolean(accManager.getUserData(a, "useSSL")));
    } catch (Exception e) {
      ldapintent.putExtra("useSSL", false);
    }
    try {
      ldapintent.putExtra("useStartTLS",
          Boolean.parseBoolean(accManager.getUserData(a, "useStartTLS")));
    } catch (Exception e) {
      ldapintent.putExtra("useStartTLS", false);
    }
    ldapintent.putExtra("bindDN", accManager.getUserData(a, "bindDN"));
    ldapintent.putExtra("bindPW", accManager.getUserData(a, "bindPW"));
    ldapintent.putExtra("baseDN", accManager.getUserData(a, "baseDN"));
    ldapTabSpec.setIndicator(("LDAP"), getResources().getDrawable(R.drawable.loup_icon)); 
    ldapTabSpec.setContent(ldapintent);
    tabHost.addTab(ldapTabSpec);

  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    switch (resultCode) {
      case Activity.RESULT_OK:
        showTabView();
        break;
      case Activity.RESULT_CANCELED:
        finish();
        break;
      default:
        break;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }

  public boolean onCreateOptionsMenu(Menu menu) {

    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.tab_browser_menu, menu);

    return true;

  }

  public boolean onOptionsItemSelected(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.choose_account_preferences:
        Intent settingsActivity = new Intent(getBaseContext(),
            PrefActivity.class);
        startActivity(settingsActivity);
        break;
      case R.id.test_conflict_view:
        Intent conflictView = new Intent(getBaseContext(), ConflictActivity.class);
        startActivity(conflictView);
        break;
      case R.id.add_contact:
        Intent addContactView = new Intent(getBaseContext(), EditContactActivity.class);
        addContactView.setAction(Intent.ACTION_INSERT);
        startActivity(addContactView);
        break;
      default:
        break;
    }
    return false;
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
      String key) {
    showTabView();
  }

}
