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
