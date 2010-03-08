/*
 * Copyright (C) 2009  Camptocamp
 *
 * This file is part of MapFish Server
 *
 * MapFish Server is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Server is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with MapFish Server.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.ho.yaml;

import org.apache.log4j.Logger;
import org.ho.yaml.exception.PropertyAccessException;
import org.ho.yaml.wrapper.DefaultBeanWrapper;

import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

/**
 * Fix a few bugs in the original class.
 * See https://sourceforge.net/tracker/index.php?func=detail&aid=1954096&group_id=153924&atid=789717
 */
public class CustomBeanWrapper extends DefaultBeanWrapper {
    public static final Logger LOGGER = Logger.getLogger(CustomBeanWrapper.class);

    public CustomBeanWrapper(Class<?> type) {
        super(type);
    }

    public void setProperty(String name, Object value) throws PropertyAccessException {
        try {
            PropertyDescriptor prop = ReflectionUtil.getPropertyDescriptor(type, name);
            if (prop == null) {
                LOGGER.warn(type.getSimpleName() + ": unknown field '" + name + "' with value '" + value + "'");
                PropertyDescriptor prop2 = ReflectionUtil.getPropertyDescriptor(type, name);
                return;
            }
            if (config.isPropertyAccessibleForDecoding(prop)) {
                Method wm = prop.getWriteMethod();
                wm.setAccessible(true);
                wm.invoke(getObject(), new Object[]{value});
                return;
            }

        } catch (Exception e) {
            LOGGER.warn(type.getSimpleName() + ": Error while writing '" + value + "' to " + name + ": " + e);
        }
        /*try {
            Field field = type.getDeclaredField(name);
            if (config.isFieldAccessibleForDecoding(field)) {
                field.setAccessible(true);
                field.set(getObject(), value);
            }
            return;
        } catch (Exception e) {
        }*/
    }
}
