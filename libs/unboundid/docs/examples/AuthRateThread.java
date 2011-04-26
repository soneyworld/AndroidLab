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
package com.unboundid.ldap.sdk.examples;



import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.unboundid.ldap.sdk.BindRequest;
import com.unboundid.ldap.sdk.CRAMMD5BindRequest;
import com.unboundid.ldap.sdk.DIGESTMD5BindRequest;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.PLAINBindRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.SimpleBindRequest;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ResultCodeCounter;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a thread that may be used to repeatedly perform
 * authentication processing.
 */
final class AuthRateThread
      extends Thread
{
  /**
   * The authentication type value that will be used to indicate that simple
   * authentication should be performed.
   */
  private static final int AUTH_TYPE_SIMPLE = 0;



  /**
   * The authentication type value that will be used to indicate that CRAM-MD5
   * authentication should be performed.
   */
  private static final int AUTH_TYPE_CRAM_MD5 = 1;



  /**
   * The authentication type value that will be used to indicate that DIGEST-MD5
   * authentication should be performed.
   */
  private static final int AUTH_TYPE_DIGEST_MD5 = 2;



  /**
   * The authentication type value that will be used to indicate that PLAIN
   * authentication should be performed.
   */
  private static final int AUTH_TYPE_PLAIN = 3;



  // Indicates whether a request has been made to stop running.
  private final AtomicBoolean stopRequested;

  // The counter used to track the number of searches performed.
  private final AtomicLong authCounter;

  // The value that will be updated with total duration of the searches.
  private final AtomicLong authDurations;

  // The counter used to track the number of errors encountered while searching.
  private final AtomicLong errorCounter;

  // The result code for this thread.
  private final AtomicReference<ResultCode> resultCode;

  // The thread that is actually performing the searches.
  private final AtomicReference<Thread> authThread;

  // The barrier that will be used to coordinate starting among all the threads.
  private final CyclicBarrier startBarrier;

  // The type of authentication to perform.
  private final int authType;

  // The connection to use for the binds.
  private final LDAPConnection bindConnection;

  // The connection to use for the searches.
  private final LDAPConnection searchConnection;

  // The result code counter to use for failed operations.
  private final ResultCodeCounter rcCounter;

  // The search request to generate.
  private final SearchRequest searchRequest;

  // The password to use to authenticate.
  private final String userPassword;

  // The value pattern to use for the base DNs.
  private final ValuePattern baseDN;

  //The value pattern to use for the filters.
  private final ValuePattern filter;

  // The barrier to use for controlling the rate of auths.  null if no
  // rate-limiting should be used.
  private final FixedRateBarrier fixedRateBarrier;



  /**
   * Creates a new auth rate thread with the provided information.
   *
   * @param  threadNumber      The thread number for this thread.
   * @param  searchConnection  The connection to use for the searches.
   * @param  bindConnection    The connection to use for the  binds.
   * @param  baseDN            The value pattern to use for the base DNs.
   * @param  scope             The scope to use for the searches.
   * @param  filter            The value pattern for the filters.
   * @param  attributes        The set of attributes to return.
   * @param  userPassword      The password to use for the bind operations.
   * @param  authType          The type of authentication to perform.
   * @param  startBarrier      A barrier used to coordinate starting between all
   *                           of the threads.
   * @param  authCounter       A value that will be used to keep track of the
   *                           total number of authentications performed.
   * @param  authDurations     A value that will be used to keep track of the
   *                           total duration for all authentications.
   * @param  errorCounter      A value that will be used to keep track of the
   *                           number of errors encountered while searching.
   * @param  rcCounter         The result code counter to use for keeping track
   *                           of the result codes for failed operations.
   * @param  rateBarrier       The barrier to use for controlling the rate of
   *                           authorizations.  {@code null} if no rate-limiting
   *                           should be used.
   */
  AuthRateThread(final int threadNumber, final LDAPConnection searchConnection,
                 final LDAPConnection bindConnection, final ValuePattern baseDN,
                 final SearchScope scope, final ValuePattern filter,
                 final String[] attributes, final String userPassword,
                 final String authType, final CyclicBarrier startBarrier,
                 final AtomicLong authCounter, final AtomicLong authDurations,
                 final AtomicLong errorCounter,
                 final ResultCodeCounter rcCounter,
                 final FixedRateBarrier rateBarrier)
  {
    setName("AuthRate Thread " + threadNumber);
    setDaemon(true);

    this.searchConnection = searchConnection;
    this.bindConnection   = bindConnection;
    this.baseDN           = baseDN;
    this.filter           = filter;
    this.userPassword     = userPassword;
    this.authCounter      = authCounter;
    this.authDurations    = authDurations;
    this.errorCounter     = errorCounter;
    this.rcCounter        = rcCounter;
    this.startBarrier     = startBarrier;
    fixedRateBarrier      = rateBarrier;

    searchConnection.setConnectionName("search-" + threadNumber);
    bindConnection.setConnectionName("bind-" + threadNumber);

    if (authType.equalsIgnoreCase("cram-md5"))
    {
      this.authType = AUTH_TYPE_CRAM_MD5;
    }
    else if (authType.equalsIgnoreCase("digest-md5"))
    {
      this.authType = AUTH_TYPE_DIGEST_MD5;
    }
    else if (authType.equalsIgnoreCase("plain"))
    {
      this.authType = AUTH_TYPE_PLAIN;
    }
    else
    {
      this.authType = AUTH_TYPE_SIMPLE;
    }

    resultCode    = new AtomicReference<ResultCode>(null);
    authThread    = new AtomicReference<Thread>(null);
    stopRequested = new AtomicBoolean(false);
    searchRequest = new SearchRequest("", scope,
         Filter.createPresenceFilter("objectClass"), attributes);
  }



  /**
   * Performs all search processing for this thread.
   */
  @Override()
  public void run()
  {
    authThread.set(currentThread());

    try
    {
      startBarrier.await();
    } catch (Exception e) {}

    while (! stopRequested.get())
    {
      try
      {
        searchRequest.setBaseDN(baseDN.nextValue());
        searchRequest.setFilter(filter.nextValue());
      }
      catch (LDAPException le)
      {
        errorCounter.incrementAndGet();

        final ResultCode rc = le.getResultCode();
        rcCounter.increment(rc);
        resultCode.compareAndSet(null, rc);
        continue;
      }

      // If we're trying for a specific target rate, then we might need to
      // wait until starting the next authorization.
      if (fixedRateBarrier != null)
      {
        fixedRateBarrier.await();
      }

      final long startTime = System.nanoTime();

      try
      {
        final SearchResult r = searchConnection.search(searchRequest);
        switch (r.getEntryCount())
        {
          case 0:
            errorCounter.incrementAndGet();
            rcCounter.increment(ResultCode.NO_RESULTS_RETURNED);
            resultCode.compareAndSet(null, ResultCode.NO_RESULTS_RETURNED);
            continue;

          case 1:
            // This is acceptable, and we can continue processing.
            break;

          default:
            errorCounter.incrementAndGet();
            rcCounter.increment(ResultCode.MORE_RESULTS_TO_RETURN);
            resultCode.compareAndSet(null, ResultCode.MORE_RESULTS_TO_RETURN);
            continue;
        }

        BindRequest bindRequest = null;
        final String dn = r.getSearchEntries().get(0).getDN();
        switch (authType)
        {
          case AUTH_TYPE_SIMPLE:
            bindRequest = new SimpleBindRequest(dn, userPassword);
            break;

          case AUTH_TYPE_CRAM_MD5:
            bindRequest = new CRAMMD5BindRequest("dn:" + dn, userPassword);
            break;

          case AUTH_TYPE_DIGEST_MD5:
            bindRequest = new DIGESTMD5BindRequest("dn:" + dn, userPassword);
            break;

          case AUTH_TYPE_PLAIN:
            bindRequest = new PLAINBindRequest("dn:" + dn, userPassword);
            break;
        }

        bindConnection.bind(bindRequest);
      }
      catch (LDAPException le)
      {
        errorCounter.incrementAndGet();

        final ResultCode rc = le.getResultCode();
        rcCounter.increment(rc);
        resultCode.compareAndSet(null, rc);

        if (! le.getResultCode().isConnectionUsable())
        {
          try
          {
            searchConnection.reconnect();
            bindConnection.reconnect();
          } catch (final LDAPException le2) {}
        }
      }
      finally
      {
        authCounter.incrementAndGet();
        authDurations.addAndGet(System.nanoTime() - startTime);
      }
    }

    searchConnection.close();
    bindConnection.close();
    authThread.set(null);
  }



  /**
   * Indicates that this thread should stop running.
   *
   * @return  A result code that provides information about whether any errors
   *          were encountered during processing.
   */
  public ResultCode stopRunning()
  {
    stopRequested.set(true);

    if (fixedRateBarrier != null)
    {
      fixedRateBarrier.shutdownRequested();
    }

    final Thread t = authThread.get();
    if (t != null)
    {
      try
      {
        t.join();
      } catch (Exception e) {}
    }

    resultCode.compareAndSet(null, ResultCode.SUCCESS);
    return resultCode.get();
  }
}
