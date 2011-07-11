package de.tubs.ibr.android.ldap.core.test;

import de.tubs.ibr.android.ldap.core.BatchOperation;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.core.ContactUtils;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.net.Uri;
import android.os.Bundle;
import android.test.AndroidTestCase;

public class CreateUpdateContact extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	private Bundle createDefaultContactBundle() {
		Bundle b = new Bundle();
		b.putString(AttributeMapper.DN, "TestDN");
		b.putString(AttributeMapper.LAST_NAME, "TestLastName");
		b.putString(AttributeMapper.FULL_NAME, "TestFullName");
		b.putString(AttributeMapper.INITIALS, "TestInitials");
		b.putString(AttributeMapper.TITLE, "TestTitle");
		b.putString(AttributeMapper.DISPLAYNAME, "TestDisplayname");
		b.putString(AttributeMapper.FIRST_NAME, "TestFirstName");
		b.putString(AttributeMapper.DESCRIPTION, "TestDescription");
		b.putString(AttributeMapper.PRIMARY_PHONE, "0175612351");
		b.putString(AttributeMapper.HOME_PHONE, "");
		b.putString(AttributeMapper.MOBILE_PHONE, "");
		b.putString(AttributeMapper.PAGER, "");
		b.putString(AttributeMapper.FAX, "");
		b.putString(AttributeMapper.TELEX, "");
		b.putString(AttributeMapper.ISDN, "");
		b.putString(AttributeMapper.PRIMARY_MAIL, "");
		b.putString(AttributeMapper.REGISTERED_ADDRESS, "");
		b.putString(AttributeMapper.STREET, "Hans-Meyer-Stra�e");
		b.putString(AttributeMapper.POST_OFFICE_BOX, "");
		b.putString(AttributeMapper.POSTAL_CODE, "31319");
		b.putString(AttributeMapper.POSTAL_ADDRESS, "");
		b.putString(AttributeMapper.HOME_ADDRESS, "");
		b.putString(AttributeMapper.ORGANIZATION, "VW");
		b.putString(AttributeMapper.BUSINESS_CATEGORY,
				"Human Ressource Management");
		b.putString(AttributeMapper.DEPARTMENT_NUMBER, "21");
		b.putString(AttributeMapper.PHYSICAL_DELIVERY_OFFICE_NAME, "");
		b.putString(AttributeMapper.UID, "12");
		b.putString(AttributeMapper.STATE, "Niedersachsen");
		b.putString(AttributeMapper.ORGANIZATION_UNIT, "Human Ressources");
		b.putString(AttributeMapper.SEE_ALSO, "www.volkswagen.de");
		b.putString(AttributeMapper.ROOM_NUMBER, "1");
		b.putString(AttributeMapper.PREFERRED_LANGUAGE, "German");

		return b;
	}

	private Account createDefaultAccount() {
		Account[] accounts = AccountManager.get(getContext()).getAccounts();
		return accounts[0];
	}

	private BatchOperation createDefaultBatchOperation() {
		BatchOperation batch = new BatchOperation(getContext(), null);
		return batch;
	}

	public void testCreateFullContactAccountBundleBatchOperationUriUri(final Account account, Bundle b,
		      BatchOperation batch, Uri rawContactUri, Uri dataUri) {
		ContactManager.saveNewLocallyAddedContactAndSync(b, account, getContext());
	}
}
