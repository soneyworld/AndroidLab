package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.AddServer;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.TabActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TabHost;
import android.widget.TabHost.TabSpec;

public class TabBrowserActivity extends TabActivity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    AccountManager accManager = AccountManager.get(this);
    Account[] accArray = accManager
        .getAccountsByType("de.tubs.ibr.android.ldap");

    if (accArray.length < 1) {
      Intent intent = new Intent(this, AddServer.class);
      intent.putExtra("INTENT_EXTRA_TILE", "Create new account!");
      startActivityForResult(intent, AddServer.INTENT_REQUEST_NEWACCOUNT);
    } else {
      showTabView();
    }

  }

  /**
   * 
   */
  private void showTabView() {
    setContentView(R.layout.tab);

    /** TabHost will have Tabs */
    TabHost tabHost = (TabHost) findViewById(android.R.id.tabhost);

    /**
     * TabSpec used to create a new tab. By using TabSpec only we can able to
     * setContent to the tab. By using TabSpec setIndicator() we can set name to
     * tab.
     */

    /** tid1 is localTabSpec Id. Its used to access outside. */
    TabSpec localTabSpec = tabHost.newTabSpec("tid1");
    TabSpec ldapTabSpec = tabHost.newTabSpec("tid1");

    /** TabSpec setIndicator() is used to set name for the tab. */
    /** TabSpec setContent() is used to set content for a particular tab. */
    localTabSpec.setIndicator("Local").setContent(
        new Intent(this, LocalTabActivity.class));
    ldapTabSpec.setIndicator("LDAP").setContent(
        new Intent(this, LDAPTabActivity.class));

    /** Add tabSpec to the TabHost to display. */
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

}
