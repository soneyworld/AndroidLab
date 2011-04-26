package de.tubs.ibr.android.ldap.core;

public class NoSyncedContactFoundException extends IllegalArgumentException {

  /**
   * 
   */
  private static final long serialVersionUID = -5315248287043689538L;
  private String msg = null;

  public NoSyncedContactFoundException() {
  }

  public NoSyncedContactFoundException(String ldapuuid) {
    this.msg = "Cannot find any synced state for remote Contact with UID: "
        + ldapuuid;
  }

  public NoSyncedContactFoundException(int localid) {
    this.msg = "Cannot find any synced state for local Contact with ID: "
        + localid;
  }

  @Override
  public String getMessage() {
    if (msg == null) {
      return super.getMessage();
    } else {
      return msg;
    }
  }
}
