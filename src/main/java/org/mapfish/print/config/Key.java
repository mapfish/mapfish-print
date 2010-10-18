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
package org.mapfish.print.config;

import org.mapfish.print.InvalidValueException;

/**
 * Encapsulates keys for a host.  For example Google requires a key.
 * 
 * @author jeichar
 */
public class Key {
    private HostMatcher host;
    private HostMatcher domain;
    private String key;
    private String id;

    public HostMatcher getDomain() {
        return domain == null ? HostMatcher.ACCEPT_ALL : domain;
    }

    public void setDomain(HostMatcher domain) {
        this.domain = domain;
    }

    public HostMatcher getHost() {
        return host == null ? HostMatcher.ACCEPT_ALL : host;
    }
    public void setHost(HostMatcher host) {
        this.host = host;
    }
    public String getKey() {
        return key;
    }
    public void setKey(String key) {
        this.key = key;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Key key1 = (Key) o;

        if (domain != null ? !domain.equals(key1.domain) : key1.domain != null) return false;
        if (host != null ? !host.equals(key1.host) : key1.host != null) return false;
        if (id != null ? !id.equals(key1.id) : key1.id != null) return false;
        if (key != null ? !key.equals(key1.key) : key1.key != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = host != null ? host.hashCode() : 0;
        result = 31 * result + (domain != null ? domain.hashCode() : 0);
        result = 31 * result + (key != null ? key.hashCode() : 0);
        result = 31 * result + (id != null ? id.hashCode() : 0);
        return result;
    }

    public void validate() {
        if(id == null) {
            throw new InvalidValueException("An 'id' attribute is required for each key defined", key);
        }
        if(key == null) {
            throw new InvalidValueException("A 'key' attribute is required for each key defined", key);
        }
    }
}
