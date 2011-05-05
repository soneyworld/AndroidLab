/*
 * Copyright 2009-2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2009-2010 UnboundID Corp.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPLv2 only)
 * or the terms of the GNU Lesser General Public License (LGPLv2.1 only)
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, see <http://www.gnu.org/licenses>.
 */
package de.tubs.ibr.android.ldap.auth;

import java.io.Serializable;
import java.util.List;
import javax.net.SocketFactory;
import android.os.Bundle;
import com.unboundid.ldap.sdk.DN;
import com.unboundid.ldap.sdk.ExtendedResult;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.extensions.StartTLSExtendedRequest;
import com.unboundid.util.ssl.SSLUtil;
import com.unboundid.util.ssl.TrustAllTrustManager;
import static com.unboundid.util.StaticUtils.*;
import de.tubs.ibr.android.ldap.provider.*;
import de.tubs.ibr.android.ldap.R;

/**
 * This class provides a data structure that holds information about a single
 * directory server instance and provides methods for interacting with it. It
 * also provides the logic for loading and saving server information to
 * persistent storage.
 */
public final class ServerInstance implements Serializable {
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 4155827014458673905L;

  // Indicates whether to use SSL when communicating with the server.
  private final boolean useSSL;

  // Indicates whether to use StartTLS when communicating with the server.
  private final boolean useStartTLS;

  // The port number of the directory server.
  private final int port;

  // The base DN to use when searching the server.
  private final String baseDN;

  // The DN to use to bind to the server.
  private final String bindDN;

  // The password to use to bind to the server.
  private final String bindPW;

  // The address of the directory server.
  private final String host;

  // The unique identifier assigned to this server.
  private final String id;

  /**
   * Creates a new server instance with the provided information in the bundle.
   * 
   * @param b
   *          The Bundle with all necessary informations for the server instance
   */
  public ServerInstance(final Bundle b) {
    this.id = b.getString("id");
    this.host = b.getString("host");
    this.port = b.getInt("port");
    this.useSSL = b.getBoolean("useSSL");
    this.useStartTLS = b.getBoolean("useStartTLS");

    this.bindDN = (b.getString("bindDN") == null)
        || (b.getString("bindDN").length() == 0) ? null : b.getString("bindDN");
    this.bindPW = (b.getString("bindPW") == null)
        || (b.getString("bindPW").length() == 0) ? null : b.getString("bindPW");
    this.baseDN = b.getString("baseDN") == null ? "" : b.getString("baseDN");
  }

  /**
   * Creates a new server instance with the provided information.
   * 
   * @param id
   *          The unique identifier assigned to the server, which will
   *          distinguish it from other servers that have been defined. It must
   *          not be {@code null}.
   * @param host
   *          The address of the server. It must not be {@code null}.
   * @param port
   *          The port number for the server. It must be between 1 and 65535.
   * @param useSSL
   *          Indicates whether to use SSL when communicating with the server.
   *          It must not be {@code true} if {@code useStartTLS} is also
   *          {@code true}.
   * @param useStartTLS
   *          Indicates whether to use StartTLS when communicating with the
   *          server. It must not be {@code true} if {@code useSSL} is also
   *          {@code true}.
   * @param bindDN
   *          The DN to use to bind to the server. It may be {@code null} or
   *          empty if no authentication should be performed.
   * @param bindPW
   *          The password to use to bind to the server. It may be {@code null}
   *          or empty if no authentication should be performed.
   * @param baseDN
   *          The base DN to use when searching the server. It may be
   *          {@code null} or empty if all searches should be based at the root
   *          DSE.
   */
  public ServerInstance(final String id, final String host, final int port,
      final boolean useSSL, final boolean useStartTLS, final String bindDN,
      final String bindPW, final String baseDN) {

    this.id = id;
    this.host = host;
    this.port = port;
    this.useSSL = useSSL;
    this.useStartTLS = useStartTLS;

    this.bindDN = (bindDN == null) || (bindDN.length() == 0) ? null : bindDN;
    this.bindPW = (bindPW == null) || (bindPW.length() == 0) ? null : bindPW;
    this.baseDN = baseDN == null ? "" : baseDN;
  }

