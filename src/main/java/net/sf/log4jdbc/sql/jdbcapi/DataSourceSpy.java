/*
 * Copyright 2010 Tim Azzopardi
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

package net.sf.log4jdbc.sql.jdbcapi;

import net.sf.log4jdbc.log.SpyLogDelegator;
import net.sf.log4jdbc.log.SpyLogFactory;
import net.sf.log4jdbc.sql.Spy;

import javax.sql.DataSource;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.util.logging.Logger;

/**
 * A proxy datasource that can be used to wrap a real data source allowing log4jdbc to do its work on the real
 * data source.
 * Inspired by <a href="http://groups.google.com/group/log4jdbc/browse_thread/thread/0706611d1b85e210">...</a>
 * <p>
 * This can be useful in a Spring context. Imagine your spring context includes this datasource definition
 * </p>
 * <pre><code>
 *   &lt;bean id="dataSource" class="com.mchange.v2.c3p0.ComboPooledDataSource"&gt;
 *     &lt;property name="driverClass" value="${datasource.driverClassName}"/&gt;
 *     &lt;property name="jdbcUrl" value="${datasource.url}"/&gt;
 *     &lt;property name="user" value="${datasource.username}"/&gt;
 *     &lt;property name="password" value="${datasource.password}"/&gt;
 *     &lt;property name="initialPoolSize" value="${datasource.initialPoolSize}" /&gt;
 *     &lt;property name="minPoolSize" value="${datasource.minPoolSize}" /&gt;
 *     &lt;property name="maxPoolSize" value="${datasource.maxPoolSize}" /&gt;
 *     &lt;property name="maxStatements" value="${datasource.maxStatements}" /&gt;
 *   &lt;/bean&gt;
 * </code></pre>
 * <p>
 * You can get log4jdbc to work on this using the following config changes
 * <pre><code>
 *   &lt;bean id="dataSourceSpied" class="com.mchange.v2.c3p0.ComboPooledDataSource"&gt;
 *     &lt;property name="driverClass" value="${datasource.driverClassName}"/&gt;
 *     &lt;property name="jdbcUrl" value="${datasource.url}"/&gt;
 *     &lt;property name="user" value="${datasource.username}"/&gt;
 *     &lt;property name="password" value="${datasource.password}"/&gt;
 *     &lt;property name="initialPoolSize" value="${datasource.initialPoolSize}" /&gt;
 *     &lt;property name="minPoolSize" value="${datasource.minPoolSize}" /&gt;
 *     &lt;property name="maxPoolSize" value="${datasource.maxPoolSize}" /&gt;
 *     &lt;property name="maxStatements" value="${datasource.maxStatements}" /&gt;
 *   &lt;/bean&gt;
 *   &lt;bean id="dataSource" class="net.sf.log4jdbc.Log4jdbcProxyDataSource"&gt;
 *     &lt;constructor-arg ref="dataSourceSpied" /&gt;
 *   &lt;/bean&gt;
 * </code></pre>
 *
 * @author tim.azzopardi Log4jdbcProxyDataSource.java
 * @author Mathieu Seppey renamed and modified
 */
public class DataSourceSpy implements DataSource, Spy {

    private final DataSource realDataSource;

    /**
     * The <code>SpyLogDelegator</code> used by this <code>DataSource</code>
     * and used by all resources obtained starting from this <code>DataSource</code>
     * (<code>Connection</code>s, <code>ResultSet</code>s, ...).
     */
    private SpyLogDelegator log;

    /**
     * Constructor
     *
     * @param realDataSource the real DataSource
     */
    public DataSourceSpy(DataSource realDataSource) {
        this.realDataSource = realDataSource;
        this.log = SpyLogFactory.getSpyLogDelegator();
    }

    /**
     * Get the <code>SpyLogDelegator</code> that are used by all resources
     * obtained starting from this <code>DataSource</code>
     * (<code>Connection</code>s, <code>ResultSet</code>s, ...).
     *
     * @return The <code>SpyLogDelegator</code> currently used
     * by this <code>DataSource</code>.
     */
    public SpyLogDelegator getLogDelegator() {
        return this.log;
    }

