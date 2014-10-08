/*
 * Copyright (C) 2014  Camptocamp
 *
 * This file is part of MapFish Print
 *
 * MapFish Print is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MapFish Print is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MapFish Print.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mapfish.print.http;

import org.junit.Test;
import org.mapfish.print.AbstractMapfishSpringTest;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.ConfigurationFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HttpConfigurationTest extends AbstractMapfishSpringTest {

    @Autowired
    ConfigurationFactory configurationFactory;

    @Test
    public void testConfiguration() throws Exception {
        final File configFile = getFile(HttpConfigurationTest.class, "configuration/config.yaml");
        final Configuration config = configurationFactory.getConfig(configFile);

        assertNotNull(config.getCertificateStore());
        assertEquals("file://keystore.jks", config.getCertificateStore().getUri().toString());

        assertNotNull(config.getCertificateStore().getSSLContext());

        assertEquals(2, config.getCredentials().size());
        assertEquals(1, config.getProxies().size());
    }
}
