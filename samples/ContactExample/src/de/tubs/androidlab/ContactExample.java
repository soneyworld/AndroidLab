package de.tubs.androidlab;

import android.app.Activity;
import android.os.Bundle;

import android.provider.ContactsContract;
import android.provider.ContactsContract.CommonDataKinds.Phone;
import android.provider.ContactsContract.CommonDataKinds.StructuredName;
import android.provider.ContactsContract.Contacts;
import android.provider.ContactsContract.Data;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.database.Cursor;
public class ContactExample extends Activity {
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        String[] projection = new String[] {Contacts._ID,Contacts.HAS_PHONE_NUMBER};
        Cursor cr = managedQuery(ContactsContract.Contacts.CONTENT_URI, projection, null, null, null);
        if (cr.moveToFirst()) {
        	String id;
        	String givenname = null;
        	String familyname = null;
        	String number = null;
        	do {
        		id = cr.getString(cr.getColumnIndex(Contacts._ID));
        		projection = new String[] {
        				StructuredName.FAMILY_NAME,
        				StructuredName.GIVEN_NAME
        		};
        		Cursor name = managedQuery(ContactsContract.Data.CONTENT_URI, projection, 
        				Data.MIMETYPE+"='"+StructuredName.CONTENT_ITEM_TYPE+"' AND "+StructuredName.CONTACT_ID+"= ?", 
        				new String[]{id},null);
        				
        		if (name.moveToFirst()) {
        			givenname = name.getString(name.getColumnIndex(StructuredName.GIVEN_NAME));
        			familyname = name.getString(name.getColumnIndex(StructuredName.FAMILY_NAME));
        		}
        		if (cr.getString(cr.getColumnIndex(Contacts.HAS_PHONE_NUMBER)).equalsIgnoreCase("1")) {
        			projection = new String[] {
        					Phone.NUMBER
        			};
        			
        			Cursor phone = managedQuery(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, projection, Phone.CONTACT_ID+"= ?",new String[]{
            				id}
            		, null);
        			phone.moveToFirst();
        			number = phone.getString(phone.getColumnIndex(Phone.NUMBER));
        		}
        		
        		TableRow row = new TableRow(getBaseContext());
        		
        		TextView text = null;
        		
        		text = new TextView(getBaseContext());
        		text.setText(familyname);
        		row.addView(text);
        		
        		text = new TextView(getBaseContext());
        		text.setText(givenname);
        		row.addView(text);
        		
        		text = new TextView(getBaseContext());
        		text.setText(number);
        		row.addView(text);
        		
        		TableLayout t = (TableLayout) findViewById(R.id.tableLayout);
        		t.addView(row);
        	} while (cr.moveToNext());
        }
    }
}