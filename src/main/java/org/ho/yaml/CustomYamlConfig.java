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

import org.ho.util.BiDirectionalMap;
import org.ho.yaml.wrapper.ArrayWrapper;
import org.ho.yaml.wrapper.DefaultCollectionWrapper;
import org.ho.yaml.wrapper.DefaultMapWrapper;
import org.ho.yaml.wrapper.DefaultSimpleTypeWrapper;
import org.ho.yaml.wrapper.EnumWrapper;
import org.ho.yaml.wrapper.ObjectWrapper;
import org.mapfish.print.config.AddressHostMatcher;
import org.mapfish.print.config.ColorWrapper;
import org.mapfish.print.config.CustomEnumWrapper;
import org.mapfish.print.config.DnsHostMatcher;
import org.mapfish.print.config.LocalHostMatcher;
import org.mapfish.print.config.layout.*;
import org.mapfish.print.scalebar.Direction;
import org.mapfish.print.scalebar.Type;
import org.mapfish.print.utils.DistanceUnit;

import java.awt.*;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class CustomYamlConfig extends YamlConfig {
    public CustomYamlConfig() {
        Map<String, Object> handlers = new HashMap<String, Object>();
        handlers.put(Layouts.class.getName(), Layouts.Wrapper.class.getName());
        handlers.put(ColumnDefs.class.getName(), ColumnDefs.Wrapper.class.getName());
        handlers.put(Exceptions.class.getName(), Exceptions.Wrapper.class.getName());
        handlers.put(Color.class.getName(), ColorWrapper.class.getName());

        //special enum parser
        handlers.put(HorizontalAlign.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(VerticalAlign.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(Direction.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(Type.class.getName(), CustomEnumWrapper.class.getName());
        handlers.put(DistanceUnit.class.getName(), DistanceUnit.Wrapper.class.getName());

        setHandlers(handlers);

        BiDirectionalMap<String, String> transfers = new BiDirectionalMap<String, String>();

        //blocks
        transfers.put("text", TextBlock.class.getName());
        transfers.put("image", ImageBlock.class.getName());
        transfers.put("columns", ColumnsBlock.class.getName());
        transfers.put("table", ColumnsBlock.class.getName());
        transfers.put("map", MapBlock.class.getName());
        transfers.put("attributes", AttributesBlock.class.getName());
        transfers.put("scalebar", ScalebarBlock.class.getName());
        transfers.put("legends", LegendsBlock.class.getName());

        //hosts matchers
        transfers.put("localMatch", LocalHostMatcher.class.getName());
        transfers.put("ipMatch", AddressHostMatcher.class.getName());
        transfers.put("dnsMatch", DnsHostMatcher.class.getName());

        setTransfers(transfers);
    }

    public ObjectWrapper getWrapper(String classname) {
        ObjectWrapper ret;
        Class<?> type = ReflectionUtil.classForName(transfer2classname(classname));
        if (type == null) {
            return null;
        }
        if (handlers != null && handlers.containsKey(classname)) {
            ret = initWrapper(classname, type);
        } else {
            if (Map.class.isAssignableFrom(type)) {
                ret = new DefaultMapWrapper(type);
            } else if (Collection.class.isAssignableFrom(type)) {
                ret = new DefaultCollectionWrapper(type);
            } else if (type.isArray()) {
                ret = new ArrayWrapper(type);
            } else if (ReflectionUtil.isSimpleType(type)) {
                return new DefaultSimpleTypeWrapper(type);
            } else if (type.isEnum()) {
                ret = new EnumWrapper(type);
            } else {
                ret = new CustomBeanWrapper(type);
            }
        }
        ret.setYamlConfig(this);
        return ret;
    }
}
