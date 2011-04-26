package de.tubs.ibr.android.ldap.sync;

import de.tubs.ibr.android.ldap.core.ConflictException;
import de.tubs.ibr.android.ldap.core.Contact;
import de.tubs.ibr.android.ldap.core.SyncConflict;

/**
 * @author Till Lorentzen
 */
public class SyncManager {
  public static SyncConflict compareContacts(int id, String uid) {
    return null;
  }

  public static void importContact(Contact remote) {

  }

  public static void importContact(String remoteuid) {

  }

  public static void importAndMergeContact(Contact remote, Contact local)
      throws ConflictException {
  }

  public static void importAndMergeContact(String remoteuid, int localid)
      throws ConflictException {

  }
  
  
}