    /**
     * Set a custom <code>SpyLogDelegator</code> to be used by all resources
     * provided by this <code>DataSource</code>, rather than the default
     * <code>SpyLogDelegator</code> returned by
     * {@link SpyLogFactory#getSpyLogDelegator()
     * SpyLogFactory#getSpyLogDelegator()}.
     *
     * @param spyLogDelegator The <code>SpyLogDelegator</code> to be used by all resources
     *                        obtained starting from this <code>DataSource</code>
     *                        (<code>Connection</code>s, <code>ResultSet</code>s, ...).
     */
    public void setLogDelegator(SpyLogDelegator spyLogDelegator) {
        this.log = spyLogDelegator;
    }

    /**
     * Report to the logger all exceptions which have to be reported by this class
     *
     * @param methodCall the method which threw an exception
     * @param exception  the thrown exception
     */
    protected void reportException(String methodCall, SQLException exception) {
        log.exceptionOccurred(this, methodCall, exception, null, -1L);
    }

    /**
     * Report to the logger all returns which have to be reported by this class
     * and which are not exceptions
     *
     * @param methodCall the method which report the value
     * @param value      the reported value
     * @return Object The object originally returned by the method called
     */
    private Object reportReturn(String methodCall, Object value) {
        log.methodReturned(this, methodCall, "");
        return value;
    }

    @Override
    public Connection getConnection() throws SQLException {
        String methodCall = "getConnection()";
        long tstart = System.currentTimeMillis();
        try {
            final Connection connection = realDataSource.getConnection();
            if (log.isJdbcLoggingEnabled()) {
                return (Connection) reportReturn(methodCall,
                        new ConnectionSpy(connection, DriverSpy.getRdbmsSpecifics(connection),
                                System.currentTimeMillis() - tstart, this.log));
            }
            //if logging is not enable, return the real connection,
            //so that there is no useless costs
            return connection;
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public Connection getConnection(String username, String password) throws SQLException {

        String methodCall = "getConnection(" + username + ", password***)";
        long tstart = System.currentTimeMillis();
        try {
            final Connection connection = realDataSource.getConnection(username, password);
            if (log.isJdbcLoggingEnabled()) {
                return (Connection) reportReturn(methodCall,
                        new ConnectionSpy(connection, DriverSpy.getRdbmsSpecifics(connection),
                                System.currentTimeMillis() - tstart, this.log));
            }
            //if logging is not enable, return the real connection,
            //so that there is no useless costs
            return connection;
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }

    }

    @Override
    public int getLoginTimeout() throws SQLException {
        String methodCall = "getLoginTimeout()";
        try {
            return (Integer) reportReturn(methodCall, realDataSource.getLoginTimeout());
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public PrintWriter getLogWriter() throws SQLException {
        String methodCall = "getLogWriter()";
        try {
            return (PrintWriter) reportReturn(methodCall, realDataSource.getLogWriter());
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public boolean isWrapperFor(Class<?> iface) throws SQLException {
        String methodCall = "isWrapperFor(" + iface + ")";
        try {
            return (Boolean) reportReturn(methodCall, realDataSource.isWrapperFor(iface));
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public void setLoginTimeout(int seconds) throws SQLException {
        String methodCall = "setLoginTimeout(" + seconds + ")";
        try {
            realDataSource.setLoginTimeout(seconds);
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public void setLogWriter(PrintWriter out) throws SQLException {
        String methodCall = "setLogWriter(" + out + ")";
        try {
            realDataSource.setLogWriter(out);
        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T unwrap(Class<T> iface) throws SQLException {
        String methodCall = "unwrap(" + iface + ")";
        try {
            return (T) reportReturn(methodCall, realDataSource.unwrap(iface));

        } catch (SQLException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public Logger getParentLogger() throws SQLFeatureNotSupportedException {
        String methodCall = "getParentLogger()";
        try {
            return (Logger) reportReturn(methodCall, realDataSource.getParentLogger());
        } catch (SQLFeatureNotSupportedException s) {
            reportException(methodCall, s);
            throw s;
        }
    }

    @Override
    public String getClassType() {
        return "DataSource";
    }

    @Override
    public Integer getConnectionNumber() {
        // No connection number in this case
        return null;
    }

}
