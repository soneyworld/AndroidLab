package de.tubs.ibr.android.ldap.core.activities;

import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.AddServer;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class LocalTabActivity extends Activity {

  @Override
  public void onCreate(Bundle savedInstanceState) {
  super.onCreate(savedInstanceState);
  setContentView(R.layout.localtab);

    /* First Tab Content */
      Button prefBtn = (Button) findViewById(R.id.prefButton);
      Button addBtn = (Button) findViewById(R.id.addServerButton);;
    
    prefBtn.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
                    Intent settingsActivity = new Intent(getBaseContext(), PrefActivity.class);
                    startActivity(settingsActivity);
      }
    });
    
    addBtn.setOnClickListener(new OnClickListener() {

      public void onClick(View v) {
                Intent addServerActivity = new Intent(getBaseContext(), AddServer.class);
                startActivity(addServerActivity);
      }
    });

  }
}
