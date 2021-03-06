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



import java.io.OutputStream;
import java.io.Serializable;
import java.text.ParseException;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicLong;

import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPConnectionOptions;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.ResultCode;
import com.unboundid.ldap.sdk.SearchScope;
import com.unboundid.util.ColumnFormatter;
import com.unboundid.util.FixedRateBarrier;
import com.unboundid.util.FormattableColumn;
import com.unboundid.util.HorizontalAlignment;
import com.unboundid.util.LDAPCommandLineTool;
import com.unboundid.util.ObjectPair;
import com.unboundid.util.OutputFormat;
import com.unboundid.util.ResultCodeCounter;
import com.unboundid.util.ThreadSafety;
import com.unboundid.util.ThreadSafetyLevel;
import com.unboundid.util.ValuePattern;
import com.unboundid.util.args.ArgumentException;
import com.unboundid.util.args.ArgumentParser;
import com.unboundid.util.args.BooleanArgument;
import com.unboundid.util.args.IntegerArgument;
import com.unboundid.util.args.StringArgument;

import static com.unboundid.util.StaticUtils.*;



/**
 * This class provides a tool that can be used to search an LDAP directory
 * server repeatedly using multiple threads, and then modify each entry
 * returned by that server.  It can help provide an estimate of the combined
 * search and modify performance that a directory server is able to achieve.
 * Either or both of the base DN and the search filter may be a value pattern as
 * described in the {@link ValuePattern} class.  This makes it possible to
 * search over a range of entries rather than repeatedly performing searches
 * with the same base DN and filter.
 * <BR><BR>
 * Some of the APIs demonstrated by this example include:
 * <UL>
 *   <LI>Argument Parsing (from the {@code com.unboundid.util.args}
 *       package)</LI>
 *   <LI>LDAP Command-Line Tool (from the {@code com.unboundid.util}
 *       package)</LI>
 *   <LI>LDAP Communication (from the {@code com.unboundid.ldap.sdk}
 *       package)</LI>
 *   <LI>Value Patterns (from the {@code com.unboundid.util} package)</LI>
 * </UL>
 * <BR><BR>
 * All of the necessary information is provided using command line arguments.
 * Supported arguments include those allowed by the {@link LDAPCommandLineTool}
 * class, as well as the following additional arguments:
 * <UL>
 *   <LI>"-b {baseDN}" or "--baseDN {baseDN}" -- specifies the base DN to use
 *       for the searches.  This must be provided.  It may be a simple DN, or it
 *       may be a value pattern to express a range of base DNs.</LI>
 *   <LI>"-s {scope}" or "--scope {scope}" -- specifies the scope to use for the
 *       search.  The scope value should be one of "base", "one", "sub", or
 *       "subord".  If this isn't specified, then a scope of "sub" will be
 *       used.</LI>
 *   <LI>"-f {filter}" or "--filter {filter}" -- specifies the filter to use for
 *       the searches.  This must be provided.  It may be a simple filter, or it
 *       may be a value pattern to express a range of filters.</LI>
 *   <LI>"-A {name}" or "--attribute {name}" -- specifies the name of an
 *       attribute that should be included in entries returned from the server.
 *       If this is not provided, then all user attributes will be requested.
 *       This may include special tokens that the server may interpret, like
 *       "1.1" to indicate that no attributes should be returned, "*", for all
 *       user attributes, or "+" for all operational attributes.  Multiple
 *       attributes may be requested with multiple instances of this
 *       argument.</LI>
 *   <LI>"-m {name}" or "--modifyAttribute {name}" -- specifies the name of the
 *       attribute to modify.  Multiple attributes may be modified by providing
 *       multiple instances of this argument.  At least one attribute must be
 *       provided.</LI>
 *   <LI>"-l {num}" or "--valueLength {num}" -- specifies the length in bytes to
 *       use for the values of the target attributes to modify.  If this is not
 *       provided, then a default length of 10 bytes will be used.</LI>
 *   <LI>"-C {chars}" or "--characterSet {chars}" -- specifies the set of
 *       characters that will be used to generate the values to use for the
 *       target attributes to modify.  It should only include ASCII characters.
 *       Values will be generated from randomly-selected characters from this
 *       set.  If this is not provided, then a default set of lowercase
 *       alphabetic characters will be used.</LI>
 *   <LI>"-t {num}" or "--numThreads {num}" -- specifies the number of
 *       concurrent threads to use when performing the searches.  If this is not
 *       provided, then a default of one thread will be used.</LI>
 *   <LI>"-i {sec}" or "--intervalDuration {sec}" -- specifies the length of
 *       time in seconds between lines out output.  If this is not provided,
 *       then a default interval duration of five seconds will be used.</LI>
 *   <LI>"-I {num}" or "--numIntervals {num}" -- specifies the maximum number of
 *       intervals for which to run.  If this is not provided, then it will
 *       run forever.</LI>
 *   <LI>"-r {ops-per-second}" or "--ratePerSecond {ops-per-second}" --
 *       specifies the target number of operations to perform per second.  Each
 *       search and modify operation will be counted separately for this
 *       purpose, so if a value of 1 is specified and a search returns two
 *       entries, then a total of three seconds will be required (one for the
 *       search and one for the modify for each entry).  It is still necessary
 *       to specify a sufficient number of threads for achieving this rate.  If
 *       this option is not provided, then the tool will run at the maximum rate
 *       for the specified number of threads.</LI>
 *   <LI>"--warmUpIntervals {num}" -- specifies the number of intervals to
 *       complete before beginning overall statistics collection.</LI>
 *   <LI>"--timestampFormat {format}" -- specifies the format to use for
 *       timestamps included before each output line.  The format may be one of
 *       "none" (for no timestamps), "with-date" (to include both the date and
 *       the time), or "without-date" (to include only time time).</LI>
 *   <LI>"-Y {authzID}" or "--proxyAs {authzID}" -- Use the proxied
 *       authorization v2 control to request that the operations be processed
 *       using an alternate authorization identity.  In this case, the bind DN
 *       should be that of a user that has permission to use this control.  The
 *       authorization identity may be a value pattern.</LI>
 *   <LI>"--suppressErrorResultCodes" -- Indicates that information about the
 *       result codes for failed operations should not be displayed.</LI>
 *   <LI>"-c" or "--csv" -- Generate output in CSV format rather than a
 *       display-friendly format.</LI>
 * </UL>
 */
