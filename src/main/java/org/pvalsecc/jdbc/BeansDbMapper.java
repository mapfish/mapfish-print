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

import java.util.HashMap;
import java.util.Map;

/**
 * Class managing the DB mappings of the Beans
 */
public class BeansDbMapper {
    private static Map<Class, BeanDbMapper> beans = new HashMap<Class, BeanDbMapper>();
    private static Map<String, BeanDbMapper> tables = new HashMap<String, BeanDbMapper>();

    /**
     * Get or create the mapper for the given class.
     *
     * @return the mapper.
     */
    public static synchronized <CLASS> BeanDbMapper<CLASS> getMapper(Class<CLASS> aClass) {
        BeanDbMapper<CLASS> result = (BeanDbMapper<CLASS>) beans.get(aClass);
        if (result == null) {
            result = new BeanDbMapper<CLASS>(aClass);
            beans.put(aClass, result);
            tables.put(result.getTableName(), result);
        }
        return result;
    }

    public static BeanDbMapper getFromTableName(String table) {
        return tables.get(table);
    }
}
