package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.AddServer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TabBrowserActivity extends TabActivity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    showTabView();

    AccountManager accManager = AccountManager.get(this);
    Account[] accArray = accManager.getAccountsByType(this
        .getString(R.string.ACCOUNT_TYPE));

    if (accArray.length < 1) {
      Intent intent = new Intent(this, AddServer.class);
      intent.putExtra("INTENT_EXTRA_TILE", "Create new account!");
      startActivityForResult(intent, AddServer.INTENT_REQUEST_NEWACCOUNT);
    } else {
      showTabView();
    }

  }

  private void showTabView() {
    setContentView(R.layout.tab);

    /** TabHost will have Tabs */
    TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

    TabSpec localTabSpec = tabHost.newTabSpec("tid1");
    TabSpec ldapTabSpec = tabHost.newTabSpec("tid1");

    localTabSpec.setIndicator("Local").setContent(
        new Intent(this, LocalTabActivity.class));
    ldapTabSpec.setIndicator("LDAP").setContent(
        new Intent(this, LDAPTabActivity.class));

    tabHost.addTab(localTabSpec);
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
      case R.id.account_preferences:
        Intent settingsActivity = new Intent(getBaseContext(),
            PrefActivity.class);
        startActivity(settingsActivity);
        break;
      default:
        break;
    }
    return false;
  }

}
