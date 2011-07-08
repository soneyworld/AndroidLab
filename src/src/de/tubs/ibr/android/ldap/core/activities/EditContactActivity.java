package de.tubs.ibr.android.ldap.core.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.accounts.OnAccountsUpdateListener;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.provider.ContactsContract.RawContacts;
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
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.AdapterView.OnItemSelectedListener;
import java.util.ArrayList;
import de.tubs.ibr.android.ldap.R;
import de.tubs.ibr.android.ldap.auth.ServerInstance;
import de.tubs.ibr.android.ldap.provider.LDAPResultRunnable;
import de.tubs.ibr.android.ldap.provider.LDAPService;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
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
  private int mStatus;
  private ArrayList<AccountData> mAccounts;
  private ArrayAdapter<String> mDirectoryAdapter;
  private AccountData mSelectedAccount;
  private LDAPService.LDAPBinder mBinder;
  private final Handler messageHandler = new Handler();
  private ServerInstance instance;
  private Context mContext;
  private AccountAdapter mAccountAdapter;
  /**
   * If a {@link RawContacts} Entry should be edited, the rawContactId holds the
   * loaded id, otherwise, it is -1
   */
  private int mRawContactId;

  /* UI elements */
  private Spinner mAccountSpinner;
  private TextView mAccountTextView;
  private Spinner mDirectorySpinner;
  private TextView mDirectoryTextView;
  private EditText mCommonNameEditText;
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
  
  private TextView mAdditionalNameInfoTextView;
  private LinearLayout mAdditionalNameInfoLinearLayout;
  private TextView mAdditionalContactInfoTextView;
  private LinearLayout mAdditionalContactInfoLinearLayout;
  private TextView mAdditionalAddressInfoTextView;
  private LinearLayout mAdditionalAddressInfoLinearLayout;
  private TextView mAdditionalInfoTextView;
  private LinearLayout mAdditionalInfoLinearLayout;

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
    mAccountTextView = (TextView) findViewById(R.id.targetAccountTextView);
    mDirectorySpinner = (Spinner) findViewById(R.id.directorySpinner);
    mDirectoryTextView = (TextView) findViewById(R.id.targetLDAPDirectory);
    mCommonNameEditText = (EditText) findViewById(R.id.cnEditText);
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
    
    mAdditionalNameInfoTextView = (TextView) findViewById(R.id.additionalNameInfoTextView);
    mAdditionalNameInfoLinearLayout = (LinearLayout) findViewById(R.id.additionalNameInfoLinearLayout);
    mAdditionalContactInfoTextView = (TextView) findViewById(R.id.additionalContactInfoTextView);
    mAdditionalContactInfoLinearLayout = (LinearLayout) findViewById(R.id.additionalContactInfoLinearLayout);
    mAdditionalAddressInfoTextView = (TextView) findViewById(R.id.additionalAddressInfoTextView);
    mAdditionalAddressInfoLinearLayout = (LinearLayout) findViewById(R.id.additionalAddressInfoLinearLayout);
    mAdditionalInfoTextView = (TextView) findViewById(R.id.additionalInfoTextView);
    mAdditionalInfoLinearLayout = (LinearLayout) findViewById(R.id.additionalInfoLinearLayout);

    final boolean hasIncomingState = savedInstanceState != null
        && savedInstanceState.containsKey(KEY_EDIT_STATE);

    if (Intent.ACTION_EDIT.equals(action) && !hasIncomingState) {
      setTitle("Edit Contact");
      mStatus = STATUS_EDIT;
      mContactActionButton.setText("Save");
      mRawContactId = intent.getExtras().getInt("_id");
      loadContactEntry(mRawContactId);
      mAccountSpinner.setEnabled(false);
      mAccountTextView.setEnabled(false);
      mDirectorySpinner.setEnabled(false);
      mDirectoryTextView.setEnabled(false);

    } else if (Intent.ACTION_INSERT.equals(action) && !hasIncomingState) {
      setTitle("Add Contact");
      mStatus = STATUS_INSERT;
      mContactActionButton.setText("Add");
      mRawContactId = -1;
      mAccountSpinner.setEnabled(true);
      mAccountTextView.setEnabled(true);
      mDirectorySpinner.setEnabled(true);
      mDirectoryTextView.setEnabled(true);
    }

    mContactActionButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        onActionButtonClicked();
      }
    });
    mContactRevertButton.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        onRevertButtonClicked(mRawContactId);
      }
    });
    
    mAdditionalNameInfoTextView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mAdditionalNameInfoLinearLayout.getVisibility() == LinearLayout.VISIBLE) {
          mAdditionalNameInfoLinearLayout.setVisibility(LinearLayout.GONE);
          mAdditionalNameInfoTextView.setText("Show additional name info");
        }
        else {
          mAdditionalNameInfoLinearLayout.setVisibility(LinearLayout.VISIBLE);
          mAdditionalNameInfoTextView.setText("Hide additional name info");
        }
      }
    });
    mAdditionalContactInfoTextView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mAdditionalContactInfoLinearLayout.getVisibility() == LinearLayout.VISIBLE) {
          mAdditionalContactInfoLinearLayout.setVisibility(LinearLayout.GONE);
          mAdditionalContactInfoTextView.setText("Show additional contact info");
        }
        else {
          mAdditionalContactInfoLinearLayout.setVisibility(LinearLayout.VISIBLE);
          mAdditionalContactInfoTextView.setText("Hide additional contact info");
        }
      }
    });
    mAdditionalAddressInfoTextView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mAdditionalAddressInfoLinearLayout.getVisibility() == LinearLayout.VISIBLE) {
          mAdditionalAddressInfoLinearLayout.setVisibility(LinearLayout.GONE);
          mAdditionalAddressInfoTextView.setText("Show additional address info");
        }
        else {
          mAdditionalAddressInfoLinearLayout.setVisibility(LinearLayout.VISIBLE);
          mAdditionalAddressInfoTextView.setText("Hide additional address info");
        }
      }
    });
    mAdditionalInfoTextView.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        if (mAdditionalInfoLinearLayout.getVisibility() == LinearLayout.VISIBLE) {
          mAdditionalInfoLinearLayout.setVisibility(LinearLayout.GONE);
          mAdditionalInfoTextView.setText("Show additional info");
        }
        else {
          mAdditionalInfoLinearLayout.setVisibility(LinearLayout.VISIBLE);
          mAdditionalInfoTextView.setText("Hide additional info");
        }
      }
    });

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
  private void onActionButtonClicked() {
    Log.v(TAG, "Save button clicked");
    if (mStatus == STATUS_INSERT) {
      Log.v(TAG, "mStatus = STATUS_INSERT");
      createContactEntry();
      finish();
    } else {
      Log.v(TAG, "mStatus = STATUS_EDIT");
      updateContactEntry();
      finish();
    }
  }

  /**
   * Creates a contact entry from the current UI values in the account named by
   * mSelectedAccount.
   */
  protected void createContactEntry() {
    // Get values from UI
    Bundle contactData = new Bundle();
    boolean syncCheckBoxValue = getValuesFromUI(contactData);
    Account account = getSelectedAccount();
    if (account != null) {
      if (syncCheckBoxValue) {
        ContactManager.saveNewLocallyAddedContactAndSync(contactData, account,
            mContext);
      } else {
        ContactManager.saveNewLocallyAddedContactAndDoNotSync(contactData,
            account, mContext);
      }
    }
  }

  /**
   * Collects all values from the UI and saves them in the given Bundle
   * 
   * @param b
   * @return true if the contact should be synchronized, otherwise false
   */
  private boolean getValuesFromUI(Bundle b) {
    b.putString(AttributeMapper.DN,
        (String) mDirectorySpinner.getSelectedItem());
    b.putString(AttributeMapper.LAST_NAME, mContactNameEditText.getText()
        .toString());
    b.putString(AttributeMapper.FULL_NAME, mCommonNameEditText.getText()
        .toString());
    b.putString(AttributeMapper.INITIALS, mInitialsEditText.getText()
        .toString());
    b.putString(AttributeMapper.TITLE, mTitleEditText.getText().toString());
    b.putString(AttributeMapper.DISPLAYNAME, mCommonNameEditText.getText()
        .toString());
    b.putString(AttributeMapper.FIRST_NAME, mContactFirstnameEditText.getText()
        .toString());
    b.putString(AttributeMapper.DESCRIPTION, mDescriptionEditText.getText()
        .toString());
    b.putString(AttributeMapper.PRIMARY_PHONE, mContactTelephoneEditText
        .getText().toString());
    b.putString(AttributeMapper.HOME_PHONE, mHomePhoneEditText.getText()
        .toString());
    b.putString(AttributeMapper.MOBILE_PHONE, mMobileEditText.getText()
        .toString());
    b.putString(AttributeMapper.PAGER, mPagerEditText.getText().toString());
    b.putString(AttributeMapper.FAX, mFacsimileEditText.getText().toString());
    b.putString(AttributeMapper.TELEX, mTelexEditText.getText().toString());
    b.putString(AttributeMapper.ISDN, miSDNEditText.getText().toString());
    b.putString(AttributeMapper.PRIMARY_MAIL, mContactEmailEditText.getText()
        .toString());
    b.putString(AttributeMapper.REGISTERED_ADDRESS, mRegAddressEditText
        .getText().toString());
    b.putString(AttributeMapper.STREET, mStreetEditText.getText().toString());
    b.putString(AttributeMapper.POST_OFFICE_BOX, mPostOfficeboxEditText
        .getText().toString());
    b.putString(AttributeMapper.POSTAL_CODE, mPostalCodeEditText.getText()
        .toString());
    b.putString(AttributeMapper.POSTAL_ADDRESS, mPostalAddressEditText
        .getText().toString());
    b.putString(AttributeMapper.HOME_ADDRESS, mHomePostalAddressEditText
        .getText().toString());
    b.putString(AttributeMapper.ORGANIZATION, mOrganizationEditText.getText()
        .toString());
    b.putString(AttributeMapper.BUSINESS_CATEGORY, mBusinessCategoryEditText
        .getText().toString());
    b.putString(AttributeMapper.DEPARTMENT_NUMBER, mDepartmentNumberEditText
        .getText().toString());
    b.putString(AttributeMapper.PHYSICAL_DELIVERY_OFFICE_NAME,
        mPhysicalDeliveryOfficeNameEditText.getText().toString());
    b.putString(AttributeMapper.UID, mUserIdEditText.getText().toString());
    b.putString(AttributeMapper.STATE, mStateEditText.getText().toString());
    b.putString(AttributeMapper.ORGANIZATION_UNIT, mOrganizationalUnitEditText
        .getText().toString());
    b.putString(AttributeMapper.SEE_ALSO, mWebSiteEditText.getText().toString());
    b.putString(AttributeMapper.ROOM_NUMBER, mRoomNumberEditText.getText()
        .toString());
    b.putString(AttributeMapper.PREFERRED_LANGUAGE, mPrefLanguageEditText
        .getText().toString());
    String key;
    for (Object k : b.keySet().toArray()) {
      key = (String) k;
      String value = b.getString(key);
      if (value == null || value.length()==0) {
        b.remove(key);
      }
    }
    return mSyncCheckBox.isChecked();
  }

  protected void loadContactEntry(int id) {

    Bundle contactData = ContactManager.loadContact(id, mContext);
    mContactNameEditText.setText(contactData.getString("sn"));
    mCommonNameEditText.setText(contactData.getString("cn"));
    mInitialsEditText.setText(contactData.getString("initials"));
    mContactFirstnameEditText.setText(contactData.getString("givenName"));
    mTitleEditText.setText(contactData.getString("title"));
    mDescriptionEditText.setText(contactData.getString("description"));
    mContactTelephoneEditText.setText(contactData.getString("telephoneNumber"));
    mHomePhoneEditText.setText(contactData.getString("homePhone"));
    mMobileEditText.setText(contactData.getString("mobile"));
    mPagerEditText.setText(contactData.getString("pager"));
    mFacsimileEditText.setText(contactData
        .getString("facsimileTelephoneNumber"));
    mTelexEditText.setText(contactData.getString("telexNumber"));
    miSDNEditText.setText(contactData.getString("internationaliSDNNumber"));
    mContactEmailEditText.setText(contactData.getString("mail"));
    mRegAddressEditText.setText(contactData.getString("registeredAddress"));
    mStreetEditText.setText(contactData.getString("street"));
    mPostOfficeboxEditText.setText(contactData.getString("postOfficeBox"));
    mPostalCodeEditText.setText(contactData.getString("postalCode"));
    mPostalAddressEditText.setText(contactData.getString("postalAddress"));
    mHomePostalAddressEditText.setText(contactData
        .getString("homePostalAddress"));
    mOrganizationEditText.setText(contactData.getString("o"));
    mBusinessCategoryEditText
        .setText(contactData.getString("businessCategory"));
    mDepartmentNumberEditText
        .setText(contactData.getString("departmentNumber"));
    mRoomNumberEditText.setText(contactData.getString("roomNumber"));
    mUserIdEditText.setText(contactData.getString("uid"));
    mStateEditText.setText(contactData.getString("st"));
    mOrganizationalUnitEditText.setText(contactData.getString("ou"));
    mPrefLanguageEditText.setText(contactData.getString("preferredLanguage"));
    mWebSiteEditText.setText(contactData.getString("seeAlso"));

  }

  protected void updateContactEntry() {
    Bundle contact = new Bundle();
    getValuesFromUI(contact);
    Account account = getSelectedAccount();
    if (account != null) {
      ContactManager.saveLocallyEditedContact(mRawContactId, contact, account,
          this);
    }
  }
  
  protected void onRevertButtonClicked(int id) {
    if (mStatus == STATUS_INSERT) {
      revertChanges();
    }
    else {
      loadContactEntry(id);
    }
  }
  
  protected void revertChanges() {
    mContactNameEditText.setText("");
    mCommonNameEditText.setText("");
    mInitialsEditText.setText("");
    mContactFirstnameEditText.setText("");
    mTitleEditText.setText("");
    mDescriptionEditText.setText("");
    mContactTelephoneEditText.setText("");
    mHomePhoneEditText.setText("");
    mMobileEditText.setText("");
    mPagerEditText.setText("");
    mFacsimileEditText.setText("");
    mTelexEditText.setText("");
    miSDNEditText.setText("");
    mContactEmailEditText.setText("");
    mRegAddressEditText.setText("");
    mStreetEditText.setText("");
    mPostOfficeboxEditText.setText("");
    mPostalCodeEditText.setText("");
    mPostalAddressEditText.setText("");
    mHomePostalAddressEditText.setText("");
    mOrganizationEditText.setText("");
    mBusinessCategoryEditText
        .setText("");
    mDepartmentNumberEditText
        .setText("");
    mRoomNumberEditText.setText("");
    mUserIdEditText.setText("");
    mStateEditText.setText("");
    mOrganizationalUnitEditText.setText("");
    mPrefLanguageEditText.setText("");
    mWebSiteEditText.setText("");
  }

  /**
   * @return the selected Account or null if no Account is selected
   */
  private Account getSelectedAccount() {
    Account[] accounts = AccountManager.get(this).getAccountsByType(
        ACCOUNT_TYPE);
    for (Account a : accounts) {
      if (a.name.equals(((AccountData) mAccountSpinner.getSelectedItem())
          .getName())) {
        return a;
      }
    }
    return null;
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
