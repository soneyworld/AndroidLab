/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2010 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package de.tubs.ibr.android.ldap.auth;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import static com.unboundid.util.StaticUtils.*;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.provider.*;

/**
 * This class provides an Android activity that may be used to define a new
 * directory server.
 */
public final class AddServer extends AccountAuthenticatorActivity implements
    OnClickListener, ServerTestInvoker {
  /**
   * The name of the field used to define the server ID.
   */
  public static final String BUNDLE_FIELD_ID = "ADD_SERVER_ID";

  /**
   * The name of the field used to define the server address.
   */
  public static final String BUNDLE_FIELD_HOST = "ADD_SERVER_HOST";

  /**
   * The name of the field used to define the server port.
   */
  public static final String BUNDLE_FIELD_PORT = "ADD_SERVER_PORT";

  /**
   * The name of the field used to define the security method.
   */
  public static final String BUNDLE_FIELD_SECURITY = "ADD_SERVER_SECURITY";

  /**
   * The name of the field used to define the bind DN.
   */
  public static final String BUNDLE_FIELD_BIND_DN = "ADD_SERVER_BIND_DN";

  /**
   * The name of the field used to define the bind password.
   */
  public static final String BUNDLE_FIELD_BIND_PW = "ADD_SERVER_BIND_PW";

  /**
   * The name of the field used to define the base DN.
   */
  public static final String BUNDLE_FIELD_BASE_DN = "ADD_SERVER_BASE_DN";

  public static final String INTENT_EXTRA_TITLE = "TITLE";

  public static final int INTENT_REQUEST_NEWACCOUNT = 1;

  /**
   * If the Activity is started with the MODIFICATION Action, this var should be
   * true, else false
   */
  private boolean modifymode = false;

  // Indicates whether to use SSL.
  private boolean useSSL = false;

  // Indicates whether to use StartTLS.
  private boolean useStartTLS = false;

  // The server port.
  private int port = 389;

  // The progress dialog displayed while the search is in progress.
  private volatile ProgressDialog progressDialog = null;

  // The server base DN.
  private String baseDN = "";

  // The bind DN.
  private String bindDN = "";

  // The bind password.
  private String bindPW = "";

  // The server address.
  private String host = "";

  // The server ID.
  private String id = "";

  /**
   * Performs all necessary processing when this activity is created.
   * 
   * @param state
   *          The state information for this activity.
   */
  @Override()
  protected void onCreate(final Bundle state) {
    super.onCreate(state);
    if (state != null) {
      restoreState(state);
    }
  }

  /**
   * Performs all necessary processing when this activity is started or resumed.
   */
  @Override()
  protected void onResume() {
    super.onResume();
    setContentView(R.layout.layout_define_server);
    Object title = getIntent().getExtras().get(INTENT_EXTRA_TITLE);
    if (title == null) {
      setTitle(R.string.activity_label);
    } else {
      try {
        setTitle((Integer) title);
      } catch (Exception e) {
        try {
          setTitle((String) title);
        } catch (Exception e1) {
          setTitle(R.string.activity_label);
        }
      }
    }
    if (this.getIntent().getAction()
        .equals(getString(R.string.MODIFY_ACCOUNT_ACTION))) {
      Bundle b = this.getIntent().getExtras();
      Set<String> keys = b.keySet();
      for (String key : keys) {
        if (b.get(key).getClass().equals(Account.class)) {
          Account acc = (Account) b.get(key);
          AccountManager accManager = AccountManager.get(this);
          this.id = accManager.getUserData(acc, "id");
          this.bindDN = accManager.getUserData(acc, "bindDN");
          this.baseDN = accManager.getUserData(acc, "baseDN");
          this.host = accManager.getUserData(acc, "host");
          try {
            this.port = Integer.parseInt(accManager.getUserData(acc, "port"));
          } catch (Exception e) {
            this.port = 389;
          }
          try {
            this.useSSL = Boolean.parseBoolean(accManager.getUserData(acc,
                "useSSL"));
          } catch (Exception e) {
            this.useSSL = false;
          }
          try {
            this.useStartTLS = Boolean.parseBoolean(accManager.getUserData(acc,
                "useStartTLS"));
          } catch (Exception e) {
            this.useStartTLS = false;
          }
          this.modifymode = true;
          break;
        }
      }

    }
    // Populate the server ID.
    final EditText idField = (EditText) findViewById(R.id.layout_define_server_field_id);
    idField.setText(id);
    if (this.modifymode) {
      idField.setEnabled(false);
    }
    // Populate the server address.
    final EditText hostField = (EditText) findViewById(R.id.layout_define_server_field_host);
    hostField.setText(host);
    // Populate the server port.
    final EditText portField = (EditText) findViewById(R.id.layout_define_server_field_port);
    portField.setText(String.valueOf(port));
    // Populate the list of available security mechanisms.
    final Spinner secSpinner = (Spinner) findViewById(R.id.layout_define_server_spinner_security);
    final ArrayAdapter<CharSequence> secAdapter = ArrayAdapter
        .createFromResource(this, R.array.add_server_security_type_list,
            android.R.layout.simple_spinner_item);
    secAdapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    secSpinner.setAdapter(secAdapter);
    if (useSSL) {
      secSpinner.setSelection(1);
    } else if (useStartTLS) {
      secSpinner.setSelection(2);
    } else {
      secSpinner.setSelection(0);
    }

    // Populate the bind DN.
    final EditText bindDNField = (EditText) findViewById(R.id.layout_define_server_field_bind_dn);
    bindDNField.setText(bindDN);

    // Populate the bind Password.
    final EditText bindPWField = (EditText) findViewById(R.id.layout_define_server_field_bind_pw);
    bindPWField.setText(bindPW);

    // Populate the base DN.
    final EditText baseDNField = (EditText) findViewById(R.id.layout_define_server_field_base);
    baseDNField.setText(baseDN);

    // Add an on-click listener to the test and save buttons.
    final Button testButton = (Button) findViewById(R.id.layout_define_server_button_server_test);
    testButton.setOnClickListener(this);

    final Button saveButton = (Button) findViewById(R.id.layout_define_server_button_server_save);
    saveButton.setOnClickListener(this);

  }

  /**
   * Performs all necessary processing when the instance state needs to be
   * saved.
   * 
   * @param state
   *          The state information to be saved.
   */
  @Override()
  protected void onSaveInstanceState(final Bundle state) {
    saveState(state);
  }

  /**
   * Performs all necessary processing when the instance state needs to be
   * restored.
   * 
   * @param state
   *          The state information to be restored.
   */
  @Override()
  protected void onRestoreInstanceState(final Bundle state) {
    restoreState(state);
  }

  /**
   * Takes any appropriate action after a button has been clicked.
   * 
   * @param view
   *          The view for the button that was clicked.
   */
  public void onClick(final View view) {
    // Figure out which button was clicked and take the appropriate action.
    switch (view.getId()) {
      case R.id.layout_define_server_button_server_test:
        testSettings();
        break;

      case R.id.layout_define_server_button_server_save:
        saveSettings();
        break;

      default:
        // This should never happen, and if it does we won't do anything.
        break;
    }
  }

  /**
   * Creates a new {@code ServerInstance} structure from the provided
   * information.
   * 
   * @return The created {@code ServerInstance} structure.
   * @throws NumberFormatException
   *           If the port number is not an integer.
   */
  private ServerInstance createInstance() throws NumberFormatException {
    final EditText idField = (EditText) findViewById(R.id.layout_define_server_field_id);
    final String serverID = idField.getText().toString();
    final EditText hostField = (EditText) findViewById(R.id.layout_define_server_field_host);
    host = hostField.getText().toString();
    final EditText portField = (EditText) findViewById(R.id.layout_define_server_field_port);
    port = Integer.parseInt(portField.getText().toString());
    useSSL = false;
    useStartTLS = false;
    final Spinner secSpinner = (Spinner) findViewById(R.id.layout_define_server_spinner_security);
    switch (secSpinner.getSelectedItemPosition()) {
      case 1:
        useSSL = true;
        break;
      case 2:
        useStartTLS = true;
        break;
      default:
        // No security.
        break;
    }
    final EditText bindDNField = (EditText) findViewById(R.id.layout_define_server_field_bind_dn);
    bindDN = bindDNField.getText().toString();
    final EditText bindPWField = (EditText) findViewById(R.id.layout_define_server_field_bind_pw);
    bindPW = bindPWField.getText().toString();
    final EditText baseField = (EditText) findViewById(R.id.layout_define_server_field_base);
    baseDN = baseField.getText().toString();
    return new ServerInstance(serverID, host, port, useSSL, useStartTLS,
        bindDN, bindPW, baseDN);
  }

  /**
   * Tests the provided server settings to determine if they are acceptable.
   */
  private void testSettings() {
    final LinkedList<String> reasons = new LinkedList<String>();
    final ServerInstance instance;
    try {
      instance = createInstance();
    } catch (final NumberFormatException nfe) {
      reasons.add(getString(R.string.add_server_err_port_not_int));
      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
          getString(R.string.add_server_popup_title_failed));
      i.putExtra(
          PopUp.BUNDLE_FIELD_TEXT,
          getString(R.string.add_server_popup_text_failed,
              listToString(reasons)));
      startActivity(i);
      return;
    }
    // Create and start a thread to test the server settings.
    final TestServerThread testThread = new TestServerThread(this, instance);
    testThread.start();
    // Create a progress dialog to display while the search is in progress.
    progressDialog = new ProgressDialog(this);
    progressDialog.setTitle(getString(R.string.add_server_progress_text));
    progressDialog.setIndeterminate(true);
    progressDialog.setCancelable(false);
    progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
    progressDialog.show();
  }

  /**
   * {@inheritDoc}
   */
  public void testCompleted(final boolean acceptable,
      final LinkedList<String> reasons) {
    progressDialog.dismiss();
    if (acceptable) {
      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
          getString(R.string.add_server_popup_title_success));
      i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
          getString(R.string.add_server_popup_text_success));
      startActivity(i);
    } else {
      final Intent i = new Intent(this, PopUp.class);
      i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
          getString(R.string.add_server_popup_title_failed));
      i.putExtra(
          PopUp.BUNDLE_FIELD_TEXT,
          getString(R.string.add_server_popup_text_failed,
              listToString(reasons)));
      startActivity(i);
    }
  }

  /**
   * Saves the provided server settings.
   */
  private void saveSettings() {
    boolean acceptable;
    final LinkedList<String> reasons = new LinkedList<String>();
    ServerInstance instance = null;
    try {
      instance = createInstance();
      acceptable = instance.isDefinitionValid(this, reasons);
    } catch (final NumberFormatException nfe) {
      acceptable = false;
      reasons.add(getString(R.string.add_server_err_port_not_int));
    }
    if (acceptable) {
      final String instanceID = instance.getID();
      try {
        AccountManager accManager = AccountManager.get(this);
        Account[] accArray = accManager.getAccountsByType(this
            .getString(R.string.ACCOUNT_TYPE));
        if (!this.modifymode) {
          if (accArray.length > 0) {
            for (Account account : accArray) {
              if (accManager.getUserData(account, "id")
                  .equals(instance.getID())) {
                acceptable = false;
                reasons.add(getString(
                    R.string.add_server_err_server_id_already_in_use,
                    instanceID));
                break;
              }
            }
          }
        }
        if (acceptable) {
          final Intent intent = new Intent();
          intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, this.id);
          intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE,
              getString(R.string.ACCOUNT_TYPE));
          intent.putExtra(AccountManager.KEY_PASSWORD, this.bindPW);
          if (this.modifymode) {
            // TODO Hier muss noch das Update integriert werden, bzw. geklärt
            // werden, wie es funktioniert
            Bundle b = instance.createBundle();
            setAccountAuthenticatorResult(b);

          } else {
            Account acc = new Account(instance.getID(),
                this.getString(R.string.ACCOUNT_TYPE));
            accManager.addAccountExplicitly(acc, instance.getBindPassword(),
                instance.createBundle());
          }
          setResult(Activity.RESULT_OK, intent);
          finish();
          return;

        }
      } catch (Exception e) {
        reasons.add(getExceptionMessage(e));
      }
    }
    final Intent i = new Intent(this, PopUp.class);
    i.putExtra(PopUp.BUNDLE_FIELD_TITLE,
        getString(R.string.add_server_popup_title_failed));
    i.putExtra(PopUp.BUNDLE_FIELD_TEXT,
        getString(R.string.add_server_popup_text_failed, listToString(reasons)));
    startActivity(i);
  }

  /**
   * Restores the state of this activity from the provided bundle.
   * 
   * @param state
   *          The bundle containing the state information.
   */
  private void restoreState(final Bundle state) {
    id = state.getString(BUNDLE_FIELD_ID);
    if (id == null) {
      id = "";
    }
    host = state.getString(BUNDLE_FIELD_HOST);
    if (host == null) {
      host = "";
    }
    port = state.getInt(BUNDLE_FIELD_PORT);
    if (port <= 0) {
      port = 389;
    }
    switch (state.getInt(BUNDLE_FIELD_SECURITY)) {
      case 1:
        useSSL = true;
        useStartTLS = false;
        break;
      case 2:
        useSSL = false;
        useStartTLS = true;
        break;
      default:
        useSSL = false;
        useStartTLS = false;
        break;
    }
    bindDN = state.getString(BUNDLE_FIELD_BIND_DN);
    if (bindDN == null) {
      bindDN = "";
    }
    bindPW = state.getString(BUNDLE_FIELD_BIND_PW);
    if (bindPW == null) {
      bindPW = "";
    }
    baseDN = state.getString(BUNDLE_FIELD_BASE_DN);
    if (baseDN == null) {
      baseDN = "";
    }
  }

  /**
   * Saves the state of this activity to the provided bundle.
   * 
   * @param state
   *          The bundle containing the state information.
   */
  private void saveState(final Bundle state) {
    final EditText idField = (EditText) findViewById(R.id.layout_define_server_field_id);
    id = idField.getText().toString();
    state.putString(BUNDLE_FIELD_ID, id);
    final EditText hostField = (EditText) findViewById(R.id.layout_define_server_field_host);
    host = hostField.getText().toString();
    state.putString(BUNDLE_FIELD_HOST, host);
    final EditText portField = (EditText) findViewById(R.id.layout_define_server_field_port);
    try {
      port = Integer.parseInt(portField.getText().toString());
    } catch (final NumberFormatException nfe) {
      port = 389;
    }
    state.putInt(BUNDLE_FIELD_PORT, port);
    final Spinner secSpinner = (Spinner) findViewById(R.id.layout_define_server_spinner_security);
    final int secVal = secSpinner.getSelectedItemPosition();
    switch (secVal) {
      case 1:
        useSSL = true;
        useStartTLS = false;
        break;
      case 2:
        useSSL = false;
        useStartTLS = true;
        break;
      default:
        useSSL = false;
        useStartTLS = false;
        break;
    }
    state.putInt(BUNDLE_FIELD_SECURITY, secVal);
    final EditText bindDNField = (EditText) findViewById(R.id.layout_define_server_field_bind_dn);
    bindDN = bindDNField.getText().toString();
    state.putString(BUNDLE_FIELD_BIND_DN, bindDN);
    final EditText bindPWField = (EditText) findViewById(R.id.layout_define_server_field_bind_pw);
    bindPW = bindPWField.getText().toString();
    state.putString(BUNDLE_FIELD_BIND_PW, bindPW);
    final EditText baseDNField = (EditText) findViewById(R.id.layout_define_server_field_base);
    baseDN = baseDNField.getText().toString();
    state.putString(BUNDLE_FIELD_BASE_DN, baseDN);
  }

  /**
   * Retrieves a string representation of the contents of the provided list.
   * 
   * @param l
   *          The list from which to take the elements.
   * @return A string representation of the contents of the provided list.
   */
  private static String listToString(final List<String> l) {
    final StringBuilder buffer = new StringBuilder();
    for (final String s : l) {
      buffer.append(EOL);
      buffer.append(EOL);
      buffer.append("- ");
      buffer.append(s);
    }
    return buffer.toString();
  }
}
