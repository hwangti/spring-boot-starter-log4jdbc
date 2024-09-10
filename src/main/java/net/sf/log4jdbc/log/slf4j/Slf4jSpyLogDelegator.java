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

package net.sf.log4jdbc.log.slf4j;

import net.sf.log4jdbc.Properties;
import net.sf.log4jdbc.log.SpyLogDelegator;
import net.sf.log4jdbc.sql.Spy;
import net.sf.log4jdbc.sql.jdbcapi.ConnectionSpy;
import net.sf.log4jdbc.sql.jdbcapi.ResultSetSpy;
import net.sf.log4jdbc.sql.resultsetcollector.ResultSetCollector;
import net.sf.log4jdbc.sql.resultsetcollector.ResultSetCollectorPrinter;
import org.hibernate.engine.jdbc.internal.FormatStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.StringTokenizer;
import java.util.regex.Pattern;

/**
 * Delegates JDBC spy logging events to the Simple Logging Facade for Java (slf4j).
 *
 * @author Arthur Blake
 * @author Frederic Bastian
 * @author Mathieu Seppey
 */
public class Slf4jSpyLogDelegator implements SpyLogDelegator {

    /**
     * Logger that shows all JDBC calls on INFO level (exception ResultSet calls)
     */
    private final Logger auditLogger = LoggerFactory.getLogger("jdbc.audit");

    /**
     * Logger that shows connection open and close events as well as current number of open connections.
     */
    private final Logger connectionLogger = LoggerFactory.getLogger("jdbc.connection");

    /**
     * Logger that shows JDBC calls for ResultSet operations
     */
    private final Logger resultSetLogger = LoggerFactory.getLogger("jdbc.resultset");

    /**
     * Logger that shows the forward scrolled result sets in a table
     */
    private final Logger resultSetTableLogger = LoggerFactory.getLogger("jdbc.resultsettable");

    /**
     * Logger that shows only the SQL that is occurring
     */
    private final Logger sqlOnlyLogger = LoggerFactory.getLogger("jdbc.sqlonly");

    /**
     * Logger that shows the SQL timing, post execution
     */
    private final Logger sqlTimingLogger = LoggerFactory.getLogger("jdbc.sqltiming");

    /**
     * Logger just for debugging things within log4jdbc itself (admin, setup, etc.)
     */
    private final Logger debugLogger = LoggerFactory.getLogger("log4jdbc.debug");

    private static final String nl = System.lineSeparator();

    /**
     * Create a SpyLogDelegator specific to the Simple Logging Facade for Java (slf4j).
     */
    public Slf4jSpyLogDelegator() {
    }

    /**
     * Determine if any of the 5 log4jdbc spy loggers are turned on
     * (jdbc.audit | jdbc.connection | jdbc.resultset | jdbc.sqlonly | jdbc.sqltiming)
     *
     * @return true if any of the 5 spy jdbc/sql loggers are enabled at debug info or error level.
     */
    @Override
    public boolean isJdbcLoggingEnabled() {
        return auditLogger.isErrorEnabled() ||
                connectionLogger.isErrorEnabled() ||
                resultSetLogger.isErrorEnabled() ||
                sqlOnlyLogger.isErrorEnabled() ||
                sqlTimingLogger.isErrorEnabled();
    }

    @Override
    public void exceptionOccurred(Spy spy, String methodCall, Exception e, String sql, long execTime) {
        String classType = spy.getClassType();
        Integer spyNo = spy.getConnectionNumber();
        String header = spyNo + ". " + classType + "." + methodCall;
        if (sql == null) {
            auditLogger.error(header, e);
            sqlOnlyLogger.error(header, e);
            sqlTimingLogger.error(header, e);
        } else {
            sql = processSql(sql);
            auditLogger.error("{} {}", header, sql, e);

            // if at debug level, display debug info to error log
            if (sqlOnlyLogger.isDebugEnabled()) {
                sqlOnlyLogger.error("{}{}{}. {}", getDebugInfo(), nl, spyNo, sql, e);
            } else {
                sqlOnlyLogger.error("{} {}", header, sql, e);
            }

            // if at debug level, display debug info to error log
            if (sqlTimingLogger.isDebugEnabled()) {
                sqlTimingLogger.error("{}{}{}. {} {FAILED after {} msec}", getDebugInfo(), nl, spyNo, sql, execTime, e);
            } else {
                sqlTimingLogger.error("{} FAILED! {} {FAILED after {} msec}", header, sql, execTime, e);
            }
        }
    }

