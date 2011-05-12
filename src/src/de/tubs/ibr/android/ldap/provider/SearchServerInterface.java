package de.tubs.ibr.android.ldap.provider;

import com.unboundid.ldap.sdk.SearchResult;

public interface SearchServerInterface {

  /**
   * Takes any appropriate action after a button has been clicked.
   *
   * @param  view  The view for the button that was clicked.
   */
  public abstract void searchDone(final SearchResult result);

}