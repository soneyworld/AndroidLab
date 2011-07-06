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
import de.tubs.ibr.android.ldap.core.ContactManager;

public class EditContactActivity extends Activity implements
    OnAccountsUpdateListener {

  /* Data structures */
  public static final String TAG = "ContactsAdder";
  public static final String ACCOUNT_NAME = "LDAP";
  public static final String ACCOUNT_TYPE = "de.tubs.ibr.ldap";
  private static final int STATUS_EDIT = 0;
  private static final int STATUS_INSERT = 1;
  private static final String KEY_EDIT_STATE = "state";
  private static int mStatus;
  private ArrayList<AccountData> mAccounts;
  private ArrayAdapter<String> mDirectoryAdapter;
  private AccountData mSelectedAccount;
  private LDAPService.LDAPBinder mBinder;
  private final Handler messageHandler = new Handler();
  private ServerInstance instance;
  private Context mContext;
  private AccountAdapter mAccountAdapter;

  /* UI elements */
  private Spinner mAccountSpinner;
  private Spinner mDirectorySpinner;
  private EditText mCommonNameEditText;
  private EditText mDisplaynameEditText;
  private EditText mContactFirstnameEditText;
  private EditText mContactNameEditText;
  private EditText mInitialsEditText;
  private EditText mTitleEditText;
  private EditText mUserIdEditText;
  private EditText mContactTelephoneEditText;
  private EditText mMobileEditText;
  private EditText mHomePhoneEditText;
  private EditText mPagerEditText;
  private EditText mFacsimileEditText;
  private EditText mTelexEditText;
  private EditText miSDNEditText;
  private EditText mContactEmailEditText;
  private EditText mDescriptionEditText;
  private EditText mRegAddressEditText;
  private EditText mStreetEditText;
  private EditText mPostalCodeEditText;
  private EditText mPostalAddressEditText;
  private EditText mPostOfficeboxEditText;
  private EditText mPhysicalDeliveryOfficeNameEditText;
  private EditText mBusinessCategoryEditText;
  private EditText mDepartmentNumberEditText;
  private EditText mHomePostalAddressEditText;
  private EditText mStateEditText;
  private EditText mOrganizationEditText;
  private EditText mOrganizationalUnitEditText;
  private EditText mRoomNumberEditText;
  private EditText mPrefLanguageEditText;
  private EditText mWebSiteEditText;
  private CheckBox mSyncCheckBox;
  private Button mContactActionButton;
  private Button mContactRevertButton;

  /**
   * Called when the activity is first created. Responsible for initializing the
   * UI.
   * 
   * @return
   */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    Log.v(TAG, "Activity State: onCreate()");
    super.onCreate(savedInstanceState);

    final Intent intent = getIntent();
    final String action = intent.getAction();

    setContentView(R.layout.layout_editcontact_view);
    mContext = this;

    // Obtain handles to UI objects
    mAccountSpinner = (Spinner) findViewById(R.id.accountSpinner);
    mDirectorySpinner = (Spinner) findViewById(R.id.directorySpinner);
    mCommonNameEditText = (EditText) findViewById(R.id.cnEditText);
    mDisplaynameEditText = (EditText) findViewById(R.id.displayNameEditText);
    mContactFirstnameEditText = (EditText) findViewById(R.id.contactFirstnameEditText);
    mContactNameEditText = (EditText) findViewById(R.id.contactNameEditText);
    mInitialsEditText = (EditText) findViewById(R.id.initialsEditText);
    mTitleEditText = (EditText) findViewById(R.id.titleEditText);
    mUserIdEditText = (EditText) findViewById(R.id.userIdEditText);
    mContactTelephoneEditText = (EditText) findViewById(R.id.contactTelephoneEditText);
    mMobileEditText = (EditText) findViewById(R.id.mobileEditText);
    mHomePhoneEditText = (EditText) findViewById(R.id.homePhoneEditText);
    mPagerEditText = (EditText) findViewById(R.id.pagerEditText);
    mFacsimileEditText = (EditText) findViewById(R.id.facsimileEditText);
    mTelexEditText = (EditText) findViewById(R.id.telexEditText);
    miSDNEditText = (EditText) findViewById(R.id.iSDNEditText);
    mContactEmailEditText = (EditText) findViewById(R.id.contactEmailEditText);
    mDescriptionEditText = (EditText) findViewById(R.id.descriptionEditText);
    mRegAddressEditText = (EditText) findViewById(R.id.regAddressEditText);
    mStreetEditText = (EditText) findViewById(R.id.streetEditText);
    mPostalCodeEditText = (EditText) findViewById(R.id.postalCodeEditText);
    mPostalAddressEditText = (EditText) findViewById(R.id.postalAddressEditText);
    mPostOfficeboxEditText = (EditText) findViewById(R.id.postOfficeboxEditText);
    mPhysicalDeliveryOfficeNameEditText = (EditText) findViewById(R.id.officeNameEditText);
    mBusinessCategoryEditText = (EditText) findViewById(R.id.businessCategoryEditText);
    mDepartmentNumberEditText = (EditText) findViewById(R.id.departmentNumberEditText);
    mHomePostalAddressEditText = (EditText) findViewById(R.id.homePostalAddressEditText);
    mStateEditText = (EditText) findViewById(R.id.stateEditText);
    mOrganizationEditText = (EditText) findViewById(R.id.organizationEditText);
    mOrganizationalUnitEditText = (EditText) findViewById(R.id.organizationalUnitEditText);
    mRoomNumberEditText = (EditText) findViewById(R.id.roomNumberEditText);
    mPrefLanguageEditText = (EditText) findViewById(R.id.prefLanguageEditText);
    mWebSiteEditText = (EditText) findViewById(R.id.webSiteEditText);
    mSyncCheckBox = (CheckBox) findViewById(R.id.syncCheckBox);
    mContactActionButton = (Button) findViewById(R.id.btn_action);
    mContactRevertButton = (Button) findViewById(R.id.btn_revert);

    final boolean hasIncomingState = savedInstanceState != null
        && savedInstanceState.containsKey(KEY_EDIT_STATE);

    if (Intent.ACTION_EDIT.equals(action) && !hasIncomingState) {
      setTitle("Edit Contact");
      mStatus = STATUS_EDIT;
      mContactActionButton.setText("Save");

      // Read initial state from database
      // TODO
    } else if (Intent.ACTION_INSERT.equals(action) && !hasIncomingState) {
      setTitle("Add Contact");
      mStatus = STATUS_INSERT;
      mContactActionButton.setText("Add");
    }

    // Prepare model for account spinner
    mAccounts = new ArrayList<AccountData>();
    mAccountAdapter = new AccountAdapter(this, mAccounts);
    mAccountSpinner.setAdapter(mAccountAdapter);

    mDirectoryAdapter = new ArrayAdapter<String>(this,
        android.R.layout.simple_spinner_item);
    mDirectorySpinner.setAdapter(mDirectoryAdapter);
    mSyncCheckBox.setChecked(true);
    // mContactExportButton.setVisibility(Button.GONE);

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
    // mContactSaveButton.setOnClickListener(new View.OnClickListener() {
    // public void onClick(View v) {
    // onSaveButtonClicked();
    // }
    // });
    final Intent intent_service = new Intent(this, LDAPService.class);
    getApplicationContext().bindService(intent_service, mServiceConnection,
        Context.BIND_AUTO_CREATE);
  }

  /**
   * Actions for when the Save button is clicked. Creates a contact entry and
   * terminates the activity.
   */
  private void onActionButtonClicked(int status) {
    Log.v(TAG, "Save button clicked");
    if (status == 1) {
      createContactEntry();
      finish();
    } else {
      // TODO
    }
  }

  /**
   * Creates a contact entry from the current UI values in the account named by
   * mSelectedAccount.
   */
  protected void createContactEntry() {
    // Get values from UI
    Bundle contactData = new Bundle();

    String accountSpinnerValue = (String) mAccountSpinner.getSelectedItem();
    String directorySpinnerValue = (String) mDirectorySpinner.getSelectedItem();
    String commonNameValue = mCommonNameEditText.getText().toString();
    String displaynameValue = mDisplaynameEditText.getText().toString();
    String contactFirstnameValue = mContactFirstnameEditText.getText()
        .toString();
    String contactNameValue = mContactNameEditText.getText().toString();
    String initialsValue = mInitialsEditText.getText().toString();
    String titleValue = mTitleEditText.getText().toString();
    String userIdValue = mUserIdEditText.getText().toString();
    String telephoneValue = mContactTelephoneEditText.getText().toString();
    String mobileValue = mMobileEditText.getText().toString();
    String homePhoneValue = mHomePhoneEditText.getText().toString();
    String pagerValue = mPagerEditText.getText().toString();
    String facsimileValue = mFacsimileEditText.getText().toString();
    String telexValue = mTelexEditText.getText().toString();
    String iSDNValue = miSDNEditText.getText().toString();
    String contactEmailValue = mContactEmailEditText.getText().toString();
    String descriptionValue = mDescriptionEditText.getText().toString();
    String regAddressValue = mRegAddressEditText.getText().toString();
    String streetValue = mStreetEditText.getText().toString();
    String postalCodeValue = mPostalCodeEditText.getText().toString();
    String postalAddressValue = mPostalAddressEditText.getText().toString();
    String postOfficeboxValue = mPostOfficeboxEditText.getText().toString();
    String physicalDeliveryOfficeNameValue = mPhysicalDeliveryOfficeNameEditText
        .getText().toString();
    String businessCategoryValue = mBusinessCategoryEditText.getText()
        .toString();
    String departmentNumberValue = mDepartmentNumberEditText.getText()
        .toString();
    String homePostalAddressValue = mHomePostalAddressEditText.getText()
        .toString();
    String stateValue = mStateEditText.getText().toString();
    String organizationValue = mOrganizationEditText.getText().toString();
    String organizationalUnitValue = mOrganizationalUnitEditText.getText()
        .toString();
    String roomNummberValue = mRoomNumberEditText.getText().toString();
    String prefLanguageValue = mPrefLanguageEditText.getText().toString();
    String webSiteValue = mWebSiteEditText.getText().toString();
    boolean syncCheckBoxValue = mSyncCheckBox.isChecked();

    contactData.putString("sn", contactNameValue);
    contactData.putString("cn", commonNameValue);
    contactData.putString("initials", initialsValue);
    contactData.putString("title", titleValue);
    contactData.putString("displayName", displaynameValue);
    contactData.putString("givenName", contactFirstnameValue);
    contactData.putString("description", descriptionValue);
    contactData.putString("telephoneNumber", telephoneValue);
    contactData.putString("homePhone", homePhoneValue);
    contactData.putString("mobile", mobileValue);
    contactData.putString("pager", pagerValue);
    contactData.putString("facsimileTelephoneNumber", facsimileValue);
    contactData.putString("telexNumber", telexValue);
    contactData.putString("internationaliSDNNumber", iSDNValue);
    contactData.putString("mail", contactEmailValue);
    contactData.putString("street", streetValue);
    contactData.putString("postOfficeBox", postOfficeboxValue);
    contactData.putString("postalCode", postalCodeValue);
    contactData.putString("postalAddress", postalAddressValue);
    contactData.putString("homePostalAddress", homePostalAddressValue);
    contactData.putString("organization", organizationValue);
    contactData.putString("businessCategory", businessCategoryValue);
    contactData.putString("uid", userIdValue);
    contactData.putString("st", stateValue);
    contactData.putString("ou", organizationalUnitValue);
    contactData.putString("seeAlso", webSiteValue);
    Account[] accounts = AccountManager.get(this).getAccountsByType(
        ACCOUNT_TYPE);
    for (Account a : accounts) {
      if (a.name.equals(((AccountData) mAccountSpinner.getSelectedItem())
          .getName())) {
        if (syncCheckBoxValue) {
          ContactManager.saveNewLocallyAddedContactAndSync(contactData, a,
              mContext);
        } else {
          ContactManager.saveNewLocallyAddedContactAndDoNotSync(contactData, a,
              mContext);
        }
        break;
      }
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
