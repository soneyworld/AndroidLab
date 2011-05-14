package de.tubs.ibr.android.ldap.provider;

import com.unboundid.ldap.sdk.SearchResult;

public abstract class SearchResultRunnable implements Runnable {
  public SearchResult result;
}