    @Override
    public void methodReturned(Spy spy, String methodCall, String returnMsg) {
        String classType = spy.getClassType();
        Logger logger = ResultSetSpy.classTypeDescription.equals(classType) ?
                resultSetLogger : auditLogger;
        if (logger.isInfoEnabled()) {
            String header = spy.getConnectionNumber() + ". " + classType + "." +
                    methodCall + " returned " + returnMsg;
            if (logger.isDebugEnabled()) {
                logger.debug("{} {}", header, getDebugInfo());
            } else {
                logger.info(header);
            }
        }
    }

    @Override
    public void constructorReturned(Spy spy, String constructionInfo) {
        // not used in this implementation -- yet
    }

    /**
     * Called whenever a new connection spy is created.
     *
     * @param spy      ConnectionSpy that was created.
     * @param execTime A <code>long</code> defining the time elapsed to open the connection in ms
     *                 (useful information, as a connection might take some time to be opened sometimes).
     *                 Caller should pass -1 if not used or unknown.
     */
    @Override
    public void connectionOpened(Spy spy, long execTime) {
        // we just delegate to the already existing method,
        // so that we do not change the behavior of the standard implementation
        this.connectionOpened(spy);
    }

    /**
     * Called whenever a new connection spy is created.
     *
     * @param spy ConnectionSpy that was created.
     */
    private void connectionOpened(Spy spy) {
        if (connectionLogger.isDebugEnabled()) {
            connectionLogger.info("{}. Connection opened {}", spy.getConnectionNumber(), getDebugInfo());
            connectionLogger.debug(ConnectionSpy.getOpenConnectionsDump());
        } else {
            connectionLogger.info("{}. Connection opened", spy.getConnectionNumber());
        }
    }

    @Override
    public void connectionClosed(Spy spy, long execTime) {
        // we just delegate to the already existing method,
        // so that we do not change the behavior of the standard implementation
        this.connectionClosed(spy);
    }

    /**
     * Called whenever a connection spy is closed.
     *
     * @param spy ConnectionSpy that was closed.
     */
    private void connectionClosed(Spy spy) {
        if (connectionLogger.isDebugEnabled()) {
            connectionLogger.info("{}. Connection closed {}", spy.getConnectionNumber(), getDebugInfo());
            connectionLogger.debug(ConnectionSpy.getOpenConnectionsDump());
        } else {
            connectionLogger.info("{}. Connection closed", spy.getConnectionNumber());
        }
    }

    @Override
    public void connectionAborted(Spy spy, long execTime) {
        // we just delegate to the already existing method,
        // so that we do not change the behavior of the standard implementation
        this.connectionAborted(spy);
    }

    /**
     * Called whenever a connection spy is aborted.
     *
     * @param spy ConnectionSpy that was aborted.
     */
    private void connectionAborted(Spy spy) {
        if (connectionLogger.isDebugEnabled()) {
            connectionLogger.info("{}. Connection aborted {}", spy.getConnectionNumber(), getDebugInfo());
            connectionLogger.debug(ConnectionSpy.getOpenConnectionsDump());
        } else {
            connectionLogger.info("{}. Connection aborted", spy.getConnectionNumber());
        }
    }

    @Override
    public void sqlOccurred(Spy spy, String methodCall, String sql) {
        if (!Properties.isDumpSqlFilteringOn() || shouldSqlBeLogged(sql)) {
            if (sqlOnlyLogger.isDebugEnabled()) {
                sqlOnlyLogger.debug("{}{}{}. {}", getDebugInfo(), nl, spy.getConnectionNumber(), processSql(sql));
            } else if (sqlOnlyLogger.isInfoEnabled()) {
                sqlOnlyLogger.info(processSql(sql));
            }
        }
    }

