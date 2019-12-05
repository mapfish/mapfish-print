package org.mapfish.print.map.tiled;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.mapfish.print.parser.HasDefaultValue;
import org.mapfish.print.wrapper.PArray;
import org.mapfish.print.wrapper.PObject;
import org.mapfish.print.wrapper.json.PJsonObject;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

/**
 * An abstract layers params class for WM* layers (e.g. WMS or WMTS).
 */
public abstract class AbstractWMXLayerParams extends AbstractTiledLayerParams {

    private final Multimap<String, String> additionalCustomParam = HashMultimap.create();
    /**
     * Custom query parameters to use when making http requests.  These are related to {@link
     * #mergeableParams} except they are the parameters that will prevent two layers from the same server from
     * being merged into a single request with both layers. See {@link #mergeableParams} for a more detailed
     * example of the difference between {@link #mergeableParams} and {@link #customParams}.
     *
     * The json should look something like:
     * <pre><code>
     * {
     *     "param1Name": "value",
     *     "param2Name": ["value1", "value2"]
     * }
     * </code></pre>
     */
    @HasDefaultValue
    public PObject customParams;
    /**
     * Custom query parameters that can be merged if multiple layers are merged together into a single
     * request.
     *
     * The json should look something like:
     * <pre><code>
     * {
     *     "param1Name": "value",
     *     "param2Name": ["value1", "value2"]
     * }
     * </code></pre>
     *
     * For example in WMS the style parameter can be merged.  If there are several wms layers that can be
     * merged except they have different style parameters they can be merged because the style parameter can
     * be merged.
     *
     * Compare that to DPI parameter (for QGIS wms mapserver).  if two layers have different DPI then the
     * layers cannot be merged.  In this case the DPI should <em>NOT</em> be one of the {@link
     * #mergeableParams} it should be one of the {@link #customParams}.
     */
    @HasDefaultValue
    public PJsonObject mergeableParams;

    /**
     * Constructor.
     */
    public AbstractWMXLayerParams() {
        super();
    }

    /**
     * Copy constructor.
     *
     * @param other the object to copy
     */
    public AbstractWMXLayerParams(final AbstractWMXLayerParams other) {
        super(other);
        this.additionalCustomParam.putAll(other.additionalCustomParam);
        this.customParams = other.customParams;
        this.mergeableParams = other.mergeableParams;
    }

    /**
     * Read the {@link #customParams} into a Multimap.
     */
    public final Multimap<String, String> getCustomParams() {
        Multimap<String, String> result = convertToMultiMap(this.customParams);
        result.putAll(this.additionalCustomParam);
        return result;
    }

    /**
     * Read the {@link #mergeableParams} into a Multimap.
     */
    public final Multimap<String, String> getMergeableParams() {
        return convertToMultiMap(this.mergeableParams);
    }

    private Multimap<String, String> convertToMultiMap(final PObject objectParams) {
        Multimap<String, String> params = HashMultimap.create();
        if (objectParams != null) {
            Iterator<String> customParamsIter = objectParams.keys();
            while (customParamsIter.hasNext()) {
                String key = customParamsIter.next();
                if (objectParams.isArray(key)) {
                    final PArray array = objectParams.optArray(key);
                    for (int i = 0; i < array.size(); i++) {
                        params.put(key, array.getString(i));
                    }
                } else {
                    params.put(key, objectParams.optString(key, ""));
                }
            }
        }

        return params;
    }


    @Override
    public String createCommonUrl()
            throws URISyntaxException {
        return getBaseUrl();
    }

    /**
     * Set a custom parameter.
     *
     * @param name the parameter name
     * @param value the parameter value
     */
    public final void setCustomParam(final String name, final String value) {
        this.additionalCustomParam.put(name, value);
    }

    @Override
    public boolean validateBaseUrl() {
        try {
            new URI(getBaseUrl());
            return true;
        } catch (URISyntaxException exc) {
            return false;
        }
    }

}
