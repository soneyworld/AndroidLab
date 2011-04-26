package de.tubs.ibr.android.ldap.core;

/**
 * @author Till Lorentzen
 */
public class SyncConflict {
  private final Contact local;
  private final Contact remote;
  private final boolean history;

  /**
   * @param local
   * @param remote
   */
  public SyncConflict(Contact local, Contact remote) {
    this.local = local;
    this.remote = remote;
    if (!this.local.getSyncedContact().equals(this.remote.getSyncedContact())) {
      this.history = false;
    } else {
      this.history = true;
    }
  }

  /**
   * @return
   */
  public final Contact getLocal() {
    return local;
  }

  /**
   * @return
   */
  public final Contact getRemote() {
    return remote;
  }

  /**
   * @return
   */
  public boolean hasHistory() {
    return history;
  }
}