  /**
   * Retrieves the unique identifier for this server instance.
   * 
   * @return The unique identifier for this server instance.
   */
  public String getID() {
    return id;
  }

  /**
   * Retrieves the address of the directory server.
   * 
   * @return The address of the directory server.
   */
  public String getHost() {
    return host;
  }

  /**
   * Retrieves the port number for the directory server.
   * 
   * @return The port number for the directory server.
   */
  public int getPort() {
    return port;
  }

  /**
   * Indicates whether to use SSL when communicating with the server.
   * 
   * @return {@code true} if SSL should be used when communicating with the
   *         server, or {@code false} if not.
   */
  public boolean useSSL() {
    return useSSL;
  }

  /**
   * Indicates whether to use StartTLS when communicating with the server.
   * 
   * @return {@code true} if StartTLS should be used when communicating with the
   *         server, or {@code false} if not.
   */
  public boolean useStartTLS() {
    return useStartTLS;
  }

  /**
   * Retrieves the DN to use to bind to the server, if any.
   * 
   * @return The DN to use to bind to the server, or {@code null} if no
   *         authentication should be performed.
   */
  public String getBindDN() {
    return bindDN;
  }

  /**
   * Retrieves the password to use to bind to the server, if any.
   * 
   * @return The password to use to bind to the server, or {@code null} if no
   *         authentication should be performed.
   */
  public String getBindPassword() {
    return bindPW;
  }

  /**
   * Retrieves the base DN to use when searching the server. It may be the empty
   * string if searches should be based at the root DSE.
   * 
   * @return The base DN to use when searching the server.
   */
  public String getBaseDN() {
    return baseDN;
  }

  /**
   * Determine whether the fields defined in this server instance appear to be
   * acceptable. This will not attempt to communicate with the directory server,
   * but will ensure that the values provided meet a number of constraints,
   * including:
   * <UL>
   * <LI>The instance ID must not be {@code null} or empty.</LI>
   * <LI>The host must not be {@code null} or empty.</LI>
   * <LI>The port number must be between 1 and 65535.</LI>
   * <LI>It cannot be configured to use both SSL and StartTLS.</LI>
   * <LI>The bind DN and password must be either both {@code null} or both non-
   * {@code null}.</LI>
   * <LI>If the bind DN is non-{@code null}, it must appear to be a valid DN.</LI>
   * <LI>The base DN must appear to be a valid DN (although it may be an empty
   * string).</LI>
   * </UL>
   * 
   * @param invoker
   *          The activity invoking this method.
   * @param unacceptableReasons
   *          A list into which information about any problems encountered will
   *          be added.
   * @return {@code true} if the fields of this server instance appear to be
   *         acceptable, or {@code false} if not.
   */
  public boolean isDefinitionValid(final StringProvider invoker,
      final List<String> unacceptableReasons) {
    boolean valid = true;
    if ((id == null) || (id.length() == 0)) {
      valid = false;
      unacceptableReasons.add(invoker
          .getString(R.string.server_instance_err_empty_id));
    }

    if ((host == null) || (host.length() == 0)) {
      valid = false;
      unacceptableReasons.add(invoker
          .getString(R.string.server_instance_err_empty_address));
    }

    if ((port < 1) || (port > 65535)) {
      valid = false;
      unacceptableReasons.add(invoker
          .getString(R.string.server_instance_err_invalid_port));
    }

    if (useSSL && useStartTLS) {
      valid = false;
      unacceptableReasons
          .add(invoker
              .getString(R.string.server_instance_err_cannot_combine_ssl_and_start_tls));
    }

    if (bindDN == null) {
      if (bindPW != null) {
        valid = false;
        unacceptableReasons.add(invoker
            .getString(R.string.server_instance_err_mismatched_bind_dn_and_pw));
      }
    } else {
      if (bindPW == null) {
        valid = false;
        unacceptableReasons.add(invoker
            .getString(R.string.server_instance_err_mismatched_bind_dn_and_pw));
      }

      try {
        new DN(bindDN);
      } catch (LDAPException le) {
        valid = false;
        unacceptableReasons.add(invoker.getString(
            R.string.server_instance_err_invalid_bind_dn, le.getMessage()));
      }
    }

    try {
      new DN(baseDN);
    } catch (LDAPException le) {
      valid = false;
      unacceptableReasons.add(invoker.getString(
          R.string.server_instance_err_invalid_base_dn, le.getMessage()));
    }

    return valid;
  }

