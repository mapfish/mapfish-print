/*
 * Copyright (C) 2008 Patrick Valsecchi
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  U
 */
package org.pvalsecc.jdbc;

import org.pvalsecc.misc.StringUtils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public abstract class ConnectionFactory {
    private static final Pattern GET_SUBPROTOCOL = Pattern.compile("^jdbc:([^:]+):.*$", Pattern.CASE_INSENSITIVE);

    private static final Map<String, List<String>> DRIVERS = new HashMap<String, List<String>>();

    /**
     * Wrapper around {@link java.sql.DriverManager#getConnection(String)} that deals
     * with the driver loading.
     */
    public static Connection getConnection(String url) throws SQLException {
        loadDriver(url);
        return DriverManager.getConnection(url);
    }

    /**
     * Wrapper around {@link java.sql.DriverManager#getConnection(String, java.util.Properties)} that deals
     * with the driver loading.
     */
    public static Connection getConnection(String url, Properties info) throws SQLException {
        loadDriver(url);
        return DriverManager.getConnection(url, info);
    }

    /**
     * Wrapper around {@link java.sql.DriverManager#getConnection(String, String, String)} that deals
     * with the driver loading.
     */
    public static Connection getConnection(String url, String user, String password) throws SQLException {
        loadDriver(url);
        return DriverManager.getConnection(url, user, password);
    }

    /**
     * Try to load the JDBC driver that matches the given url (as specified in {@link java.sql.DriverManager#getConnection(String)})
     *
     * @throws SQLException If the url is invalid or if there is no known driver.
     */
    public static void loadDriver(String url) throws SQLException {
        Matcher matcher = GET_SUBPROTOCOL.matcher(url);
        if (matcher.matches()) {
            List<String> drivers = DRIVERS.get(matcher.group(1).toLowerCase());
            if (drivers != null) {
                for (int i = 0; i < drivers.size(); i++) {
                    String subDriver = drivers.get(i);
                    try {
                        Class.forName(subDriver);
                        return;
                    } catch (ClassNotFoundException e) {
                        //ignored
                    }
                }
                throw new SQLException("Driver(s) [" + StringUtils.join(drivers, ",") + "] not found in classpath");
            } else {
                //let's hope it's already been installed...
                try {
                    if (DriverManager.getDriver(url) == null) {
                        throw new SQLException("No JDBC driver know for this connection url: " + url);
                    }
                } catch (SQLException ex) {
                    throw new SQLException("No JDBC driver know for this connection url: " + url);
                }
            }
        } else {
            throw new SQLException("Cannot parse connection string: " + url);
        }
    }

    /**
     * If you want to add a new driver...
     *
     * @param type   The string seen after the "jdbc:" in the url
     * @param driver The class to use as driver
     */
    public static void add(String type, String driver) {
        List<String> cur = DRIVERS.get(type);
        if (cur == null) {
            cur = new ArrayList<String>();
            DRIVERS.put(type, cur);
        }
        cur.add(driver);
    }

    static {
        add("postgresql", "org.postgresql.Driver");
        add("postgres", "postgresql.Driver");
        add("postgresql_postgis", "org.postgis.DriverWrapper");
        add("db2", "COM.ibm.db2.jdbc.app.DB2Driver");
        add("odbc", "sun.jdbc.odbc.JdbcOdbcDriver");
        add("weblogic", "weblogic.jdbc.mssqlserver4.Driver");
        add("oracle", "oracle.jdbc.driver.OracleDriver");
        add("pointbase", "com.pointbase.jdbc.jdbcUniversalDriver");
        add("cloudscape", "COM.cloudscape.core.JDBCDriver");
        add("rmi", "RmiJdbc.RJDriver");
        add("firebirdsql", "org.firebirdsql.jdbc.FBDriver");
        add("ids", "ids.sql.IDSDriver");
        add("informix-sqli", "com.informix.jdbc.IfxDriver");
        add("idb", "org.enhydra.instantdb.jdbc.idbDriver");
        add("interbase", "interbase.interclient.Driver");
        add("HypersonicSQL", "org.hsql.jdbcDriver");
        add("HypersonicSQL", "hSql.hDriver");
        add("JTurbo", "com.ashna.jturbo.driver.Driver");
        add("inetdae", "com.inet.tds.TdsDriver");
        add("microsoft", "com.microsoft.jdbc.sqlserver.SQLServerDriver");
        add("mysql", "org.gjt.mm.mysql.Driver");
        add("sybase", "com.sybase.jdbc2.jdbc.SybDriver");
        add("sybase", "com.sybase.jdbc.SybDriver");
    }
}
