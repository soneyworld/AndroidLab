package de.tubs.ibr.android.ldap.sync.test;

import de.tubs.ibr.android.ldap.sync.LDAPSyncService;
import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OperationCanceledException;
import android.test.AndroidTestCase;

public class LDAPSyncServiceTest extends AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

	public void testPerformSync() {
	    AccountManager accountManager = AccountManager.get(getContext());
	    for (Account account : accountManager.getAccounts()) {
	    	try {
				LDAPSyncService.performSync(getContext(), account, null, null, null, null);
			} catch (OperationCanceledException e) {
				e.printStackTrace();
			}	
		}
		
	}

}
