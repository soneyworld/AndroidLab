package de.tubs.ibr.android.ldap.sync;

import java.io.UnsupportedEncodingException;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPSearchResults;
import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class ViewLDAPContacts extends Activity {
  /** Called when the activity is first created. */
  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.main);
    int ldapPort = LDAPConnection.DEFAULT_PORT;
    int searchScope = LDAPConnection.SCOPE_ONE;
    int ldapVersion = LDAPConnection.LDAP_V3;
    String ldapHost = "ldap.tu-bs.de";
    String loginDN = "o=TU Braunschweig, c=DE";
    String password = "";
    String searchBase = "";
    String searchFilter = "(sn=Lorentzen)";
    LDAPConnection lc = new LDAPConnection();
    TextView loggingTextView = (TextView) findViewById(R.id.editText1);
    try {
      // connect to the server
      lc.connect(ldapHost, ldapPort);
      loggingTextView.append("\nConnected to LDAP Server");
      // bind to the server
      lc.bind(ldapVersion, loginDN, password.getBytes("UTF8"));
      loggingTextView.append("\nBind to LDAP Server");
      LDAPSearchResults searchResults = lc.search(searchBase, // container to
                                                              // search
          searchScope, // search scope
          searchFilter, // search filter
          null, // "1.1" returns entry name only
          false); // no attributes are returned
      // print out all the objects
//      while (searchResults.hasMore()) {
//        LDAPEntry nextEntry = null;
//        try {
//          nextEntry = searchResults.next();
//          System.out.println("\n" + nextEntry.getDN());
//        } catch (LDAPException e) {
//          e.printStackTrace();
//          if (e.getResultCode() == LDAPException.LDAP_TIMEOUT
//              || e.getResultCode() == LDAPException.CONNECT_ERROR)
//            break;
//          else
//            continue;
//        }
//
//      }
      // disconnect with the server
      lc.disconnect();
      loggingTextView.append("\nDisonnected from LDAP Server");
    } catch (LDAPException e) {
      loggingTextView.setText("ERROR: "+e.getLocalizedMessage());
      e.printStackTrace();
    } catch (UnsupportedEncodingException e) {
      e.printStackTrace();
    }
  }
}