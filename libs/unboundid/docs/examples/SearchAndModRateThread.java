/*
 * Copyright 2010 UnboundID Corp.
 * All Rights Reserved.
 */
/*
 * Copyright (C) 2010 UnboundID Corp.
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



import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.unboundid.asn1.ASN1OctetString;
import com.unboundid.ldap.sdk.Filter;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.LDAPSearchException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.ModifyRequest;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.ldap.sdk.controls.ProxiedAuthorizationV2RequestControl;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.ResultCodeCounter;
import com.unboundid.util.ValuePattern;



/**
 * This class provides a thread that may be used to repeatedly perform search
 * and modify operations.
 */
final class SearchAndModRateThread
      extends Thread
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = -8238795420559281873L;



  // Indicates whether a request has been made to stop running.
  private final AtomicBoolean stopRequested;

  // The counter used to track the number of errors encountered while searching.
  private final AtomicLong errorCounter;

  // The counter used to track the number of modifications performed.
  private final AtomicLong modCounter;

  // The value that will be updated with total duration of the modifies.
  private final AtomicLong modDurations;

  // The counter used to track the number of searches performed.
  private final AtomicLong searchCounter;

  // The value that will be updated with total duration of the searches.
  private final AtomicLong searchDurations;

  // The thread that is actually performing the search and modify operations.
  private final AtomicReference<Thread> searchAndModThread;

  // The connection to use for the searches.
  private final LDAPConnection connection;

  // The result code for this thread.
  private final AtomicReference<ResultCode> resultCode;

  // The set of characters that may be included in modify values.
  private final byte[] charSet;

  // The barrier that will be used to coordinate starting among all the threads.
  private final CyclicBarrier startBarrier;

  // The length to use for modify values.
  private final int valueLength;

  // The random number generator to use for this thread.
  private final Random random;

  // The result code counter to use for failed operations.
  private final ResultCodeCounter rcCounter;

  // The search request to generate.
  private final SearchRequest searchRequest;

  // The scope to use for search requests.
  private final SearchScope scope;

  // The set of attributes to modify.
  private final String[] modAttributes;

  // The set of requested attributes for search requests.
  private final String[] returnAttributes;

  // The value pattern to use for proxied authorization.
  private final ValuePattern authzID;

  // The value pattern to use for the base DNs.
  private final ValuePattern baseDN;

  // The value pattern to use for the filters.
  private final ValuePattern filter;

  // The barrier to use for controlling the rate of searches.  null if no
  // rate-limiting should be used.
  private final FixedRateBarrier fixedRateBarrier;



  /**
   * Creates a new search rate thread with the provided information.
   *
   * @param  threadNumber      The thread number for this thread.
   * @param  connection        The connection to use for the searches.
   * @param  baseDN            The value pattern to use for the base DNs.
   * @param  scope             The scope to use for the searches.
   * @param  filter            The value pattern for the filters.
   * @param  returnAttributes  The set of attributes to return for searches.
   * @param  modAttributes     The set of attributes to modify.
   * @param  valueLength       The length to use for generated modify values.
   * @param  charSet           The set of characters that may be included in
   *                           modify values.
   * @param  authzID           The value pattern to use to generate
   *                           authorization identities for use with the proxied
   *                           authorization control.  It may be {@code null} if
   *                           proxied authorization should not be used.
   * @param  randomSeed        The seed to use for the random number generator.
   * @param  startBarrier      A barrier used to coordinate starting between all
   *                           of the threads.
   * @param  searchCounter     A value that will be used to keep track of the
   *                           total number of searches performed.
   * @param  modCounter        A value that will be used to keep track of the
   *                           total number of modifications performed.
   * @param  searchDurations   A value that will be used to keep track of the
   *                           total duration for all searches.
   * @param  modDurations      A value that will be used to keep track of the
   *                           total duration for all modifications.
   * @param  errorCounter      A value that will be used to keep track of the
   *                           number of errors encountered while searching.
   * @param  rcCounter         The result code counter to use for keeping track
   *                           of the result codes for failed operations.
   * @param  rateBarrier       The barrier to use for controlling the rate of
   *                           searches.  {@code null} if no rate-limiting
   *                           should be used.
   */
  SearchAndModRateThread(final int threadNumber,
       final LDAPConnection connection, final ValuePattern baseDN,
       final SearchScope scope, final ValuePattern filter,
       final String[] returnAttributes, final String[] modAttributes,
       final int valueLength, final byte[] charSet, final ValuePattern authzID,
       final long randomSeed, final CyclicBarrier startBarrier,
       final AtomicLong searchCounter, final AtomicLong modCounter,
       final AtomicLong searchDurations, final AtomicLong modDurations,
       final AtomicLong errorCounter, final ResultCodeCounter rcCounter,
       final FixedRateBarrier rateBarrier)
  {
    setName("SearchRate Thread " + threadNumber);
    setDaemon(true);

    this.connection       = connection;
    this.baseDN           = baseDN;
    this.scope            = scope;
    this.filter           = filter;
    this.returnAttributes = returnAttributes;
    this.modAttributes    = modAttributes;
    this.valueLength      = valueLength;
    this.charSet          = charSet;
    this.authzID          = authzID;
    this.searchCounter    = searchCounter;
    this.modCounter       = modCounter;
    this.searchDurations  = searchDurations;
    this.modDurations     = modDurations;
    this.errorCounter     = errorCounter;
    this.rcCounter        = rcCounter;
    this.startBarrier     = startBarrier;
    fixedRateBarrier      = rateBarrier;

    connection.setConnectionName("search-and-mod-" + threadNumber);

    random             = new Random(randomSeed);
    resultCode         = new AtomicReference<ResultCode>(null);
    searchAndModThread = new AtomicReference<Thread>(null);
    stopRequested      = new AtomicBoolean(false);
    searchRequest      = new SearchRequest("", scope,
         Filter.createPresenceFilter("objectClass"), returnAttributes);
  }



  /**
   * Performs all processing for this thread.
   */
  @Override()
  public void run()
  {
    searchAndModThread.set(currentThread());

    final Modification[] mods = new Modification[modAttributes.length];
    final byte[] valueBytes = new byte[valueLength];
    final ASN1OctetString[] values = new ASN1OctetString[1];
    final ModifyRequest modifyRequest = new ModifyRequest("", mods);

    try
    {
      startBarrier.await();
    } catch (Exception e) {}

    while (! stopRequested.get())
    {
      // If we're trying for a specific target rate, then we might need to
      // wait until issuing the next search.
      if (fixedRateBarrier != null)
      {
        fixedRateBarrier.await();
      }

      try
      {
        searchRequest.setBaseDN(baseDN.nextValue());
        searchRequest.setFilter(filter.nextValue());

        if (authzID != null)
        {
          searchRequest.setControls(new ProxiedAuthorizationV2RequestControl(
               authzID.nextValue()));
        }
      }
      catch (LDAPException le)
      {
        errorCounter.incrementAndGet();

        final ResultCode rc = le.getResultCode();
        rcCounter.increment(rc);
        resultCode.compareAndSet(null, rc);
        continue;
      }

      final long searchStartTime = System.nanoTime();

      final SearchResult r;
      try
      {
        r = connection.search(searchRequest);
      }
      catch (LDAPSearchException lse)
      {
        errorCounter.incrementAndGet();

        final ResultCode rc = lse.getResultCode();
        rcCounter.increment(rc);
        resultCode.compareAndSet(null, rc);

        if (! lse.getResultCode().isConnectionUsable())
        {
          try
          {
            connection.reconnect();
          } catch (final LDAPException le2) {}
        }

        continue;
      }
      finally
      {
        searchCounter.incrementAndGet();
        searchDurations.addAndGet(System.nanoTime() - searchStartTime);
      }

      for (int i=0; i < valueLength; i++)
      {
        valueBytes[i] = charSet[random.nextInt(charSet.length)];
      }

      values[0] = new ASN1OctetString(valueBytes);
      for (int i=0; i < modAttributes.length; i++)
      {
        mods[i] = new Modification(ModificationType.REPLACE, modAttributes[i],
                                   values);
      }
      modifyRequest.setModifications(mods);

      if (authzID != null)
      {
        modifyRequest.setControls(new ProxiedAuthorizationV2RequestControl(
             authzID.nextValue()));
      }

      for (final SearchResultEntry e : r.getSearchEntries())
      {
        if (fixedRateBarrier != null)
        {
          fixedRateBarrier.await();
        }

        modifyRequest.setDN(e.getDN());

        final long modStartTime = System.nanoTime();
        try
        {
          connection.modify(modifyRequest);
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
              connection.reconnect();
            } catch (final LDAPException le2) {}
          }
        }
        finally
        {
          modCounter.incrementAndGet();
          modDurations.addAndGet(System.nanoTime() - modStartTime);
        }
      }
    }

    connection.close();
    searchAndModThread.set(null);
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

    final Thread t = searchAndModThread.get();
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