@ThreadSafety(level=ThreadSafetyLevel.NOT_THREADSAFE)
public final class SearchAndModRate
       extends LDAPCommandLineTool
       implements Serializable
{
  /**
   * The serial version UID for this serializable class.
   */
  private static final long serialVersionUID = 3242469381380526294L;



  // The argument used to indicate whether to generate output in CSV format.
  private BooleanArgument csvFormat;

  // The argument used to indicate whether to suppress information about error
  // result codes.
  private BooleanArgument suppressErrors;

  // The argument used to specify the collection interval.
  private IntegerArgument collectionInterval;

  // The argument used to specify the number of intervals.
  private IntegerArgument numIntervals;

  // The argument used to specify the number of threads.
  private IntegerArgument numThreads;

  // The target rate of operations per second.
  private IntegerArgument ratePerSecond;

  // The argument used to specify the length of the values to generate.
  private IntegerArgument valueLength;

  // The number of warm-up intervals to perform.
  private IntegerArgument warmUpIntervals;

  // The argument used to specify the base DNs for the searches.
  private StringArgument baseDN;

  // The argument used to specify the set of characters to use when generating
  // values.
  private StringArgument characterSet;

  // The argument used to specify the filters for the searches.
  private StringArgument filter;

  // The argument used to specify the attributes to modify.
  private StringArgument modifyAttributes;

  // The argument used to specify the proxied authorization identity.
  private StringArgument proxyAs;

  // The argument used to specify the attributes to return.
  private StringArgument returnAttributes;

  // The argument used to specify the scope for the searches.
  private StringArgument scopeStr;

  // The argument used to specify the timestamp format.
  private StringArgument timestampFormat;



  /**
   * Parse the provided command line arguments and make the appropriate set of
   * changes.
   *
   * @param  args  The command line arguments provided to this program.
   */
  public static void main(final String[] args)
  {
    final ResultCode resultCode = main(args, System.out, System.err);
    if (resultCode != ResultCode.SUCCESS)
    {
      System.exit(resultCode.intValue());
    }
  }



  /**
   * Parse the provided command line arguments and make the appropriate set of
   * changes.
   *
   * @param  args       The command line arguments provided to this program.
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   *
   * @return  A result code indicating whether the processing was successful.
   */
  public static ResultCode main(final String[] args,
                                final OutputStream outStream,
                                final OutputStream errStream)
  {
    final SearchAndModRate searchAndModRate =
         new SearchAndModRate(outStream, errStream);
    return searchAndModRate.runTool(args);
  }



  /**
   * Creates a new instance of this tool.
   *
   * @param  outStream  The output stream to which standard out should be
   *                    written.  It may be {@code null} if output should be
   *                    suppressed.
   * @param  errStream  The output stream to which standard error should be
   *                    written.  It may be {@code null} if error messages
   *                    should be suppressed.
   */
  public SearchAndModRate(final OutputStream outStream,
                          final OutputStream errStream)
  {
    super(outStream, errStream);
  }



  /**
   * Retrieves the name for this tool.
   *
   * @return  The name for this tool.
   */
  @Override()
  public String getToolName()
  {
    return "search-and-mod-rate";
  }



  /**
   * Retrieves the description for this tool.
   *
   * @return  The description for this tool.
   */
  @Override()
  public String getToolDescription()
  {
    return "Perform repeated searches against an " +
           "LDAP directory server and modify each entry returned.";
  }



  /**
   * Adds the arguments used by this program that aren't already provided by the
   * generic {@code LDAPCommandLineTool} framework.
   *
   * @param  parser  The argument parser to which the arguments should be added.
   *
   * @throws  ArgumentException  If a problem occurs while adding the arguments.
   */
  @Override()
  public void addNonLDAPArguments(final ArgumentParser parser)
         throws ArgumentException
  {
    String description = "The base DN to use for the searches.  It may be a " +
         "simple DN or a value pattern to specify a range of DNs (e.g., " +
         "\"uid=user.[1-1000],ou=People,dc=example,dc=com\").  This must be " +
         "provided.";
    baseDN = new StringArgument('b', "baseDN", true, 1, "{dn}", description);
    parser.addArgument(baseDN);


    description = "The scope to use for the searches.  It should be 'base', " +
                  "'one', 'sub', or 'subord'.  If this is not provided, then " +
                  "a default scope of 'sub' will be used.";
    final LinkedHashSet<String> allowedScopes = new LinkedHashSet<String>(4);
    allowedScopes.add("base");
    allowedScopes.add("one");
    allowedScopes.add("sub");
    allowedScopes.add("subord");
    scopeStr = new StringArgument('s', "scope", false, 1, "{scope}",
                                  description, allowedScopes, "sub");
    parser.addArgument(scopeStr);


    description = "The filter to use for the searches.  It may be a simple " +
                  "filter or a value pattern to specify a range of filters " +
                  "(e.g., \"(uid=user.[1-1000])\").  This must be provided.";
    filter = new StringArgument('f', "filter", true, 1, "{filter}",
                                description);
    parser.addArgument(filter);


    description = "The name of an attribute to include in entries returned " +
                  "from the searches.  Multiple attributes may be requested " +
                  "by providing this argument multiple times.  If no request " +
                  "attributes are provided, then the entries returned will " +
                  "include all user attributes.";
    returnAttributes = new StringArgument('A', "attribute", false, 0, "{name}",
                                          description);
    parser.addArgument(returnAttributes);


    description = "The name of the attribute to modify.  Multiple attributes " +
                  "may be specified by providing this argument multiple " +
                  "times.  At least one attribute must be specified.";
    modifyAttributes = new StringArgument('m', "modifyAttribute", true, 0,
                                          "{name}", description);
    parser.addArgument(modifyAttributes);


    description = "The length in bytes to use when generating values for the " +
                  "modifications.  If this is not provided, then a default " +
                  "length of ten bytes will be used.";
    valueLength = new IntegerArgument('l', "valueLength", true, 1, "{num}",
                                      description, 1, Integer.MAX_VALUE, 10);
    parser.addArgument(valueLength);


    description = "The set of characters to use to generate the values for " +
                  "the modifications.  It should only include ASCII " +
                  "characters.  If this is not provided, then a default set " +
                  "of lowercase alphabetic characters will be used.";
    characterSet = new StringArgument('C', "characterSet", true, 1, "{chars}",
                                      description,
                                      "abcdefghijklmnopqrstuvwxyz");
    parser.addArgument(characterSet);


    description = "The number of threads to use to perform the searches.  If " +
                  "this is not provided, then a default of one thread will " +
                  "be used.";
    numThreads = new IntegerArgument('t', "numThreads", true, 1, "{num}",
                                     description, 1, Integer.MAX_VALUE, 1);
    parser.addArgument(numThreads);


    description = "The length of time in seconds between output lines.  If " +
                  "this is not provided, then a default interval of five " +
                  "seconds will be used.";
    collectionInterval = new IntegerArgument('i', "intervalDuration", true, 1,
                                             "{num}", description, 1,
                                             Integer.MAX_VALUE, 5);
    parser.addArgument(collectionInterval);


    description = "The maximum number of intervals for which to run.  If " +
                  "this is not provided, then the tool will run until it is " +
                  "interrupted.";
    numIntervals = new IntegerArgument('I', "numIntervals", true, 1, "{num}",
                                       description, 1, Integer.MAX_VALUE,
                                       Integer.MAX_VALUE);
    parser.addArgument(numIntervals);

    description = "The target number of searches to perform per second.  It " +
                  "is still necessary to specify a sufficient number of " +
                  "threads for achieving this rate.  If this option is not " +
                  "provided, then the tool will run at the maximum rate for " +
                  "the specified number of threads.";
    ratePerSecond = new IntegerArgument('r', "ratePerSecond", false, 1,
                                        "{searches-per-second}", description,
                                        1, Integer.MAX_VALUE);
    parser.addArgument(ratePerSecond);

    description = "The number of intervals to complete before beginning " +
                  "overall statistics collection.  Specifying a nonzero " +
                  "number of warm-up intervals gives the client and server " +
                  "a chance to warm up without skewing performance results.";
    warmUpIntervals = new IntegerArgument(null, "warmUpIntervals", true, 1,
         "{num}", description, 0, Integer.MAX_VALUE, 0);
    parser.addArgument(warmUpIntervals);

    description = "Indicates the format to use for timestamps included in " +
                  "the output.  A value of 'none' indicates that no " +
                  "timestamps should be included.  A value of 'with-date' " +
                  "indicates that both the date and the time should be " +
                  "included.  A value of 'without-date' indicates that only " +
                  "the time should be included.";
    final LinkedHashSet<String> allowedFormats = new LinkedHashSet<String>(3);
    allowedFormats.add("none");
    allowedFormats.add("with-date");
    allowedFormats.add("without-date");
    timestampFormat = new StringArgument(null, "timestampFormat", true, 1,
         "{format}", description, allowedFormats, "none");
    parser.addArgument(timestampFormat);

    description = "Indicates that the proxied authorization control (as " +
                  "defined in RFC 4370) should be used to request that " +
                  "operations be processed using an alternate authorization " +
                  "identity.";
    proxyAs = new StringArgument('Y', "proxyAs", false, 1, "{authzID}",
                                 description);
    parser.addArgument(proxyAs);

    description = "Indicates that information about the result codes for " +
                  "failed operations should not be displayed.";
    suppressErrors = new BooleanArgument(null,
         "suppressErrorResultCodes", 1, description);
    parser.addArgument(suppressErrors);

    description = "Generate output in CSV format rather than a " +
                  "display-friendly format";
    csvFormat = new BooleanArgument('c', "csv", 1, description);
    parser.addArgument(csvFormat);
  }



  /**
   * Indicates whether this tool supports creating connections to multiple
   * servers.  If it is to support multiple servers, then the "--hostname" and
   * "--port" arguments will be allowed to be provided multiple times, and
   * will be required to be provided the same number of times.  The same type of
   * communication security and bind credentials will be used for all servers.
   *
   * @return  {@code true} if this tool supports creating connections to
   *          multiple servers, or {@code false} if not.
   */
  @Override()
  protected boolean supportsMultipleServers()
  {
    return true;
  }



  /**
   * Retrieves the connection options that should be used for connections
   * created for use with this tool.
   *
   * @return  The connection options that should be used for connections created
   *          for use with this tool.
   */
  @Override()
  public LDAPConnectionOptions getConnectionOptions()
  {
    final LDAPConnectionOptions options = new LDAPConnectionOptions();
    options.setAutoReconnect(true);
    options.setUseSynchronousMode(true);
    return options;
  }



  /**
   * Performs the actual processing for this tool.  In this case, it gets a
   * connection to the directory server and uses it to perform the requested
   * searches.
   *
   * @return  The result code for the processing that was performed.
   */
  @Override()
  public ResultCode doToolProcessing()
  {
    // Convert the search scope from a string to an integer.
    final SearchScope scope;
    if (scopeStr.getValue().equalsIgnoreCase("base"))
    {
      scope = SearchScope.BASE;
    }
    else if (scopeStr.getValue().equalsIgnoreCase("one"))
    {
      scope = SearchScope.ONE;
    }
    else if (scopeStr.getValue().equalsIgnoreCase("subord"))
    {
      scope = SearchScope.SUBORDINATE_SUBTREE;
    }
    else
    {
      scope = SearchScope.SUB;
    }


    // Create value patterns for the base DN, filter, and proxied authorization
    // DN.
    final ValuePattern dnPattern;
    try
    {
      dnPattern = new ValuePattern(baseDN.getValue());
    }
    catch (ParseException pe)
    {
      err("Unable to parse the base DN value pattern:  ", pe.getMessage());
      return ResultCode.PARAM_ERROR;
    }

    final ValuePattern filterPattern;
    try
    {
      filterPattern = new ValuePattern(filter.getValue());
    }
    catch (ParseException pe)
    {
      err("Unable to parse the filter pattern:  ", pe.getMessage());
      return ResultCode.PARAM_ERROR;
    }

    final ValuePattern authzIDPattern;
    if (proxyAs.isPresent())
    {
      try
      {
        authzIDPattern = new ValuePattern(proxyAs.getValue());
      }
      catch (ParseException pe)
      {
        err("Unable to parse the proxied authorization pattern:  ",
            pe.getMessage());
        return ResultCode.PARAM_ERROR;
      }
    }
    else
    {
      authzIDPattern = null;
    }


    // Get the attributes to return.
    final String[] returnAttrs;
    if (returnAttributes.isPresent())
    {
      final List<String> attrList = returnAttributes.getValues();
      returnAttrs = new String[attrList.size()];
      attrList.toArray(returnAttrs);
    }
    else
    {
      returnAttrs = new String[0];
    }


    // Get the names of the attributes to modify.
    final String[] modAttrs = new String[modifyAttributes.getValues().size()];
    modifyAttributes.getValues().toArray(modAttrs);


    // Get the character set as a byte array.
    final byte[] charSet = getBytes(characterSet.getValue());


    // If the --ratePerSecond option was specified, then limit the rate
    // accordingly.
    FixedRateBarrier fixedRateBarrier = null;
    if (ratePerSecond.isPresent())
    {
      final int intervalSeconds = collectionInterval.getValue();
      final int ratePerInterval = ratePerSecond.getValue() * intervalSeconds;

      fixedRateBarrier =
           new FixedRateBarrier(1000L * intervalSeconds, ratePerInterval);
    }


    // Determine whether to include timestamps in the output and if so what
    // format should be used for them.
    final boolean includeTimestamp;
    final String timeFormat;
    if (timestampFormat.getValue().equalsIgnoreCase("with-date"))
    {
      includeTimestamp = true;
      timeFormat       = "dd/MM/yyyy HH:mm:ss";
    }
    else if (timestampFormat.getValue().equalsIgnoreCase("without-date"))
    {
      includeTimestamp = true;
      timeFormat       = "HH:mm:ss";
    }
    else
    {
      includeTimestamp = false;
      timeFormat       = null;
    }


    // Determine whether any warm-up intervals should be run.
    final long totalIntervals;
    final boolean warmUp;
    int remainingWarmUpIntervals = warmUpIntervals.getValue();
    if (remainingWarmUpIntervals > 0)
    {
      warmUp = true;
      totalIntervals = 0L + numIntervals.getValue() + remainingWarmUpIntervals;
    }
    else
    {
      warmUp = true;
      totalIntervals = 0L + numIntervals.getValue();
    }


    // Create the table that will be used to format the output.
    final OutputFormat outputFormat;
    if (csvFormat.isPresent())
    {
      outputFormat = OutputFormat.CSV;
    }
    else
    {
      outputFormat = OutputFormat.COLUMNS;
    }

    final ColumnFormatter formatter = new ColumnFormatter(includeTimestamp,
         timeFormat, outputFormat, " ",
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Recent",
                  "Searches/Sec"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Recent",
                  "Srch Dur ms"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Recent",
                  "Mods/Sec"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Recent",
                  "Mod Dur ms"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Recent",
                  "Errors/Sec"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Overall",
                  "Searches/Sec"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Overall",
                  "Srch Dur ms"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Overall",
                  "Mods/Sec"),
         new FormattableColumn(12, HorizontalAlignment.RIGHT, "Overall",
                  "Mod Dur ms"));


    // Create values to use for statistics collection.
    final AtomicLong        searchCounter   = new AtomicLong(0L);
    final AtomicLong        errorCounter    = new AtomicLong(0L);
    final AtomicLong        modCounter      = new AtomicLong(0L);
    final AtomicLong        modDurations    = new AtomicLong(0L);
    final AtomicLong        searchDurations = new AtomicLong(0L);
    final ResultCodeCounter rcCounter       = new ResultCodeCounter();


    // Determine the length of each interval in milliseconds.
    final long intervalMillis = 1000L * collectionInterval.getValue();


    // Create the threads to use for the searches.
    final Random random = new Random();
    final CyclicBarrier barrier = new CyclicBarrier(numThreads.getValue() + 1);
    final SearchAndModRateThread[] threads =
         new SearchAndModRateThread[numThreads.getValue()];
    for (int i=0; i < threads.length; i++)
    {
      final LDAPConnection connection;
      try
      {
        connection = getConnection();
      }
      catch (LDAPException le)
      {
        err("Unable to connect to the directory server:  ",
            getExceptionMessage(le));
        return le.getResultCode();
      }

      threads[i] = new SearchAndModRateThread(i, connection, dnPattern, scope,
           filterPattern, returnAttrs, modAttrs, valueLength.getValue(),
           charSet, authzIDPattern, random.nextLong(), barrier, searchCounter,
           modCounter, searchDurations, modDurations, errorCounter, rcCounter,
           fixedRateBarrier);
      threads[i].start();
    }


    // Display the table header.
    for (final String headerLine : formatter.getHeaderLines(true))
    {
      out(headerLine);
    }


    // Indicate that the threads can start running.
    try
    {
      barrier.await();
    } catch (Exception e) {}
    long overallStartTime = System.nanoTime();
    long nextIntervalStartTime = System.currentTimeMillis() + intervalMillis;


    boolean setOverallStartTime = false;
    long    lastSearchDuration  = 0L;
    long    lastModDuration     = 0L;
    long    lastNumEntries      = 0L;
    long    lastNumErrors       = 0L;
    long    lastNumSearches     = 0L;
    long    lastNumMods          = 0L;
    long    lastEndTime         = System.nanoTime();
    for (long i=0; i < totalIntervals; i++)
    {
      final long startTimeMillis = System.currentTimeMillis();
      final long sleepTimeMillis = nextIntervalStartTime - startTimeMillis;
      nextIntervalStartTime += intervalMillis;
      try
      {
        if (sleepTimeMillis > 0)
        {
          Thread.sleep(sleepTimeMillis);
        }
      } catch (Exception e) {}

      final long endTime          = System.nanoTime();
      final long intervalDuration = endTime - lastEndTime;

      final long numSearches;
      final long numMods;
      final long numErrors;
      final long totalSearchDuration;
      final long totalModDuration;
      if (warmUp && (remainingWarmUpIntervals > 0))
      {
        numSearches         = searchCounter.getAndSet(0L);
        numMods             = modCounter.getAndSet(0L);
        numErrors           = errorCounter.getAndSet(0L);
        totalSearchDuration = searchDurations.getAndSet(0L);
        totalModDuration    = modDurations.getAndSet(0L);
      }
      else
      {
        numSearches         = searchCounter.get();
        numMods             = modCounter.get();
        numErrors           = errorCounter.get();
        totalSearchDuration = searchDurations.get();
        totalModDuration    = modDurations.get();
      }

      final long recentNumSearches = numSearches - lastNumSearches;
      final long recentNumMods = numMods - lastNumMods;
      final long recentNumErrors = numErrors - lastNumErrors;
      final long recentSearchDuration =
           totalSearchDuration - lastSearchDuration;
      final long recentModDuration = totalModDuration - lastModDuration;

      final double numSeconds = intervalDuration / 1000000000.0D;
      final double recentSearchRate = recentNumSearches / numSeconds;
      final double recentModRate = recentNumMods / numSeconds;
      final double recentErrorRate  = recentNumErrors / numSeconds;
      final double recentAvgSearchDuration =
                  1.0D * recentSearchDuration / recentNumSearches / 1000000;
      final double recentAvgModDuration =
                  1.0D * recentModDuration / recentNumMods / 1000000;

      if (warmUp && (remainingWarmUpIntervals > 0))
      {
        out(formatter.formatRow(recentSearchRate, recentAvgSearchDuration,
             recentModRate, recentAvgModDuration, recentErrorRate, "warming up",
             "warming up", "warming up", "warming up"));

        remainingWarmUpIntervals--;
        if (remainingWarmUpIntervals == 0)
        {
          out("Warm-up completed.  Beginning overall statistics collection.");
          setOverallStartTime = true;
        }
      }
      else
      {
        if (setOverallStartTime)
        {
          overallStartTime    = lastEndTime;
          setOverallStartTime = false;
        }

        final double numOverallSeconds =
             (endTime - overallStartTime) / 1000000000.0D;
        final double overallSearchRate = numSearches / numOverallSeconds;
        final double overallModRate = numMods / numOverallSeconds;
        final double overallAvgSearchDuration =
             1.0D * totalSearchDuration / numSearches / 1000000;
        final double overallAvgModDuration =
             1.0D * totalModDuration / numMods / 1000000;

        out(formatter.formatRow(recentSearchRate, recentAvgSearchDuration,
             recentModRate, recentAvgModDuration, recentErrorRate,
             overallSearchRate, overallAvgSearchDuration, overallModRate,
             overallAvgModDuration));

        lastNumSearches    = numSearches;
        lastNumMods        = numMods;
        lastNumErrors      = numErrors;
        lastSearchDuration = totalSearchDuration;
        lastModDuration    = totalModDuration;
      }

      final List<ObjectPair<ResultCode,Long>> rcCounts =
           rcCounter.getCounts(true);
      if ((! suppressErrors.isPresent()) && (! rcCounts.isEmpty()))
      {
        err("\tError Results:");
        for (final ObjectPair<ResultCode,Long> p : rcCounts)
        {
          err("\t", p.getFirst().getName(), ":  ", p.getSecond());
        }
      }

      lastEndTime = endTime;
    }


    // Stop all of the threads.
    ResultCode resultCode = ResultCode.SUCCESS;
    for (final SearchAndModRateThread t : threads)
    {
      final ResultCode r = t.stopRunning();
      if (resultCode == ResultCode.SUCCESS)
      {
        resultCode = r;
      }
    }

    return resultCode;
  }



  /**
   * {@inheritDoc}
   */
  @Override()
  public LinkedHashMap<String[],String> getExampleUsages()
  {
    final LinkedHashMap<String[],String> examples =
         new LinkedHashMap<String[],String>();

    final String[] args =
    {
      "--hostname", "server.example.com",
      "--port", "389",
      "--bindDN", "uid=admin,dc=example,dc=com",
      "--bindPassword", "password",
      "--baseDN", "dc=example,dc=com",
      "--scope", "sub",
      "--filter", "(uid=user.[1-1000000])",
      "--attribute", "givenName",
      "--attribute", "sn",
      "--attribute", "mail",
      "--modifyAttribute", "description",
      "--valueLength", "10",
      "--characterSet", "abcdefghijklmnopqrstuvwxyz0123456789",
      "--numThreads", "10"
    };
    final String description =
         "Test search and modify performance by searching randomly across a " +
         "set of one million users located below 'dc=example,dc=com' with " +
         "ten concurrent threads.  The entries returned to the client will " +
         "include the givenName, sn, and mail attributes, and the " +
         "description attribute of each entry returned will be replaced " +
         "with a string of ten randomly-selected alphanumeric characters.";
    examples.put(args, description);

    return examples;
  }
}
