package de.tubs.ibr.android.ldap.sync;

import java.io.UnsupportedEncodingException;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import android.app.Activity;
import android.os.Bundle;

public class ViewLDAPContacts extends Activity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    int ldapPort = LDAPConnection.DEFAULT_PORT;
    int searchScope = LDAPConnection.SCOPE_ONE;
    int ldapVersion = LDAPConnection.LDAP_V3;
    boolean attributeOnly = true;
    String attrs[] = { LDAPConnection.NO_ATTRS };
    String ldapHost = "soney";
    String loginDN = "";
    String password = "";
    String searchBase = "";
    String searchFilter = "(objectclass=*)";
    LDAPConnection lc = new LDAPConnection();
    try {
      // connect to the server
      lc.connect(ldapHost, ldapPort);
      // bind to the server
      lc.bind(ldapVersion, loginDN, password.getBytes("UTF8"));
      LDAPSearchResults searchResults = lc.search(searchBase, // container to
                                                              // search
          searchScope, // search scope
          searchFilter, // search filter
          attrs, // "1.1" returns entry name only
          attributeOnly); // no attributes are returned
      // print out all the objects
      while (searchResults.hasMore()) {
        LDAPEntry nextEntry = null;
        try {
          nextEntry = searchResults.next();
          System.out.println("\n" + nextEntry.getDN());
        } catch (LDAPException e) {
          e.printStackTrace();
          // Exception is thrown, go for next entry
          break;
        }

      }
      // disconnect with the server
      lc.disconnect();
    } catch (LDAPException e) {
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }
}