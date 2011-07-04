package de.tubs.ibr.android.ldap.core.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Email;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;
import java.util.ArrayList;
import java.util.Iterator;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import de.tubs.ibr.android.ldap.provider.LDAPResultRunnable;
import de.tubs.ibr.android.ldap.provider.LDAPService;

public final class ContactAdder extends Activity implements
    OnAccountsUpdateListener {
  public static final String TAG = "ContactsAdder";
  public static final String ACCOUNT_NAME = "LDAP";
  public static final String ACCOUNT_TYPE = "de.tubs.ibr.ldap";

  private ArrayList<AccountData> mAccounts;
  private AccountAdapter mAccountAdapter;
  private ArrayAdapter<String> mDirectoryAdapter;
  private Spinner mAccountSpinner;
  private Spinner mDirectorySpinner;
  private EditText mUserIdEditText;
  private TextView mUserIdTextView;
  private EditText mContactEmailEditText;
  private ArrayList<Integer> mContactEmailTypes;
  private Spinner mContactEmailTypeSpinner;
  private EditText mContactFirstnameEditText;
  private EditText mContactNameEditText;
  private EditText mContactPhoneEditText;
  private ArrayList<Integer> mContactPhoneTypes;
  private Spinner mContactPhoneTypeSpinner;
  private CheckBox mSyncCheckBox;
  private Button mContactSaveButton;
  private Button mContactExportButton;
  private AccountData mSelectedAccount;
  private LDAPService.LDAPBinder mBinder;
  private final Handler messageHandler = new Handler();
  private ServerInstance instance;
  private Context mContext;

  /**
   * Called when the activity is first created. Responsible for initializing the
   * UI.
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.v(TAG, "Activity State: onCreate()");
    super.onCreate(savedInstanceState);
    setContentView(R.layout.contact_adder);
    mContext = this;

    // Obtain handles to UI objects
    mAccountSpinner = (Spinner) findViewById(R.id.accountSpinner);
    mDirectorySpinner = (Spinner) findViewById(R.id.directorySpinner);
    mUserIdEditText = (EditText) findViewById(R.id.userIdEditText);
    mUserIdTextView = (TextView) findViewById(R.id.userIdTextView);
    mContactFirstnameEditText = (EditText) findViewById(R.id.contactFirstnameEditText);
    mContactNameEditText = (EditText) findViewById(R.id.contactNameEditText);
    mContactPhoneEditText = (EditText) findViewById(R.id.contactPhoneEditText);
    mContactEmailEditText = (EditText) findViewById(R.id.contactEmailEditText);
    mContactPhoneTypeSpinner = (Spinner) findViewById(R.id.contactPhoneTypeSpinner);
    mContactEmailTypeSpinner = (Spinner) findViewById(R.id.contactEmailTypeSpinner);
    mSyncCheckBox = (CheckBox) findViewById(R.id.syncCheckBox);
    mContactSaveButton = (Button) findViewById(R.id.contactSaveButton);
    mContactExportButton = (Button) findViewById(R.id.contactExportButton);

    // Prepare list of supported account types
    // Note: Other types are available in ContactsContract.CommonDataKinds
    // Also, be aware that type IDs differ between Phone and Email, and MUST be
    // computed
    // separately.
    mContactPhoneTypes = new ArrayList<Integer>();
    mContactPhoneTypes.add(ContactsContract.CommonDataKinds.Phone.TYPE_HOME);
    mContactPhoneTypes.add(ContactsContract.CommonDataKinds.Phone.TYPE_WORK);
    mContactPhoneTypes.add(ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
    mContactPhoneTypes.add(ContactsContract.CommonDataKinds.Phone.TYPE_OTHER);
    mContactEmailTypes = new ArrayList<Integer>();
    mContactEmailTypes.add(ContactsContract.CommonDataKinds.Email.TYPE_HOME);
    mContactEmailTypes.add(ContactsContract.CommonDataKinds.Email.TYPE_WORK);
    mContactEmailTypes.add(ContactsContract.CommonDataKinds.Email.TYPE_MOBILE);
    mContactEmailTypes.add(ContactsContract.CommonDataKinds.Email.TYPE_OTHER);

    // Prepare model for account spinner
    mAccounts = new ArrayList<AccountData>();
    mAccountAdapter = new AccountAdapter(this, mAccounts);
    mAccountSpinner.setAdapter(mAccountAdapter);

    mDirectoryAdapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item);
    mDirectorySpinner.setAdapter(mDirectoryAdapter);
    mSyncCheckBox.setChecked(true);
    mContactExportButton.setVisibility(Button.GONE);

    // Populate list of account types for phone
    ArrayAdapter<String> adapter;
    adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item);
    adapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    Iterator<Integer> iter;
    iter = mContactPhoneTypes.iterator();
    while (iter.hasNext()) {
      adapter.add(ContactsContract.CommonDataKinds.Phone.getTypeLabel(
          this.getResources(), iter.next(),
          getString(R.string.undefinedTypeLabel)).toString());
    }
    mContactPhoneTypeSpinner.setAdapter(adapter);
    mContactPhoneTypeSpinner.setPrompt(getString(R.string.selectLabel));

    // Populate list of account types for email
    adapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item);
    adapter
        .setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    iter = mContactEmailTypes.iterator();
    while (iter.hasNext()) {
      adapter.add(ContactsContract.CommonDataKinds.Email.getTypeLabel(
          this.getResources(), iter.next(),
          getString(R.string.undefinedTypeLabel)).toString());
    }
    mContactEmailTypeSpinner.setAdapter(adapter);
    mContactEmailTypeSpinner.setPrompt(getString(R.string.selectLabel));

    // OnCLickListener for the SyncCheckBox
    mSyncCheckBox.setOnClickListener(new OnClickListener() {
      public void onClick(View v) {
        // Perform action on clicks, depending on whether it's now checked
        if (((CheckBox) v).isChecked()) {
          mUserIdEditText.setEnabled(true);
          mUserIdTextView.setEnabled(true);
        } else {
          mUserIdEditText.setText("");
          mUserIdEditText.setEnabled(false);
          mUserIdTextView.setEnabled(false);
        }
      }
    });

    // Prepare the system account manager. On registering the listener below, we
    // also ask for
    // an initial callback to pre-populate the account list.
    AccountManager.get(this).addOnAccountsUpdatedListener(this, null, true);

    // Register handlers for UI elements
    mAccountSpinner.setOnItemSelectedListener(new OnItemSelectedListener() {
      public void onItemSelected(AdapterView<?> parent, View view,
          int position, long i) {
        updateAccountSelection();
      }

      public void onNothingSelected(AdapterView<?> parent) {
        // We don't need to worry about nothing being selected, since Spinners
        // don't allow
        // this.
      }
    });
    mContactSaveButton.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        onSaveButtonClicked();
      }
    });
    final Intent intent = new Intent(this, LDAPService.class);
    getApplicationContext().bindService(intent, mServiceConnection,
        Context.BIND_AUTO_CREATE);
  }

  /**
   * Actions for when the Save button is clicked. Creates a contact entry and
   * terminates the activity.
   */
  private void onSaveButtonClicked() {
    Log.v(TAG, "Save button clicked");
    createContactEntry();
    finish();
  }

  /**
   * Creates a contact entry from the current UI values in the account named by
   * mSelectedAccount.
   */
  protected void createContactEntry() {
    // Get values from UI
    String userid = mUserIdEditText.getText().toString();
    Log.v(TAG, "UserID: " + userid);
    String firstname = mContactFirstnameEditText.getText().toString();
    String name = mContactNameEditText.getText().toString();
    String displayname = firstname + " " + name;
    String syncstatus;
    String dn = (String) mDirectorySpinner.getSelectedItem();
    if (mSyncCheckBox.isChecked()) {
      syncstatus = "locally added";
    } else {
      syncstatus = "";
      userid = "";
    }
    Log.v(TAG, "Syncstatus: " + syncstatus);
    if (userid.length() > 0) {
      dn = "uid=" + userid + "," + dn;
    } else {
      dn = "cn='" + displayname + "', " + dn;
    }
    String phone = mContactPhoneEditText.getText().toString();
    String email = mContactEmailEditText.getText().toString();
    int phoneType;
    switch (mContactPhoneTypeSpinner.getSelectedItemPosition()) {
      case 0:
        phoneType = Phone.TYPE_HOME;
        break;
      case 1:
        phoneType = Phone.TYPE_WORK;
        break;
      case 2:
        phoneType = Phone.TYPE_MOBILE;
        break;
      default:
        phoneType = Phone.TYPE_OTHER;
        break;
    }
    int emailType;
    switch (mContactEmailTypeSpinner.getSelectedItemPosition()) {
      case 0:
        emailType = Email.TYPE_HOME;
        break;
      case 1:
        emailType = Email.TYPE_WORK;
        break;
      case 2:
        emailType = Email.TYPE_MOBILE;
        break;
      default:
        emailType = Email.TYPE_OTHER;
        break;
    }
    // Prepare contact creation request
    //
    // Note: We use RawContacts because this data must be associated with a
    // particular account.
    // The system will aggregate this with any other data for this contact and
    // create a
    // coresponding entry in the ContactsContract.Contacts provider for us.
    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
    ops.add(ContentProviderOperation
        .newInsert(ContactsContract.RawContacts.CONTENT_URI)
        .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE,
            mSelectedAccount.getType())
        .withValue(ContactsContract.RawContacts.ACCOUNT_NAME,
            mSelectedAccount.getName())
        .withValue(ContactsContract.RawContacts.SYNC3, dn)
        .withValue(ContactsContract.RawContacts.SYNC1, syncstatus).build());
    ops.add(ContentProviderOperation
        .newInsert(ContactsContract.Data.CONTENT_URI)
        .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
        .withValue(ContactsContract.Data.MIMETYPE,
            ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
        .withValue(
            ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME,
            displayname)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME,
            firstname)
        .withValue(ContactsContract.CommonDataKinds.StructuredName.FAMILY_NAME,
            name).build());
    if (phone != null && phone.length() > 0) {
      ops.add(ContentProviderOperation
          .newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phone)
          .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
          .build());
    }
    if (email != null && email.length() > 0) {
      ops.add(ContentProviderOperation
          .newInsert(ContactsContract.Data.CONTENT_URI)
          .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
          .withValue(ContactsContract.Data.MIMETYPE,
              ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE)
          .withValue(ContactsContract.CommonDataKinds.Email.DATA, email)
          .withValue(ContactsContract.CommonDataKinds.Email.TYPE, emailType)
          .build());
    }
    // Ask the Contact provider to create a new contact
    Log.i(TAG, "Selected account: " + mSelectedAccount.getName() + " ("
        + mSelectedAccount.getType() + ")");
    Log.i(TAG, "Creating contact: " + displayname);
    try {
      getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
    } catch (Exception e) {
      // Display warning
      Context ctx = getApplicationContext();
      CharSequence txt = getString(R.string.contactCreationFailure);
      int duration = Toast.LENGTH_SHORT;
      Toast toast = Toast.makeText(ctx, txt, duration);
      toast.show();

      // Log exception
      Log.e(TAG, "Exceptoin encoutered while inserting contact: " + e);
    }
  }

  /**
   * Called when this activity is about to be destroyed by the system.
   */
  @Override
  public void onDestroy() {
    // Remove AccountManager callback
    AccountManager.get(this).removeOnAccountsUpdatedListener(this);
    super.onDestroy();
  }

  /**
   * Updates account list spinner when the list of Accounts on the system
   * changes. Satisfies OnAccountsUpdateListener implementation.
   */
  public void onAccountsUpdated(Account[] a) {
    Log.i(TAG, "Account list update detected");
    // Clear out any old data to prevent duplicates
    mAccounts.clear();

    // Get account data from system
    AuthenticatorDescription[] accountTypes = AccountManager.get(this)
        .getAuthenticatorTypes();

    // Populate tables
    for (int i = 0; i < a.length; i++) {
      // The user may have multiple accounts with the same name, so we need to
      // construct a
      // meaningful display name for each.
      String systemAccountType = a[i].type;
      AuthenticatorDescription ad = getAuthenticatorDescription(
          systemAccountType, accountTypes);
      AccountData data = new AccountData(a[i].name, ad);
      mAccounts.add(data);
    }
    if (mSelectedAccount == null) {
      mSelectedAccount = (AccountData) mAccountSpinner.getItemAtPosition(0);
    }
    // Update the account spinner
    mAccountAdapter.notifyDataSetChanged();
  }

  /**
   * Obtain the AuthenticatorDescription for a given account type.
   * 
   * @param type
   *          The account type to locate.
   * @param dictionary
   *          An array of AuthenticatorDescriptions, as returned by
   *          AccountManager.
   * @return The description for the specified account type.
   */
  private static AuthenticatorDescription getAuthenticatorDescription(
      String type, AuthenticatorDescription[] dictionary) {
    for (int i = 0; i < dictionary.length; i++) {
      if (dictionary[i].type.equals(type)) {
        return dictionary[i];
      }
    }
    // No match found
    throw new RuntimeException("Unable to find matching authenticator");
  }

  /**
   * Update account selection. If NO_ACCOUNT is selected, then we prohibit
   * inserting new contacts.
   */
  private void updateAccountSelection() {
    // Read current account selection
    mSelectedAccount = (AccountData) mAccountSpinner.getSelectedItem();
  }

  /**
   * A container class used to repreresent all known information about an
   * account.
   */
  private class AccountData {
    private String mName;
    private String mType;
    private CharSequence mTypeLabel;
    private Drawable mIcon;

    /**
     * @param name
     *          The name of the account. This is usually the user's email
     *          address or username.
     * @param description
     *          The description for this account. This will be dictated by the
     *          type of account returned, and can be obtained from the system
     *          AccountManager.
     */
    public AccountData(String name, AuthenticatorDescription description) {
      mName = name;
      if (description != null) {
        mType = description.type;

        // The type string is stored in a resource, so we need to convert it
        // into something
        // human readable.
        String packageName = description.packageName;
        PackageManager pm = getPackageManager();

        if (description.labelId != 0) {
          mTypeLabel = pm.getText(packageName, description.labelId, null);
          if (mTypeLabel == null) {
            throw new IllegalArgumentException(
                "LabelID provided, but label not found");
          }
        } else {
          mTypeLabel = "";
        }

        if (description.iconId != 0) {
          mIcon = pm.getDrawable(packageName, description.iconId, null);
          if (mIcon == null) {
            throw new IllegalArgumentException(
                "IconID provided, but drawable not " + "found");
          }
        } else {
          mIcon = getResources().getDrawable(
              android.R.drawable.sym_def_app_icon);
        }
      }
    }

    public String getName() {
      return mName;
    }

    public String getType() {
      return mType;
    }

    public CharSequence getTypeLabel() {
      return mTypeLabel;
    }

    public Drawable getIcon() {
      return mIcon;
    }

    public String toString() {
      return mName;
    }
  }

  /**
   * Custom adapter used to display account icons and descriptions in the
   * account spinner.
   */
  private class AccountAdapter extends ArrayAdapter<AccountData> {
    public AccountAdapter(Context context, ArrayList<AccountData> accountData) {
      super(context, android.R.layout.simple_spinner_item, accountData);
      setDropDownViewResource(R.layout.account_entry);
    }

    public View getDropDownView(int position, View convertView, ViewGroup parent) {
      // Inflate a view template
      if (convertView == null) {
        LayoutInflater layoutInflater = getLayoutInflater();
        convertView = layoutInflater.inflate(R.layout.account_entry, parent,
            false);
      }
      TextView firstAccountLine = (TextView) convertView
          .findViewById(R.id.firstAccountLine);
      TextView secondAccountLine = (TextView) convertView
          .findViewById(R.id.secondAccountLine);
      ImageView accountIcon = (ImageView) convertView
          .findViewById(R.id.accountIcon);

      // Populate template
      AccountData data = getItem(position);
      firstAccountLine.setText(data.getName());
      secondAccountLine.setText(data.getTypeLabel());
      Drawable icon = data.getIcon();
      if (icon == null) {
        icon = getResources().getDrawable(android.R.drawable.ic_menu_search);
      }
      accountIcon.setImageDrawable(icon);
      return convertView;
    }
  }

  class ShowDirResultsRunnable extends LDAPResultRunnable {
    public Context c;

    public ShowDirResultsRunnable(Context c) {
      this.c = c;
    }

    @Override
    public void run() {
      final String[] result = dirsResult;
      if (result != null) {
        showDirs(result);
      }
    }
  }

  private void showDirs(final String[] dirs) {
    mDirectoryAdapter.clear();
    for (String dir : dirs) {
      mDirectoryAdapter.add(dir);
    }
  }

  private ServiceConnection mServiceConnection = new ServiceConnection() {

    @Override
    public void onServiceDisconnected(ComponentName name) {
    }

    @Override
    public void onServiceConnected(ComponentName name, IBinder service) {
      mBinder = (LDAPService.LDAPBinder) service;
      mBinder.setActivityCallBackHandler(messageHandler);
      mBinder.setRunnable(new ShowDirResultsRunnable(mContext));
      for (Account acc : AccountManager.get(mContext).getAccounts()) {
        if (acc.name.equals(mSelectedAccount.getName())
            && acc.type.equals(mSelectedAccount.getType())) {
          instance = new ServerInstance(AccountManager.get(mContext), acc);
          break;
        }
      }
      mBinder.searchDirs(instance);
    }
  };
}
