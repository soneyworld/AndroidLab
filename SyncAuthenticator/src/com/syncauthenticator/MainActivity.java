package com.syncauthenticator;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

public class MainActivity extends Activity {
	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        Button prefBtn = (Button) findViewById(R.id.prefButton);
        Button addBtn = (Button) findViewById(R.id.addServerButton);
        Button setFalse = (Button) findViewById(R.id.setFalseButton);
        
        prefBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                        Intent settingsActivity = new Intent(getBaseContext(), PrefActivity.class);
                        startActivity(settingsActivity);
            }
        });
        
        addBtn.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
                    Intent addServerActivity = new Intent(getBaseContext(), SyncAuthenticatorActivity.class);
                    startActivity(addServerActivity);
            }
        });
        
        setFalse.setOnClickListener(new OnClickListener() {

            public void onClick(View v) {
            	setPrefFalse();
            }
        });
    }
	
	public void setPrefFalse() {
        SharedPreferences mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        Editor prefEditor = mPrefs.edit();
        prefEditor.putBoolean("backGroundDataPref", false);
        prefEditor.commit();
	}

}
