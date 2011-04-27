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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.net.SocketFactory;
import android.content.Context;
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
   * The name of the file containing information about the defined instances.
   */
  private static final String INSTANCE_FILE_NAME = "INSTANCES";

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
   * Encodes the information in this instance to a string.
   * 
   * @return A string representation of this encoded instance.
   */
  private String encode() {
    final StringBuilder buffer = new StringBuilder();
    buffer.append(id);
    buffer.append('\t');
    buffer.append(host);
    buffer.append('\t');
    buffer.append(port);
    buffer.append('\t');
    buffer.append(useSSL);
    buffer.append('\t');
    buffer.append(useStartTLS);
    buffer.append('\t');

    if (bindDN != null) {
      buffer.append(bindDN);
    }
    buffer.append('\t');

    if (bindPW != null) {
      buffer.append(bindPW);
    }
    buffer.append('\t');

    if (baseDN != null) {
      buffer.append(baseDN);
    }

    return buffer.toString();
  }

  /**
   * Decodes the provided string to an instance.
   * 
   * @param s
   *          The string to be decoded.
   * @return The decoded instance.
   */
  private static ServerInstance decode(final String s) {
    int start = 0;
    final String[] tokens = new String[8];
    for (int i = 0; i < (tokens.length - 1); i++) {
      final int tabPos = s.indexOf('\t', start);
      tokens[i] = s.substring(start, tabPos);
      start = tabPos + 1;
    }

    tokens[tokens.length - 1] = s.substring(start);

    final String id = tokens[0];
    final String host = tokens[1];
    final int port = Integer.parseInt(tokens[2]);
    final boolean useSSL = Boolean.valueOf(tokens[3]);
    final boolean useStartTLS = Boolean.valueOf(tokens[4]);
    final String bindDN = tokens[5];
    final String bindPW = tokens[6];
    final String baseDN = tokens[7];

    return new ServerInstance(id, host, port, useSSL, useStartTLS, bindDN,
        bindPW, baseDN);
  }

  /**
   * Retrieves a map of all defined server instances, mapped from the instance
   * ID to the instance object.
   * 
   * @param context
   *          The application context. It must not be {@code null}.
   * @return A map of all defined server instances.
   * @throws IOException
   *           If a problem occurs while reading information about the defined
   *           instances.
   */
  public static Map<String, ServerInstance> getInstances(final Context context)
      throws IOException {
    final LinkedHashMap<String, ServerInstance> instances = new LinkedHashMap<String, ServerInstance>(
        1);

    BufferedReader reader = null;
    try {
      try {
        reader = new BufferedReader(new InputStreamReader(
            context.openFileInput(INSTANCE_FILE_NAME)), 1024);
      } catch (final FileNotFoundException fnfe) {

        // This is fine -- no instances have been defined yet.
        return Collections.unmodifiableMap(instances);
      }

      while (true) {
        final String line = reader.readLine();
        if (line == null) {
          break;
        }
        if (line.length() == 0) {
          continue;
        }
        final ServerInstance i = decode(line);
        instances.put(i.getID(), i);
      }

      return Collections.unmodifiableMap(instances);
    } finally {
      if (reader != null) {
        reader.close();
      }
    }
  }

  /**
   * Saves the provided list of instances, overwriting any existing instance
   * information.
   * 
   * @param context
   *          The application context. It must not be {@code null}.
   * @param instances
   *          The set of instances to be saved. If it is {@code null} or empty,
   *          then any existing instance information will be removed.
   * @throws IOException
   *           If a problem occurs while writing the information about the
   *           defined instances.
   */
  public static void saveInstances(final Context context,
      final Map<String, ServerInstance> instances) throws IOException {
    if (instances == null) {
      saveInstances(context, (List<ServerInstance>) null);
      return;
    }

    final LinkedList<ServerInstance> instanceList = new LinkedList<ServerInstance>(
        instances.values());
    saveInstances(context, instanceList);
  }

  /**
   * Saves the provided list of instances, overwriting any existing instance
   * information.
   * 
   * @param context
   *          The application context. It must not be {@code null}.
   * @param instances
   *          The list of instances to be saved. If it is {@code null} or empty,
   *          then any existing instance information will be removed.
   * @throws IOException
   *           If a problem occurs while writing the information about the
   *           defined instances.
   */
  public static void saveInstances(final Context context,
      final List<ServerInstance> instances) throws IOException {
    final File instanceFile = context.getFileStreamPath(INSTANCE_FILE_NAME);
    final File tmpFile = new File(instanceFile.getAbsolutePath() + ".tmp");
    if ((instances == null) || instances.isEmpty()) {
      return;
    }
    final BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile),
        1024);
    try {
      for (final ServerInstance i : instances) {
        writer.write(i.encode());
        writer.newLine();
      }
    } finally {
      writer.close();
    }
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
}
