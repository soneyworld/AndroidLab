package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;

public class PrefActivity extends PreferenceActivity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    addPreferencesFromResource(R.xml.choose_account_preferences);
    ListPreference list = (ListPreference) findPreference("selectedAccount");
    AccountManager accManager = AccountManager.get(this);
    Account[] accounts = accManager.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
    CharSequence[] entries = new CharSequence[accounts.length];
    for (int i=0;i<entries.length;i++) {
      entries[i] = accounts[i].name;
    }
    list.setEntries(entries);
    list.setEntryValues(entries);
  }
}