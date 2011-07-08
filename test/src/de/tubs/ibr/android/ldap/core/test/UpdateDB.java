package de.tubs.ibr.android.ldap.core.test;

import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract.Data;
import de.tubs.ibr.android.ldap.core.BatchOperation;
import de.tubs.ibr.android.ldap.core.ContactManager;
import de.tubs.ibr.android.ldap.core.ContactUtils;
import de.tubs.ibr.android.ldap.sync.AttributeMapper;

public class UpdateDB extends android.test.AndroidTestCase {

	private int rawid;

	protected void setUp() throws Exception {
		super.setUp();
		rawid = 7;
	}

	protected void tearDown() throws Exception {
		super.tearDown();
	}

//	public void testCreateUpdateBatch() {
//		
//	}

	public void testUpdateInitials() {
		Bundle contact = ContactManager.loadContact(rawid, getContext());
		BatchOperation batch = new BatchOperation(getContext(), getContext()
				.getContentResolver());
		contact.remove(AttributeMapper.INITIALS);
		contact.putString(AttributeMapper.INITIALS, " test ");
		Uri dataUri = Data.CONTENT_URI;
		ContactUtils.updateInitials(contact, batch, dataUri, rawid);
		batch.execute();
		Bundle updatedcontact = ContactManager.loadContact(rawid, getContext());
		assertEquals(contact.getString(AttributeMapper.INITIALS),
				updatedcontact.getString(AttributeMapper.INITIALS));
	}

}
