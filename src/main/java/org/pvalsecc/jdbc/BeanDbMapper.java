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

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;


/**
 * Class for managing the DB mapping of one single bean.
 */
public class BeanDbMapper<CLASS> {
    private static Log log = LogFactory.getLog(BeanDbMapper.class);

    private List<ColumnMapping<CLASS>> cols = new ArrayList<ColumnMapping<CLASS>>();
    private Constructor<CLASS> constructor;
    private Class<CLASS> aClass;
    private Entity table;

    public BeanDbMapper(Class<CLASS> aClass) {
        this.aClass = aClass;
        table = aClass.getAnnotation(Entity.class);
        if (table == null) {
            throw new RuntimeException("Cannot find @Table for class " + aClass.getSimpleName());
        }

        try {
            constructor = aClass.getConstructor();

            //look for the mapped fields
            for (int i = 0; i < aClass.getDeclaredFields().length; i++) {
                Field field = aClass.getDeclaredFields()[i];
                Column column = field.getAnnotation(Column.class);
                if (column != null) {
                    //found one
                    cols.add(new ColumnMapping<CLASS>(field, column, aClass));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Problem with the structure of class " + aClass.getSimpleName(), e);
        }
    }

    /**
     * @return the DB table name.
     */
    public String getTableName() {
        return table.name();
    }

    /**
     * @return The list of field names, separated by ','.
     */
    public String getFieldNames() {
        return getFieldNames(null);
    }

    /**
     * @param prefix an optional prefix to add to each field name.
     * @return The list of field names, separated by ','.
     */
    public String getFieldNames(String prefix) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            if (result.length() > 0) result.append(',');
            if (prefix != null) result.append(prefix);
            result.append(columnMapping.getDBName());
        }
        return result.toString();
    }

    public String getPrimaryKeyFieldName() {
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            if (columnMapping.primaryKey) return columnMapping.getDBName();
        }
        throw new RuntimeException("Did not find any primary key for class " + aClass.getSimpleName());
    }

    public PropertyDescriptor getField(String dbName) {
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            if (columnMapping.getDBName().equals(dbName)) {
                return columnMapping.property;
            }
        }
        throw new RuntimeException("Did not find any field named " + dbName);
    }

    /**
     * Create a bean and fill it with what can be found in the DB.
     *
     * @param rs  The result set.
     * @param pos The position of the first field.
     * @return The newly created bean.
     * @throws SQLException
     */
    public CLASS createFromDb(ResultSet rs, int pos) throws SQLException {
        final CLASS bean;
        try {
            bean = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Cannot invoque constructor for class " + aClass.getSimpleName(), e);
        }
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            columnMapping.getFromDb(bean, rs, pos++);
        }

        return bean;
    }

    public List<CLASS> selectFromDb(Connection conn) throws SQLException {
        StringBuffer query = new StringBuffer("select ");
        query.append(getFieldNames(null));
        query.append(" from ").append(getTableName());

        //log query.
        if (log.isDebugEnabled())
            log.debug("query for farmers is: " + query.toString());

        Statement stmt = conn.createStatement();
        try {
            ResultSet rs = stmt.executeQuery(query.toString());

            // only one result expected: id must be a unique constraint
            List<CLASS> result = new ArrayList<CLASS>();
            while (rs.next()) {
                result.add(createFromDb(rs, 1));
            }
            return result;
        } finally {
            stmt.close();
        }
    }

    public int saveToDb(PreparedStatement stmt, CLASS bean, int pos) {
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            columnMapping.setToDb(bean, stmt, pos++);
        }
        return pos;
    }

    public String getInsertPlaceHolders() {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < cols.size(); i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append('?');
        }
        return result.toString();
    }

    /*
    public void toJson(JSONWriter json, CLASS bean) throws JSONException {
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            json.key(columnMapping.dbName);
            json.value(columnMapping.getValue(bean));
        }
    }

    public void toJson(JSONWriter json, CLASS bean, Set<String> filtreColonnes) throws JSONException {
        for (int i = 0; i < cols.size(); i++) {
            ColumnMapping<CLASS> columnMapping = cols.get(i);
            if (filtreColonnes.contains(columnMapping.getDBName())) {
                json.key(columnMapping.dbName);
                json.value(columnMapping.getValue(bean));
            }
        }
    }*/

    private static class ColumnMapping<CLASS> {
        private boolean primaryKey = false;
        private PropertyDescriptor property;
        private String dbName;

        public ColumnMapping(Field field, Column column, Class<CLASS> aClass) throws IntrospectionException {
            if (field.getAnnotation(Id.class) != null) {
                this.primaryKey = true;
            }
            dbName = extractDbName(field, column);
            property = new PropertyDescriptor(field.getName(), aClass);
        }

        private static String extractDbName(Field field, Column column) {
            if (column.name().length() > 0) {
                return column.name();
            } else {
                StringBuffer parsedPrefix = new StringBuffer();
                String propertyName = field.getName();
                for (int i = 0; i < propertyName.length(); i++) {
                    char c = propertyName.charAt(i);
                    if (Character.isUpperCase(c)) {
                        parsedPrefix.append("_");
                        parsedPrefix.append(c);
                    } else {
                        //parsedPrefix.append(Character.toUpperCase(c));
                        parsedPrefix.append(c);
                    }
                }
                return parsedPrefix.toString().toLowerCase();
            }
        }

        public String getDBName() {
            return dbName;
        }

        public void getFromDb(CLASS bean, ResultSet rs, int pos) throws SQLException {
            Object value = rs.getObject(pos);
            if (rs.wasNull()) value = null;
            try {
                property.getWriteMethod().invoke(bean, value);
            } catch (Exception e) {
                throw new RuntimeException("Cannot invoke method " + property.getWriteMethod() + " in class " + bean.getClass().getSimpleName() + " with value=" + value + "(" +
                        (value != null ? value.getClass().getSimpleName() : "") + ")", e);
            }
        }

        public Object getValue(CLASS bean) {
            try {
                return property.getReadMethod().invoke(bean);
            } catch (Exception e) {
                throw new RuntimeException("Cannot invoke method " + property.getReadMethod() + " in class " + bean.getClass().getSimpleName(), e);
            }
        }

        public void setToDb(CLASS bean, PreparedStatement stmt, int pos) {
            try {
                stmt.setObject(pos, property.getReadMethod().invoke(bean));
            } catch (Exception e) {
                throw new RuntimeException("Cannot invoke method " + property.getReadMethod() + " in class " + bean.getClass().getSimpleName(), e);
            }
        }
    }
}
