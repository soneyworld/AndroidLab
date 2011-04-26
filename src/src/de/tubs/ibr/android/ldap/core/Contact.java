package de.tubs.ibr.android.ldap.core;

/**
 * @author Till Lorentzen
 */
public class Contact{

  private ContactState state;
  private final Contact syncedContact;

  /**
   * Creates a Contact from a given UID of a remote LDAP
   * 
   * @param ldapuid
   */
  public Contact(String ldapuid) {
    // TODO in DB schauen, ob es einen synchronisierten Stand gibt und diesen
    // einfügen, ansonsten null zuweisen
    this.syncedContact = null;
    if (this.equals(this.syncedContact)) {
      this.state = ContactState.synced;
    }else{
      this.state = ContactState.remotechanged;
    }
  }

  /**
   * Creates the last synchronized state of a remote LDAPContact with a given UID
   * 
   * @param ldapuid
   */
  private Contact(String ldapuid, boolean synced) {
    this.state = ContactState.synced;
    // TODO Contact aus der DB laden
    this.syncedContact = this;
  }

  /**
   * Creates a Contact from a given ID of a local Contact
   * 
   * @param id
   */
  public Contact(int id) {
    // TODO in DB schauen, ob es einen synchronisierten Stand gibt und diesen
    // einfügen, ansonsten null zuweisen
    this.syncedContact = null;
    if(this.equals(this.syncedContact)){
      this.state = ContactState.synced;
    }else{
      this.state = ContactState.localchanged;
    }
  }

  /**
   * Creates the last synchronized state of a local Contact with the given ID
   * 
   * @param id
   */
  private Contact(int id, boolean synced) {
    this.state = ContactState.localchanged;
    this.syncedContact = this;
  }

  public void mergeWith(Contact another) throws ConflictException {
    // TODO
  }

  public boolean equals(Contact c) {
    if(c==null){
      return false;
    }
    //TODO Inhalte vergleichen und state enumeration auslassen
    return super.equals(c);
  }

  public final Contact getSyncedContact() {
    return syncedContact;
  }
  
}
