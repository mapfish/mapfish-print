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

package org.mapfish.print.attribute;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.config.Template;
import org.mapfish.print.parser.ParserUtils;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.mapfish.print.parser.MapfishParser.stringRepresentation;

/**
 * An attribute whose type is an object that will be auto-populated from the json based on the public fields of the value object.
 * <p/>
 * This type of attribute creates a template object from the {@link #createValue(org.mapfish.print.config.Template)} method.  The
 * instance that is created
 * has the appropriate default values and the rest of the values are parsed from the json provided for the print request.
 * <p/>
 * For more details on parsing see: {@link #createValue(org.mapfish.print.config.Template)}
 *
 * @param <Value> The value object of the attribute
 * @author Jesse on 4/9/2014.
 */
public abstract class ReflectiveAttribute<Value> implements Attribute {

    /**
     * Create an instance of a attribute value object.  Each instance must be new and unique. Instances must <em>NOT</em> be shared.
     * <p/>
     * The object will be populated from the json.  Each public field will be populated by looking up the value in the json.
     * <p/>
     * If a field in the object has the {@link org.mapfish.print.parser.HasDefaultValue} annotation then no exception
     * will be thrown if the json does not contain a value.
     * <p/>
     * Fields in the object with the {@link org.mapfish.print.parser.OneOf} annotation must have one of the fields in the
     * request data.
     * <p/>
     * <ul>
     * <li>{@link java.lang.String}</li>
     * <li>{@link java.lang.Integer}</li>
     * <li>{@link java.lang.Float}</li>
     * <li>{@link java.lang.Double}</li>
     * <li>{@link java.lang.Short}</li>
     * <li>{@link java.lang.Boolean}</li>
     * <li>{@link java.lang.Character}</li>
     * <li>{@link java.lang.Byte}</li>
     * <li>{@link java.lang.Enum}</li>
     * <li>PJsonObject</li>
     * <li>URL</li>
     * <li>Any enum</li>
     * <li>PJsonArray</li>
     * <li>any type with a 0 argument constructor</li>
     * <li>array of any of the above (String[], boolean[], PJsonObject[], ...)</li>
     * </ul>
     * <p/>
     * If there is a public <code>{@value org.mapfish.print.json.parser.MapfishParser#POST_CONSTRUCT_METHOD_NAME}()</code>
     * method then it will be called after the fields are all set.
     * <p/>
     * In the case where the a parameter type is a normal POJO (not a special case like PJsonObject, URL, enum, double, etc...)
     * then it will be assumed that the json data is a json object and the parameters will be recursively parsed into the new
     * object as if it is also MapLayer parameter object.
     * <p/>
     * It is important to put values in the value object as public fields because reflection is used when printing client config
     * as well as generating documentation.  If a field is intended for the client software as information but is not intended
     * to be set (or sent as part of the request data), the field can be a final field.
     *
     * @param template the template that this attribute is part of.
     */
    public abstract Value createValue(Template template);

    /**
     * Uses reflection on the object created by {@link #createValue(org.mapfish.print.config.Template)} to create the options.
     * <p/>
     * The public final fields are written as the field name as the key and the value as the value.
     * <p/>
     * The public (non-final) mandatory fields are written as part of clientParams and are written with the field name as the key and
     * the field type as the value.
     * <p/>
     * The public (non-final) {@link org.mapfish.print.parser.HasDefaultValue} fields are written as part of clientOptions and are
     * written with the field name as the key and an object as a value with a type property with the type and a default property
     * containing the default value.
     *
     * @param json     the json writer to write to
     * @param template the template that this attribute is part of
     * @throws JSONException
     */
    @Override
    public final void printClientConfig(final JSONWriter json, final Template template) throws JSONException {
        try {
            final Value exampleValue = createValue(template);
            json.key("name").value(stringRepresentation(exampleValue.getClass()));
            final Collection<Field> finalFields = ParserUtils.getAttributes(exampleValue.getClass(),
                    ParserUtils.FILTER_FINAL_FIELDS);
            if (!finalFields.isEmpty()) {
                json.object();
                for (Field attribute : finalFields) {
                    json.key(attribute.getName()).value(attribute.get(exampleValue));
                }
                json.endObject();
            }

            json.key("clientParams");
            final Collection<Field> mutableFields = ParserUtils.getAttributes(exampleValue.getClass(),
                    ParserUtils.FILTER_ONLY_REQUIRED_ATTRIBUTES);
            if (!mutableFields.isEmpty()) {
                json.object();
                for (Field attribute : mutableFields) {
                    json.key(attribute.getName()).value(stringRepresentation(attribute.getType()));
                }
                json.endObject();
            }

            json.key("clientOptions");
            final Collection<Field> hasDefaultFields = ParserUtils.getAttributes(exampleValue.getClass(),
                    ParserUtils.FILTER_HAS_DEFAULT_ATTRIBUTES);
            if (!hasDefaultFields.isEmpty()) {
                json.object();
                for (Field attribute : hasDefaultFields) {
                    json.key(attribute.getName());
                    json.object();
                    json.key("type").value(stringRepresentation(attribute.getType()));
                    json.key("default").value(attribute.get(exampleValue));
                    json.endObject();
                }
                json.endObject();
            }
        } catch (IllegalAccessException e) {
            throw new Error(e);
        }
    }


}
