package com.syncauthenticator;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
 
public class PrefActivity extends PreferenceActivity {
        @Override
        protected void onCreate(Bundle savedInstanceState) {
                super.onCreate(savedInstanceState);
                addPreferencesFromResource(R.xml.preferences);
                SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
                Editor prefEditor = mPrefs.edit();
                prefEditor.putBoolean("backGroundDataPref", false);
                
                
        }
}