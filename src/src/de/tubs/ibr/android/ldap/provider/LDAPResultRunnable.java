package de.tubs.ibr.android.ldap.provider;

import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPResult;
import com.unboundid.ldap.sdk.SearchResult;

public abstract class LDAPResultRunnable implements Runnable {
  public SearchResult searchResult;
  public LDAPResult insertResult;
  public LDAPException exception;
  public LDAPResult deleteResult;
  public LDAPResult modificationResult;
  public String[] dirsResult;
}
