package org.mapfish.print.attribute;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.data.JsonDataSource;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mapfish.print.config.Configuration;
import org.mapfish.print.config.Template;
import org.mapfish.print.wrapper.PObject;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.annotation.Nonnull;

/**
 * A JSON data source attribute. To be declared as net.sf.jasperreports.engine.data.JsonDataSource in Jasper.
 * Accepts only JSON objects at the root. To use it, you must pass it to a sub report like that:
 * <pre>
 * {@code
 * < subreport>
 *     ...
 *     < dataSourceExpression>
 *         < ![CDATA[$P{json_attribute}.subDataSource("path.to.list")]]>
 *     < /dataSourceExpression>
 *     < subreportExpression><![CDATA["subreport_name.jasper"]]>< /subreportExpression>
 * < /subreport>
 * }
 * </pre>
 *
 * The expression given to the subDataSource method must select an array in the JSON structure.
 */
public class JsonDatasourceAttribute implements Attribute {
    private String configName;

    @Override
    public void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        json.key(ReflectiveAttribute.JSON_NAME).value(this.configName);
        json.key(ReflectiveAttribute.JSON_ATTRIBUTE_TYPE).value("json");
    }

    @Override
    public void setConfigName(final String name) {
        this.configName = name;
    }

    @Override
    public Class getValueType() {
        return JsonDataSource.class;
    }

    @Override
    public Object getValue(@Nonnull final Template template, @Nonnull final String attributeName,
                           @Nonnull final PObject requestJsonAttributes) {
        final String json;
        final Object value = requestJsonAttributes.opt(attributeName);

        if (value == null) {
            json = "null";
        } else if (value instanceof JSONObject) {
            json = value.toString();
        } else {
            final String message = "Expected a JSON Object as the value for the element with the path: '" +
                    requestJsonAttributes.getPath(attributeName) + "' but instead "
                    + "got a '" + value.getClass().toString() + "'";

            throw new IllegalArgumentException(message);
        }

        try {
            final JsonDataSource result = new JsonDataSource(new ByteArrayInputStream(json.getBytes(StandardCharsets.UTF_8)));
            result.next();
            return result;
        } catch (JRException e) {
            throw new RuntimeException("Error while parsing " +
                    requestJsonAttributes.getPath(attributeName) + "as json", e);
        }
    }

    @Override
    public void validate(final List<Throwable> validationErrors, final Configuration configuration) {
        // nothing to validate
    }
}
