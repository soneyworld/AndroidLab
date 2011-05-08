package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import android.app.Activity;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LDAPTabActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.ldaptab);

    /* First Tab Content */
    Button setFalse = (Button) findViewById(R.id.setFalseButton);

    setFalse.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
        setPrefFalse();
      }
    });

  }

  public void setPrefFalse() {
    SharedPreferences mPrefs = PreferenceManager
        .getDefaultSharedPreferences(this);
    Editor prefEditor = mPrefs.edit();
    prefEditor.putBoolean("backGroundDataPref", false);
    prefEditor.commit();
  }
}