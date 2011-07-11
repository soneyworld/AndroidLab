package de.tubs.ibr.android.ldap.core.test;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class UpdateDB extends android.test.AndroidTestCase {

	protected void setUp() throws Exception {
		super.setUp();
		cleanUpDatabase();
	}

	protected void tearDown() throws Exception {
		cleanUpDatabase();
		super.tearDown();

	}

	private void cleanUpDatabase() {
		LinkedHashMap<Integer, Bundle> contacts = ContactManager
				.loadContactList(getContext());
		for (Integer id : contacts.keySet()) {
			ContactManager.deleteLocalContact(id, getContext()
					.getContentResolver());
		}
	}

	@SuppressWarnings("deprecation")
	private Bundle createDefaultContact(String postfix) {
		Bundle result = new Bundle();
		for (String key : AttributeMapper.getContactAttrs()) {
			if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME))
				result.putString(key, key + postfix);
		}
		return result;
	}

	private int localInsert(Bundle b, Account account) {
		String searchForName = b.getString(AttributeMapper.FULL_NAME);
		ContactManager.saveNewLocallyAddedContactAndSync(b, account,
				getContext());
		LinkedHashMap<Integer, Bundle> contacts = ContactManager
				.loadContactList(getContext());
		int id = -1;
		boolean inserted_found = false;
		for (Entry<Integer, Bundle> entry : contacts.entrySet()) {
			String name = entry.getValue().getString(AttributeMapper.FULL_NAME);
			if (name != null && name.equalsIgnoreCase(searchForName)) {
				if (inserted_found) {
					fail("Multiple Entries found!");
				} else {
					inserted_found = true;
					id = entry.getKey();
				}
			}
		}
		assertEquals(inserted_found, true);
		return id;
	}

	@SuppressWarnings("deprecation")
	public void testCreateUpdateDeleteContact() {
		cleanUpDatabase();
		Account[] accounts = AccountManager.get(getContext()).getAccounts();
		for (Account account : accounts) {
			Bundle insertContact = createDefaultContact("1");
			int rawid = localInsert(insertContact, account);
			Bundle insertedContact = ContactManager.loadContact(rawid,
					getContext());
			checkContactAttributes(insertContact, insertedContact);
			for (String key : AttributeMapper.getDescAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					insertedContact.remove(key);
					insertedContact.putString(key, key + "2");
				}
				checkUpdatedContact(account, rawid, insertedContact);
			}
			for (String key : AttributeMapper.getNameSubAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					insertedContact.remove(key);
					insertedContact.putString(key, key + "2");
				}
			}
			checkUpdatedContact(account, rawid, insertedContact);
			for (String key : AttributeMapper.getPostalHomeAddressAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					insertedContact.remove(key);
					insertedContact.putString(key, key + "2");
				}
			}
			checkUpdatedContact(account, rawid, insertedContact);
			for (String key : AttributeMapper.getPostalWorkAddressAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					insertedContact.remove(key);
					insertedContact.putString(key, key + "2");
				}
			}
			checkUpdatedContact(account, rawid, insertedContact);

			ContactManager.deleteLocalContact(rawid, getContext()
					.getContentResolver());
			cleanUpDatabase();
		}
	}

	private void checkUpdatedContact(Account account, int rawid,
			Bundle insertedContact) {
		ContactManager.saveLocallyEditedContact(rawid, insertedContact,
				account, getContext());
		Bundle updatedcontact = ContactManager.loadContact(rawid, getContext());
		checkContactAttributes(insertedContact, updatedcontact);
	}

	public void testCreateSingleUpdateDeleteContact() {
		cleanUpDatabase();
		Account[] accounts = AccountManager.get(getContext()).getAccounts();
		for (Account account : accounts) {
			Bundle insertContact = createDefaultContact("1");
			int rawid = localInsert(insertContact, account);
			Bundle insertedContact = ContactManager.loadContact(rawid,
					getContext());
			checkContactAttributes(insertContact, insertedContact);
			String updatedvalue = " name ";
			insertedContact.remove(AttributeMapper.STREET);
			insertedContact.putString(AttributeMapper.STREET, updatedvalue);
			checkUpdatedContact(account, rawid, insertedContact);
			ContactManager.deleteLocalContact(rawid, getContext()
					.getContentResolver());
			cleanUpDatabase();
		}
	}

	@SuppressWarnings("deprecation")
	private void checkContactAttributes(Bundle insertContact,
			Bundle insertedContact) {
		for (String key : AttributeMapper.getContactAttrs()) {
			if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
				String o = insertContact.getString(key);
				String n = insertedContact.getString(key);
				assertEquals(o, n);
			}
		}
	}

	public void testLocalInsertContact() {
		cleanUpDatabase();
		Account[] accounts = AccountManager.get(getContext()).getAccounts();
		for (Account account : accounts) {
			Bundle insertContact = createDefaultContact("1");
			int rawid = localInsert(insertContact, account);
			Bundle insertedContact = ContactManager.loadContact(rawid,
					getContext());
			String dirty = insertedContact
					.getString(ContactManager.LOCAL_ACCOUNT_DIRTY_KEY);
			assertEquals(dirty, "1");
			checkContactAttributes(insertContact, insertedContact);
			cleanUpDatabase();
		}
	}

}
