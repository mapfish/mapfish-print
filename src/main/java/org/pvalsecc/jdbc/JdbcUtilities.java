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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pvalsecc.misc.UnitUtilities;

import java.sql.BatchUpdateException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Iterator;


public abstract class JdbcUtilities {
    /**
     * Its technical logger.
     */
    public static final Log LOGGER = LogFactory.getLog(JdbcUtilities.class);

    /**
     * Logger for timing statistics.
     */
    public static final Log TIMING_LOGGER = LogFactory.getLog(JdbcUtilities.class.getName() + ".timing");

    public static void safeClose(ResultSet rs) {
        try {
            if (rs != null) {
                rs.close();
            }
        }
        catch (SQLException ignored) {
            LOGGER.warn("Could not close properly a JDBC result set!");
        }
    }

    public static void safeClose(Statement stmt) {
        try {
            if (stmt != null) {
                stmt.close();
            }
        }
        catch (SQLException ignored) {
            LOGGER.warn("Could not close properly a JDBC statement!");
        }
    }

    public static void safeClose(Connection connection) {
        try {
            if (connection != null) {
                connection.close();
            }
        }
        catch (SQLException ignored) {
            LOGGER.warn("Could not close properly a JDBC connection!");
        }
    }

    /**
     * Allows to do something within an existing connection. Takes care of closing everything
     * even in case of errors.
     */
    public static void runSelectQuery(String description, String sqlStatement, Connection conn, SelectTask task) throws SQLException {
        PreparedStatement stmt = null;
        ResultSet rs = null;
        boolean queryDisplayed = false;

        try {
            //noinspection JDBCResourceOpenedButNotSafelyClosed
            stmt = conn.prepareStatement(sqlStatement, ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY);

            stmt.setFetchSize(500);  //for using cursors in the select

            task.setupStatement(stmt);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing SQL : " + sqlStatement + " (" + description + ")");
                queryDisplayed = true;
            }

            long beginTime = System.currentTimeMillis();

            //noinspection JDBCResourceOpenedButNotSafelyClosed
            rs = stmt.executeQuery();

            long middleTime = System.currentTimeMillis();
            if (TIMING_LOGGER.isDebugEnabled()) {
                TIMING_LOGGER.debug("Time " + description + " (executeQuery): " + UnitUtilities.toElapsedTime(middleTime - beginTime));
            }

            task.run(rs);

            if (TIMING_LOGGER.isDebugEnabled()) {
                TIMING_LOGGER.debug("Time " + description + " (read): " + UnitUtilities.toElapsedTime(System.currentTimeMillis() - middleTime));
            }

        } catch (SQLException ex) {
            if (!queryDisplayed) {
                //add the query if it was not displayed previously (will help to debug)
                LOGGER.error(sqlStatement);
            }
            throw ex;
        } finally {
            safeClose(rs);
            safeClose(stmt);
        }
    }

    public static long countTable(Connection con, String tableName, final DeleteTask task) throws SQLException {
        final long[] result = new long[1];
        runSelectQuery("counting table " + tableName, "SELECT count(*) FROM " + tableName, con, new SelectTask() {
            public void setupStatement(PreparedStatement stmt) throws SQLException {
                if (task != null) {
                    task.setupStatement(stmt);
                }
            }

            public void run(ResultSet rs) throws SQLException {
                if (!rs.next()) {
                    throw new SQLException("didn't return any result");
                }
                result[0] = rs.getLong(1);
                if (rs.next()) {
                    throw new SQLException("returned more than one result");
                }
            }
        });
        return result[0];
    }

    public interface SelectTask {
        /**
         * Setup the values for all the '?' in the SQL query.
         */
        void setupStatement(PreparedStatement stmt) throws SQLException;

        /**
         * Iterate on the result set.
         */
        void run(ResultSet rs) throws SQLException;
    }

    /**
     * Execute a delete statement
     *
     * @return The number of rows deleted
     * @throws SQLException
     */
    public static int runDeleteQuery(String description, String sqlStatement, Connection conn, DeleteTask task) throws SQLException {
        PreparedStatement stmt = null;
        boolean queryDisplayed = false;

        try {
            //noinspection JDBCResourceOpenedButNotSafelyClosed
            stmt = conn.prepareStatement(sqlStatement);

            if (task != null) {
                task.setupStatement(stmt);
            }

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing SQL : " + sqlStatement + " (" + description + ")");
                queryDisplayed = true;
            }

            long beginTime = System.currentTimeMillis();

            //noinspection JDBCResourceOpenedButNotSafelyClosed
            boolean type = stmt.execute();
            if (type) {
                throw new RuntimeException("Delete statement returning a result set?");
            }

            final int count = stmt.getUpdateCount();
            if (TIMING_LOGGER.isDebugEnabled()) {
                if (count > 0) {
                    TIMING_LOGGER.debug("Time " + description + " (count=" + count + "): " + UnitUtilities.toElapsedTime(System.currentTimeMillis() - beginTime));
                } else {
                    TIMING_LOGGER.debug("Time " + description + ": " + UnitUtilities.toElapsedTime(System.currentTimeMillis() - beginTime));
                }
            }

            return count;
        } catch (SQLException ex) {
            if (!queryDisplayed) {
                //add the query if it was not displayed previously (will help to debug)
                LOGGER.error(sqlStatement);
            }
            throw ex;
        } finally {
            safeClose(stmt);
        }
    }

    public interface DeleteTask {
        /**
         * Setup the values for all the '?' in the SQL query.
         */
        void setupStatement(PreparedStatement stmt) throws SQLException;
    }

    public static interface InsertTask<T> {
        boolean marshall(PreparedStatement stmt, T item) throws SQLException;
    }

    public static <T> void runInsertQuery(String description, String sqlStatement, Connection conn,
                                          Iterator<T> items, int batchSize, InsertTask<T> task) throws SQLException {
        PreparedStatement stmt = null;

        try {
            long beginTime = System.currentTimeMillis();

            // It's property closed in the finally!
            //noinspection JDBCResourceOpenedButNotSafelyClosed
            stmt = conn.prepareStatement(sqlStatement);

            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing SQL : " + sqlStatement + " (" + description + ")");
            }

            int idx = 0;
            int cpt = 0;
            while (items.hasNext()) {
                T item = items.next();
                if (task.marshall(stmt, item)) {
                    stmt.addBatch();

                    if (++idx >= batchSize) {
                        stmt.executeBatch();
                        idx = 0;
                    }
                    ++cpt;
                }
            }

            if (idx > 0) {
                stmt.executeBatch(); // do not forget the last one as usual...
            }
            if (TIMING_LOGGER.isDebugEnabled()) {
                TIMING_LOGGER.debug("Time " + description + " (" + cpt + " records): " + UnitUtilities.toElapsedTime(System.currentTimeMillis() - beginTime));
            }
        } catch (BatchUpdateException ex) {
            LOGGER.error(ex.getNextException());
            throw ex;
        }
        finally {
            safeClose(stmt);
        }
    }

    public static <T> void runInsertQuery(String description, String sqlStatement, Connection conn,
                                          Iterable<T> items, int batchSize, InsertTask<T> task) throws SQLException {
        runInsertQuery(description, sqlStatement, conn, items.iterator(), batchSize, task);
    }
}
