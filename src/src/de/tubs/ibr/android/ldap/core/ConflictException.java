package de.tubs.ibr.android.ldap.core;

public class ConflictException extends Exception {

  /**
   * 
   */
  private static final long serialVersionUID = -1723161807941783775L;
  private final SyncConflict conflict;

  public ConflictException(SyncConflict conflict) {
    this.conflict = conflict;
  }

  public final SyncConflict getConflict() {
    return this.conflict;
  }
}