    /**
     * Special call that is called only for JDBC method calls that contain SQL.
     *
     * @param spy        the Spy wrapping the class where the SQL occurred.
     * @param execTime   how long it took the SQL to run, in milliseconds.
     * @param methodCall a description of the name and call parameters of the method that generated the SQL.
     * @param sql        SQL that occurred.
     */
    @Override
    public void sqlTimingOccurred(Spy spy, long execTime, String methodCall, String sql) {
        if (sqlTimingLogger.isErrorEnabled() &&
                !methodCall.startsWith("getGeneratedKeys") &&
                (!Properties.isDumpSqlFilteringOn() || shouldSqlBeLogged(sql))) {

            boolean shouldLogError = Properties.isSqlTimingErrorThresholdEnabled() &&
                    execTime >= Properties.getSqlTimingErrorThresholdMsec();

            boolean shouldLogWarn = Properties.isSqlTimingWarnThresholdEnabled() &&
                    execTime >= Properties.getSqlTimingWarnThresholdMsec();

            boolean isDebug = sqlTimingLogger.isDebugEnabled();
            boolean isInfo = sqlTimingLogger.isInfoEnabled();

            // Determine log level and build the SQL timing dump once
            String sqlTimingDump = buildSqlTimingDump(spy, execTime, methodCall, sql, isDebug);

            if (shouldLogError) {
                sqlTimingLogger.error(sqlTimingDump);
            } else if (sqlTimingLogger.isWarnEnabled()) {
                if (shouldLogWarn) {
                    sqlTimingLogger.warn(sqlTimingDump);
                } else if (isDebug) {
                    sqlTimingLogger.debug(sqlTimingDump);
                } else if (isInfo) {
                    sqlTimingLogger.info(sqlTimingDump);
                }
            }
        }
    }

    /**
     * Helper method to quickly build a SQL timing dump output String for logging.
     *
     * @param spy        the Spy wrapping the class where the SQL occurred.
     * @param execTime   how long it took the SQL to run, in milliseconds.
     * @param methodCall a description of the name and call parameters of the method that generated the SQL.
     * @param sql        SQL that occurred.
     * @param debugInfo  if true, include debug info at the front of the output.
     * @return a SQL timing dump String for logging.
     */
    private String buildSqlTimingDump(Spy spy, long execTime, String methodCall, String sql, boolean debugInfo) {
        StringBuilder out = new StringBuilder();

        out.append(spy.getConnectionNumber());
        out.append(". executed in ");
        out.append(execTime);
        out.append(" ms | ");
        out.append(methodCall);

        if (debugInfo) {
            out.append(" |");
            out.append(getDebugInfo());
        }

        // NOTE: if both sql dump and sql timing dump are on, the processSql algorithm
        // will run TWICE once at the beginning and once at the end this is not very efficient
        // but usually only one or the other dump should be on and not both.

        out.append(nl);
        out.append(processSql(sql));

        return out.toString();
    }

    @Override
    public boolean isResultSetCollectionEnabled() {
        return resultSetTableLogger.isInfoEnabled();
    }

    @Override
    public boolean isResultSetCollectionEnabledWithUnreadValueFillIn() {
        return resultSetTableLogger.isDebugEnabled();
    }

    @Override
    public void resultSetCollected(ResultSetCollector resultSetCollector) {
        String resultsToPrint = new ResultSetCollectorPrinter().getResultSetToPrint(resultSetCollector);
        resultSetTableLogger.info(resultsToPrint);
    }

    @Override
    public void debug(String msg) {
        debugLogger.debug(msg);
    }

    /**
     * Get debugging info - the module and line number that called the logger
     * version that prints the stack trace information from the point just before
     * we got it (net.sf.log4jdbc)
     * <p>
     * if the optional log4jdbc.debug.stack.prefix system property is defined then
     * the last call point from an application is shown in the debug
     * trace output, instead of the last direct caller into log4jdbc
     *
     * @return debugging info for whoever called into JDBC from within the application.
     */
    private static String getDebugInfo() {
        Throwable t = new Throwable();
        t.fillInStackTrace();
        StackTraceElement[] stackTrace = t.getStackTrace();

        StringBuilder dump = new StringBuilder();

        /*
         * The DumpFullDebugStackTrace option is useful in some situations when we want to see the full stack trace
         * in the debug info-watch out though as this will make the logs HUGE!
         */
        if (Properties.isDumpFullDebugStackTrace()) {
            boolean first = true;
            for (StackTraceElement element : stackTrace) {
                String className = element.getClassName();
                if (!className.startsWith("net.sf.log4jdbc")) {
                    if (first) {
                        first = false;
                    } else {
                        dump.append("  ");
                    }
                    dump.append("at ").append(element).append(nl);
                }
            }
        } else {
            dump.append(" ");
            int firstLog4jdbcCall = -1;
            int lastApplicationCall = -1;

            for (int i = 0; i < stackTrace.length; i++) {
                String className = stackTrace[i].getClassName();
                if (className.startsWith("net.sf.log4jdbc")) {
                    firstLog4jdbcCall = i;
                } else if (Properties.isTraceFromApplication() &&
                        Pattern.matches(Properties.getDebugStackPrefix(), className)) {
                    lastApplicationCall = i;
                    break;
                }
            }

            // if app not found, then use whoever was the last guy that called a log4jdbc class.
            int targetIndex = lastApplicationCall != -1 ? lastApplicationCall : firstLog4jdbcCall + 1;

            if (targetIndex < stackTrace.length) {
                StackTraceElement traceElement = stackTrace[targetIndex];
                dump.append(traceElement.getClassName())
                        .append(".")
                        .append(traceElement.getMethodName())
                        .append("(").append(traceElement.getFileName())
                        .append(":").append(traceElement.getLineNumber())
                        .append(")");
            }
        }

        return dump.toString();
    }

