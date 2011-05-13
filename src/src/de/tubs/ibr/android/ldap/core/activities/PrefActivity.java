package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

public class PrefActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener{

  @Override
  protected void onResume() {
    super.onResume();
    addPreferencesFromResource(R.xml.choose_account_preferences);
    ListPreference list = (ListPreference) findPreference("selectedAccount");
    AccountManager accManager = AccountManager.get(this);
    Account[] accounts = accManager.getAccountsByType(getString(R.string.ACCOUNT_TYPE));
    CharSequence[] entries = new CharSequence[accounts.length];
    for (int i=0;i<entries.length;i++) {
      entries[i] = accounts[i].name;
    }
    SharedPreferences mPrefs = PreferenceManager
    .getDefaultSharedPreferences(this);
    String accountname = mPrefs.getString("selectedAccount", "");
    list.setEntries(entries);
    list.setEntryValues(entries);
    list.setTitle(accountname);
    mPrefs.registerOnSharedPreferenceChangeListener(this);
  }

  @Override
  public void onSharedPreferenceChanged(SharedPreferences arg0, String arg1) {
    ListPreference list = (ListPreference) findPreference("selectedAccount");
    list.setTitle(arg0.getString(arg1, "<none selected>"));
    
  }
}