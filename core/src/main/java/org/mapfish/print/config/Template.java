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

package org.mapfish.print.config;

import com.google.common.base.Optional;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.geotools.styling.Style;
import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.attribute.Attribute;
import org.mapfish.print.attribute.InternalAttribute;
import org.mapfish.print.attribute.map.MapfishMapContext;
import org.mapfish.print.config.access.AccessAssertion;
import org.mapfish.print.config.access.AlwaysAllowAssertion;
import org.mapfish.print.config.access.RoleAccessAssertion;
import org.mapfish.print.map.style.StyleParser;
import org.mapfish.print.processor.Processor;
import org.mapfish.print.processor.ProcessorDependencyGraph;
import org.mapfish.print.processor.ProcessorDependencyGraphFactory;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nonnull;

/**
 * Represents a report template configuration.
 *
 * @author sbrunner
 */
public class Template implements ConfigurationObject, HasConfiguration {
    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(Template.class);
    @Autowired
    private ProcessorDependencyGraphFactory processorGraphFactory;


    private String reportTemplate;
    private Map<String, Attribute> attributes = Maps.newHashMap();
    private List<Processor> processors = Lists.newArrayList();

    private String jdbcUrl;
    private String jdbcUser;
    private String jdbcPassword;
    private volatile ProcessorDependencyGraph processorGraph;
    private volatile ProcessorDependencyGraph iterProcessorGraph;
    private Map<String, String> styles = new HashMap<String, String>();
    private Configuration configuration;
    private AccessAssertion accessAssertion = AlwaysAllowAssertion.INSTANCE;
    @Autowired
    private StyleParser styleParser;
    @Qualifier("httpClientFactory")
    @Autowired
    private ClientHttpRequestFactory httpRequestFactory;
    private String tableDataKey;


    /**
     * Print out the template information that the client needs for performing a request.
     *
     * @param json the writer to write the information to.
     */
    public final void printClientConfig(final JSONWriter json) throws JSONException {
        json.key("attributes");
        json.array();
        for (String name : this.attributes.keySet()) {
            final Attribute attribute = this.attributes.get(name);
            if (attribute.getClass().getAnnotation(InternalAttribute.class) == null) {
                json.object();
                json.key("name").value(name);
                attribute.printClientConfig(json, this);
                json.endObject();
            }
        }
        json.endArray();
    }


    public final Map<String, Attribute> getAttributes() {
        return this.attributes;
    }

    /**
     * Set the attributes for this template.
     *
     * @param attributes the attribute map
     */
    public final void setAttributes(final Map<String, Attribute> attributes) {
        for (Map.Entry<String, Attribute> entry : attributes.entrySet()) {
            Object attribute = entry.getValue();
            if (!(attribute instanceof Attribute)) {
                final String msg = "Attribute: '" + entry.getKey() + "' is not an attribute. It is a: " + attribute;
                LOGGER.error("Error setting the Attributes: " + msg);
                throw new IllegalArgumentException(msg);
            } else {
                ((Attribute) attribute).setConfigName(entry.getKey());
            }
        }
        this.attributes = attributes;
    }

    public final String getReportTemplate() {
        return this.reportTemplate;
    }

    public final void setReportTemplate(final String reportTemplate) {
        this.reportTemplate = reportTemplate;
    }

    public final List<Processor> getProcessors() {
        return this.processors;
    }

    /**
     * Set the normal processors.
     *
     * @param processors the processors to set.
     */
    public final void setProcessors(final List<Processor> processors) {
        assertProcessors(processors);
        this.processors = processors;
    }

    private void assertProcessors(final List<Processor> processorsToCheck) {
        for (Processor entry : processorsToCheck) {
            if (!(entry instanceof Processor)) {
                final String msg = "Processor: " + entry + " is not a processor.";
                LOGGER.error("Error setting the Attributes: " + msg);
                throw new IllegalArgumentException(msg);
            }
        }

    }

    /**
     * Set the key of the data that is the datasource for the main table in the report.
     *
     * @param tableData the key of the data that is the datasource for the main table in the report.
     */
    public final void setTableData(final String tableData) {
        this.tableDataKey = tableData;
    }

    public final String getTableDataKey() {
        return this.tableDataKey;
    }

    public final String getJdbcUrl() {
        return this.jdbcUrl;
    }