    /**
     * Break an SQL statement up into multiple lines in an attempt to make it
     * more readable
     *
     * @param sql SQL to break up.
     * @return SQL broken up into multiple lines
     */
    private String processSql(String sql) {
        if (sql == null) {
            return null;
        }

        StringBuilder output = new StringBuilder();

        if (Properties.isDumpSqlAddSemicolon()) {
            output.append(";");
        }

        if (Properties.isFormatSql()) {
            if (isDDL(sql)) {
                output.insert(0, FormatStyle.DDL.getFormatter().format(sql));
            } else {
                output.insert(0, FormatStyle.BASIC.getFormatter().format(sql));
            }
        } else {
            output.insert(0, sql);
        }

        if (Properties.isSqlTrim()) {
            output = new StringBuilder(output.toString().trim());
        }

        if (Properties.isHighlightSql()) {
            output = new StringBuilder(FormatStyle.HIGHLIGHT.getFormatter().format(output.toString()));
        }

        if (Properties.getDumpSqlMaxLineLength() > 0) {
            // insert line breaks into sql to make it more readable
            StringTokenizer st = new StringTokenizer(output.toString());
            String token;
            int lineLength = 0;
            output = new StringBuilder();

            while (st.hasMoreElements()) {
                token = (String) st.nextElement();

                output.append(token);
                lineLength += token.length();
                output.append(" ");
                lineLength++;
                if (lineLength > Properties.getDumpSqlMaxLineLength()) {
                    output.append(nl);
                    lineLength = 0;
                }
            }
        }

        if (Properties.isTrimExtraBlankLinesInSql()) {
            LineNumberReader lineReader = new LineNumberReader(new StringReader(output.toString()));
            int contiguousBlankLines = 0;
            output = new StringBuilder();

            try {
                while (true) {
                    String line = lineReader.readLine();
                    if (line == null) {
                        break;
                    }

                    // is this line blank?
                    if (line.trim().isEmpty()) {
                        contiguousBlankLines++;
                        // skip contiguous blank lines
                        if (contiguousBlankLines > 1) {
                            continue;
                        }
                    } else {
                        contiguousBlankLines = 0;
                        output.append(line);
                    }
                    output.append(nl);
                }
            } catch (IOException e) {
                // since we are reading from a buffer, this isn't likely to happen,
                // but if it does we just ignore it and treat it like its the end of the stream
            }
        }

        if (Properties.isFormatSql()) {
            if (Properties.getDumpSqlMaxLineLength() <= 0) {
                output.insert(0, "    ");
            }
            output.insert(0, nl);
        }

        if (Properties.isHighlightSql()) {
            output.insert(0, "\033[35m[log4jdbc]\033[0m ");
        } else {
            output.insert(0, "[log4jdbc] ");
        }

        return output.toString();
    }

    /**
     * Determine if the given sql should be logged or not
     * based on the various DumpSqlXXXXXX flags.
     *
     * @param sql SQL to test.
     * @return true if the SQL should be logged, false if not.
     */
    private boolean shouldSqlBeLogged(String sql) {
        if (sql == null) {
            return false;
        }
        sql = sql.trim();

        if (sql.length() < 6) {
            return false;
        }
        sql = sql.substring(0, 6).toLowerCase();
        return
                (Properties.isDumpSqlSelect() && "select".equals(sql)) ||
                        (Properties.isDumpSqlInsert() && "insert".equals(sql)) ||
                        (Properties.isDumpSqlUpdate() && "update".equals(sql)) ||
                        (Properties.isDumpSqlDelete() && "delete".equals(sql)) ||
                        (Properties.isDumpSqlCreate() && "create".equals(sql));
    }

    private boolean isDDL(String lowerSql) {
        return lowerSql.startsWith("create")
                || lowerSql.startsWith("alter")
                || lowerSql.startsWith("drop")
                || lowerSql.startsWith("comment");
    }

}