  /**
   * Retrieves an LDAP connection that may be used to communicate with the
   * directory server.
   * 
   * @param invoker
   *          The activity invoking this method.
   * @return An LDAP connection that may be used to communicate with the
   *         directory server.
   * @throws LDAPException
   *           If a problem occurs while attempting to establish the connection.
   */
  public LDAPConnection getConnection(final StringProvider invoker)
      throws LDAPException {
    SocketFactory socketFactory = null;
    if (useSSL) {
      // FIXME -- We should use something more secure than blindly trusting any
      // certificate.
      final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
      try {
        socketFactory = sslUtil.createSSLSocketFactory();
      } catch (Exception e) {
        throw new LDAPException(ResultCode.LOCAL_ERROR, invoker.getString(
            R.string.server_instance_err_cannot_initialize_ssl,
            getExceptionMessage(e)), e);
      }
    }

    final LDAPConnectionOptions options = new LDAPConnectionOptions();
    options.setAutoReconnect(true);
    options.setConnectTimeoutMillis(30000);
    options.setFollowReferrals(false);
    options.setMaxMessageSize(1024 * 1024);

    final LDAPConnection conn = new LDAPConnection(socketFactory, options,
        host, port);

    if (useStartTLS) {
      // FIXME -- We should use something more secure than blindly trusting any
      // certificate.
      final SSLUtil sslUtil = new SSLUtil(new TrustAllTrustManager());
      try {
        final ExtendedResult r = conn
            .processExtendedOperation(new StartTLSExtendedRequest(sslUtil
                .createSSLContext()));
        if (r.getResultCode() != ResultCode.SUCCESS) {
          throw new LDAPException(r);
        }
      } catch (LDAPException le) {
        conn.close();
        throw le;
      } catch (Exception e) {
        conn.close();
        throw new LDAPException(ResultCode.CONNECT_ERROR, invoker.getString(
            R.string.server_instance_err_start_tls_failed,
            getExceptionMessage(e)), e);
      }
    }

    if ((bindDN != null) && (bindPW != null)) {
      try {
        conn.bind(bindDN, bindPW);
      } catch (LDAPException le) {
        conn.close();
        throw le;
      }
    }

    return conn;
  }

  /**
   * Retrieves a string representation of this server instance.
   * 
   * @return A string representation of this server instance.
   */
  @Override()
  public String toString() {

    final StringBuilder buffer = new StringBuilder();
    toString(buffer);
    return buffer.toString();
  }

  /**
   * Appends a string representation of this server instance to the provided
   * buffer.
   * 
   * @param buffer
   *          The buffer to which the information should be appended.
   */
  public void toString(final StringBuilder buffer) {
    buffer.append("ServerInstance(id=\"");
    buffer.append(id);
    buffer.append("\", host=\"");
    buffer.append(host);
    buffer.append("\", port=");
    buffer.append(port);
    buffer.append(", bindDN=\"");

    if (bindDN != null) {
      buffer.append(bindDN);
    }

    buffer.append("\", baseDN=\"");
    buffer.append(baseDN);
    buffer.append("\")");
  }
  
  public Bundle createBundle(){
    Bundle b = new Bundle();
    b.putCharSequence("id", this.id);
    b.putCharSequence("host", this.host);
    b.putInt("port", this.port);
    b.putBoolean("useSSL", this.useSSL);
    b.putBoolean("useStartTLS", this.useStartTLS);
    b.putCharSequence("bindDN", this.bindDN);
    b.putCharSequence("bindPW", this.bindPW);
    b.putCharSequence("baseDN", this.baseDN);
    return b;
  }
}
