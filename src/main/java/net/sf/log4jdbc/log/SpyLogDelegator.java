/*
 * Copyright 2007-2012 Arthur Blake
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.sf.log4jdbc.log;

import net.sf.log4jdbc.sql.Spy;
import net.sf.log4jdbc.sql.resultsetcollector.ResultSetCollector;

/**
 * Delegates Spy events to a logger.
 * This interface is used for all logging activity used by log4jdbc and hides the specific implementation
 * of any given logging system from log4jdbc.
 *
 * @author Arthur Blake
 * @author Frederic Bastian
 * @author Mathieu Seppey
 */
public interface SpyLogDelegator {

    /**
     * Determine if any of the jdbc or sql loggers are turned on.
     *
     * @return true if any of the jdbc or sql loggers are enabled at error level or higher.
     */
    boolean isJdbcLoggingEnabled();

    /**
     * Called when a spied upon method throws an Exception.
     *
     * @param spy        the Spy wrapping the class that threw an Exception.
     * @param methodCall a description of the name and call parameters of the method generated the Exception.
     * @param e          the Exception that was thrown.
     * @param sql        optional sql that occured just before the exception occured.
     * @param execTime   optional amount of time that passed before an exception was thrown when sql was being executed.
     *                   caller should pass -1 if not used
     */
    void exceptionOccurred(Spy spy, String methodCall, Exception e, String sql, long execTime);

    /**
     * Called when spied upon method call returns.
     *
     * @param spy        the Spy wrapping the class that called the method that returned.
     * @param methodCall a description of the name and call parameters of the method that returned.
     * @param returnMsg  return value converted to a String for integral types, or String representation for Object
     *                   return types this will be null for void return types.
     */
    void methodReturned(Spy spy, String methodCall, String returnMsg);

    /**
     * Called when a spied upon object is constructed.
     *
     * @param spy              the Spy wrapping the class that called the method that returned.
     * @param constructionInfo information about the object construction
     */
    void constructorReturned(Spy spy, String constructionInfo);

    /**
     * Special call that is called only for JDBC method calls that contain SQL.
     *
     * @param spy        the Spy wrapping the class where the SQL occurred.
     * @param methodCall a description of the name and call parameters of the method that generated the SQL.
     * @param sql        sql that occurred.
     */
    void sqlOccurred(Spy spy, String methodCall, String sql);

    /**
     * Similar to sqlOccured, but reported after SQL executes and used to report timing stats on the SQL
     *
     * @param spy the    Spy wrapping the class where the SQL occurred.
     * @param execTime   how long it took the sql to run, in ms.
     * @param methodCall a description of the name and call parameters of the method that generated the SQL.
     * @param sql        sql that occurred.
     */
    void sqlTimingOccurred(Spy spy, long execTime, String methodCall, String sql);

    /**
     * Called whenever a new connection spy is created.
     *
     * @param spy ConnectionSpy that was created.
     * @param execTime  A <code>long</code> defining the time elapsed to open the connection in ms
     *                  (useful information, as a connection might take some time to be opened sometimes).
     *                  Caller should pass -1 if not used or unknown.
     */
    void connectionOpened(Spy spy, long execTime);

    /**
     * Called whenever a connection spy is closed.
     *
     * @param spy       <code>ConnectionSpy</code> that was closed.
     * @param execTime  A <code>long</code> defining the time elapsed to close the connection in ms
     *                  (useful information, as a connection might take some time to be closed sometimes).
     *                  Caller should pass -1 if not used or unknown.
     */
    void connectionClosed(Spy spy, long execTime);

    /**
     * Called whenever a connection spy is aborted.
     *
     * @param spy       <code>ConnectionSpy</code> that was aborted.
     * @param execTime  A <code>long</code> defining the time elapsed to abort the connection in ms
     *                  (useful information, as a connection might take some time to be aborted sometimes).
     *                  Caller should pass -1 if not used or unknown.
     */
    void connectionAborted(Spy spy, long execTime);

    /**
     * Log a Setup and/or administrative log message for log4jdbc.
     *
     * @param msg message to log.
     */
    void debug(String msg);

    /**
     * Determine whether the logger is expecting results sets to be collected
     *
     * @return true if the logger is expecting results sets to be collected
     */
    boolean isResultSetCollectionEnabled();

    /**
     * Determine whether the logger is expecting results sets to be collected
     * AND any unread result set values read explicitly
     *
     * @return true if the logger is expecting results sets to be collected
     */
    boolean isResultSetCollectionEnabledWithUnreadValueFillIn();

    /**
     * Called whenever result set has been collected.
     *
     * <p>This method will be actually called
     * when the <code>next()</code> method of the spied <code>ResultSet</code>
     * return <code>false</code> meaning that its end is reached.
     * It will be also called if the <code>ResultSet</code> is closed.</p>
     *
     * @param resultSetCollector the <code>ResultSetCollector</code> that has collected the result set.
     * @see net.sf.log4jdbc.sql.jdbcapi.ResultSetSpy
     * @see net.sf.log4jdbc.sql.resultsetcollector.DefaultResultSetCollector
     */
    void resultSetCollected(ResultSetCollector resultSetCollector);

}
