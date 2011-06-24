package de.tubs.ibr.android.ldap.core.activities;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import android.net.Uri;
//import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.Entry;
import com.unboundid.ldif.LDIFException;
import com.unboundid.ldif.LDIFReader;

public class LDAPContactMapper {
  private LDAPContactMapping mapping;
  public LDAPContactMapper() {
    this.mapping = LDAPContactMapping.createInetOrgPersonMapping();
  }
  public LDAPContactMapper(LDAPContactMapping custommapping){
    this.mapping = custommapping;
  }
  
  public Entry entryFromLocalContact(Uri rawcontacturi){
    StringReader sr = new StringReader("");
    BufferedReader br = new BufferedReader(sr);
    LDIFReader ldifreader = new LDIFReader(br);
    try {
      Entry contactentry = ldifreader.readEntry();
      return contactentry;
    } catch (LDIFException e) {
      return null;
    } catch (IOException e) {
      return null;
    }
//    return null;
  }
}
