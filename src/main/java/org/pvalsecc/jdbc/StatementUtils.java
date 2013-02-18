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

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * Misc utility methods around {@link java.sql.PreparedStatement}.
 */
public class StatementUtils {
    public static void copyDouble(ResultSet rs, int sourcePos, PreparedStatement stmt, int destPos) throws SQLException {
        final double value = rs.getDouble(sourcePos);
        if (rs.wasNull())
            stmt.setNull(destPos, Types.DOUBLE);
        else
            stmt.setDouble(destPos, value);
    }

    public static void copyFloat(ResultSet rs, int sourcePos, PreparedStatement stmt, int destPos) throws SQLException {
        final float value = rs.getFloat(sourcePos);
        if (rs.wasNull())
            stmt.setNull(destPos, Types.FLOAT);
        else
            stmt.setFloat(destPos, value);
    }

    public static void copyInt(ResultSet rs, int sourcePos, PreparedStatement stmt, int destPos) throws SQLException {
        final int value = rs.getInt(sourcePos);
        if (rs.wasNull())
            stmt.setNull(destPos, Types.INTEGER);
        else
            stmt.setInt(destPos, value);
    }

    public static void copyLong(ResultSet rs, int sourcePos, PreparedStatement stmt, int destPos) throws SQLException {
        final long value = rs.getLong(sourcePos);
        if (rs.wasNull())
            stmt.setNull(destPos, Types.BIGINT);
        else
            stmt.setLong(destPos, value);
    }

    public static void copyString(ResultSet rs, int sourcePos, PreparedStatement stmt, int destPos) throws SQLException {
        final String value = rs.getString(sourcePos);
        if (rs.wasNull())
            stmt.setNull(destPos, Types.VARCHAR);
        else
            stmt.setString(destPos, value);
    }
}
