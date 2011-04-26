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



import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.unboundid.ldap.sdk.AsyncRequestID;
import com.unboundid.ldap.sdk.AsyncSearchResultListener;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchResultReference;
import com.unboundid.util.ResultCodeCounter;



/**
 * This class provides an asynchronous search result listener that will be used
 * for the {@link SearchRate} tool when operating in asynchronous mode.
 */
final class SearchRateAsyncListener
      implements AsyncSearchResultListener
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 4929527281011834420L;



  // The counter used to track the number of entries returned.
  private final AtomicLong entryCounter;

  // The counter used to track the number of errors encountered while searching.
  private final AtomicLong errorCounter;

  // The counter used to track the number of searches performed.
  private final AtomicLong searchCounter;

  // The value that will be updated with total duration of the searches.
  private final AtomicLong searchDurations;

  // The result code for the search.
  private final AtomicReference<ResultCode> resultCode;

  // The time that the search was invoked, in nanoseconds.
  private final long startTime;

  // The result code counter to use for failed operations.
  private final ResultCodeCounter rcCounter;

  // The semaphore used to limit total number of outstanding asynchronous
  // requests.
  private final Semaphore asyncSemaphore;



  /**
   * Creates a new instance of this listener with the provided information.
   *
   * @param  searchCounter    A value that will be used to keep track of the
   *                          total number of searches performed.
   * @param  entryCounter     A value that will be used to keep track of the
   *                          total number of entries returned.
   * @param  searchDurations  A value that will be used to keep track of the
   *                          total duration for all searches.
   * @param  errorCounter     A value that will be used to keep track of the
   *                          number of errors encountered while searching.
   * @param  rcCounter        The result code counter to use for keeping track
   *                          of the result codes for failed operations.
   * @param  asyncSemaphore   The semaphore used ot limit the total number of
   *                          outstanding asynchronous requests.
   * @param  resultCode       The result code for the search thread.
   */
  SearchRateAsyncListener(final AtomicLong searchCounter,
                          final AtomicLong entryCounter,
                          final AtomicLong searchDurations,
                          final AtomicLong errorCounter,
                          final ResultCodeCounter rcCounter,
                          final Semaphore asyncSemaphore,
                          final AtomicReference<ResultCode> resultCode)
  {
    this.searchCounter   = searchCounter;
    this.entryCounter    = entryCounter;
    this.searchDurations = searchDurations;
    this.errorCounter    = errorCounter;
    this.rcCounter       = rcCounter;
    this.asyncSemaphore  = asyncSemaphore;
    this.resultCode      = resultCode;

    startTime = System.nanoTime();
  }



  /**
   * {@inheritDoc}
   */
  public void searchEntryReturned(final SearchResultEntry searchEntry)
  {
    // No implementation required.
  }



  /**
   * {@inheritDoc}
   */
  public void searchReferenceReturned(
                   final SearchResultReference searchReference)
  {
    // No implementation required.
  }



  /**
   * {@inheritDoc}
   */
  public void searchResultReceived(final AsyncRequestID requestID,
                                   final SearchResult searchResult)
  {
    searchDurations.addAndGet(System.nanoTime() - startTime);

    if (asyncSemaphore != null)
    {
      asyncSemaphore.release();
    }

    searchCounter.incrementAndGet();
    entryCounter.addAndGet(searchResult.getEntryCount());

    final ResultCode rc = searchResult.getResultCode();
    if (rc != ResultCode.SUCCESS)
    {
      errorCounter.incrementAndGet();
      rcCounter.increment(rc);
      resultCode.compareAndSet(null, rc);
    }
  }
}