    public final void setJdbcUrl(final String jdbcUrl) {
        this.jdbcUrl = jdbcUrl;
    }

    public final String getJdbcUser() {
        return this.jdbcUser;
    }

    public final void setJdbcUser(final String jdbcUser) {
        this.jdbcUser = jdbcUser;
    }

    public final String getJdbcPassword() {
        return this.jdbcPassword;
    }

    public final void setJdbcPassword(final String jdbcPassword) {
        this.jdbcPassword = jdbcPassword;
    }

    /**
     * Get the processor graph to use for executing all the processors for the template.
     *
     * @return the processor graph.
     */
    public final ProcessorDependencyGraph getProcessorGraph() {
        if (this.processorGraph == null) {
            synchronized (this) {
                if (this.processorGraph == null) {
                    this.processorGraph = this.processorGraphFactory.build(this.processors);
                }
            }
        }
        return this.processorGraph;
    }

    /**
     * Set the named styles defined in the configuration for this.
     *
     * @param styles set the styles specific for this template.
     */
    public final void setStyles(final Map<String, String> styles) {
        this.styles = styles;
    }

    /**
     * Look for a style in the named styles provided in the configuration.
     *
     * @param styleName  the name of the style to look for.
     * @param mapContext information about the map projection, bounds, size, etc...
     */
    @SuppressWarnings("unchecked")
    @Nonnull
    public final Optional<Style> getStyle(final String styleName,
                                          final MapfishMapContext mapContext) {
        final String styleRef = this.styles.get(styleName);
        Optional<Style> style;
        if (styleRef != null) {
            style = (Optional<Style>) this.styleParser.loadStyle(getConfiguration(), this.httpRequestFactory, styleRef, mapContext);
        } else {
            style = Optional.absent();
        }
        return style.or(this.configuration.getStyle(styleName, mapContext));
    }

    @Override
    public final void setConfiguration(final Configuration configuration) {
        this.configuration = configuration;
    }

    public final Configuration getConfiguration() {
        return this.configuration;
    }

    @Override
    public final void validate(final List<Throwable> validationErrors) {
        this.accessAssertion.validate(validationErrors);
        int numberOfTableConfigurations = this.tableDataKey == null ? 0 : 1;
        numberOfTableConfigurations += this.jdbcUrl == null ? 0 : 1;

        if (numberOfTableConfigurations > 1) {
            validationErrors.add(new ConfigurationException("Only one of 'iterValue' or 'tableData' or 'jdbcUrl' should be defined."));
        }

        for (Processor processor : this.processors) {
            processor.validate(validationErrors);
        }

        for (Attribute attribute : this.attributes.values()) {
            attribute.validate(validationErrors);
        }

        try {
            getProcessorGraph();
        } catch (Throwable t) {
            validationErrors.add(t);
        }

        if (getJdbcUrl() != null) {

            Connection connection = null;
            try {
                if (getJdbcUser() != null) {
                    connection = DriverManager.getConnection(getJdbcUrl(), getJdbcUser(), getJdbcPassword());
                } else {
                    connection = DriverManager.getConnection(getJdbcUrl());
                }
            } catch (SQLException e) {
                validationErrors.add(e);
            } finally {
                if (connection != null) {
                    try {
                        connection.close();
                    } catch (SQLException e) {
                        validationErrors.add(e);
                    }
                }
            }
        }
    }

    final void assertAccessible(final String name) {
        this.accessAssertion.assertAccess("Template '" + name + "'", this);
    }


    /**
     * The roles required to access this template.  If empty or not set then it is a <em>public</em> template.  If there are
     * many roles then a user must have one of the roles in order to access the template.
     * <p/>
     * The security (how authentication/authorization is done) is configured in the /WEB-INF/classes/mapfish-spring-security.xml
     * <p>
     * Any user without the required role will get an error when trying to access the template and the template will not
     * be visible in the capabilities requests.
     * </p>
     *
     * @param access the roles needed to access this
     */
    public final void setAccess(final List<String> access) {
        final RoleAccessAssertion assertion = new RoleAccessAssertion();
        assertion.setRequiredRoles(access);
        this.accessAssertion = assertion;
    }
    public final AccessAssertion getAccessAssertion() {
        return this.accessAssertion;
    }
}
