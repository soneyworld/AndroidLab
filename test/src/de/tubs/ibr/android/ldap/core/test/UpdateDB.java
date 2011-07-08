package de.tubs.ibr.android.ldap.core.test;

import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.os.Bundle;
import de.tubs.ibr.android.ldap.core.BatchOperation;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class UpdateDB extends android.test.AndroidTestCase {

	private int rawid = -1;

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

	@SuppressWarnings("deprecation")
	public void testCreateUpdateDeleteContact() {
		cleanUpDatabase();
		Account[] accounts = AccountManager.get(getContext()).getAccounts();
		for (Account account : accounts) {
			Bundle insertContact = createDefaultContact("");
			ContactManager.saveNewLocallyAddedContactAndSync(insertContact,
					account, getContext());
			LinkedHashMap<Integer, Bundle> contacts = ContactManager
					.loadContactList(getContext());
			boolean inserted_found = false;
			for (Entry<Integer, Bundle> entry : contacts.entrySet()) {
				String name = entry.getValue().getString(
						AttributeMapper.FULL_NAME);
				if (name != null
						&& name.equalsIgnoreCase(AttributeMapper.FULL_NAME)) {
					if (inserted_found) {
						 fail("Multiple Entries found!");
					} else {
						inserted_found = true;
						rawid = entry.getKey();
					}
				}
			}
			Bundle insertedContact = ContactManager.loadContact(rawid,
					getContext());
			for (String key : AttributeMapper.getContactAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					String o = insertContact.getString(key);
					String n = insertedContact.getString(key);
					assertEquals(o, n);
				}
			}

			BatchOperation batch = new BatchOperation(getContext(),
					getContext().getContentResolver());
			for (String key : AttributeMapper.getContactAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					insertedContact.remove(key);
					insertedContact.putString(key, key + "2");
				}
			}
			// Uri dataUri = Data.CONTENT_URI;

			ContactManager.saveLocallyEditedContact(rawid, insertedContact,
					account, getContext());
			batch.execute();
			Bundle updatedcontact = ContactManager.loadContact(rawid,
					getContext());
			for (String key : AttributeMapper.getContactAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					String o = insertedContact.getString(key);
					String n = updatedcontact.getString(key);
					assertEquals(o, n);
				}
			}
			ContactManager.deleteLocalContact(rawid, getContext()
					.getContentResolver());
			cleanUpDatabase();
		}
	}

	@SuppressWarnings("deprecation")
	public void testLocalInsertContact() {
		cleanUpDatabase();
		Account[] accounts = AccountManager.get(getContext()).getAccounts();
		for (Account account : accounts) {
			Bundle insertContact = createDefaultContact("");
			ContactManager.saveNewLocallyAddedContactAndSync(insertContact,
					account, getContext());
			LinkedHashMap<Integer, Bundle> contacts = ContactManager
					.loadContactList(getContext());
			boolean inserted_found = false;
			for (Entry<Integer, Bundle> entry : contacts.entrySet()) {
				String name = entry.getValue().getString(
						AttributeMapper.FULL_NAME);
				if (name != null
						&& name.equalsIgnoreCase(AttributeMapper.FULL_NAME)) {
					if (inserted_found) {
						 fail("Multiple Entries found!");
					} else {
						inserted_found = true;
						rawid = entry.getKey();
					}
				}
			}
			Bundle insertedContact = ContactManager.loadContact(rawid,
					getContext());
			String dirty = insertedContact.getString(ContactManager.LOCAL_ACCOUNT_DIRTY_KEY);
			assertEquals(dirty, "1");
			for (String key : AttributeMapper.getContactAttrs()) {
				if (!key.equalsIgnoreCase(AttributeMapper.DISPLAYNAME)) {
					String o = insertContact.getString(key);
					String n = insertedContact.getString(key);
					assertEquals(o, n);

				}
			}
			cleanUpDatabase();
		}
	}

}
