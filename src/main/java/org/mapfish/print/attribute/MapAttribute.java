package org.mapfish.print.attribute;

import org.json.JSONException;
import org.json.JSONWriter;
import org.mapfish.print.json.PJsonObject;

public class MapAttribute extends AbstractAttribute {

    float maxDpi;
    float width;
    float height;

    @Override
    public Object getValue(PJsonObject values, String name) {
        return values.getJSONObject(name);
    }

    @Override
    protected String getType() {
        return "map";
    }

    protected void additionaPrintClientConfig(JSONWriter json) throws JSONException {
        json.key("maxDpi").value(maxDpi);
        json.key("width").value(width);
        json.key("height").value(height);
    }

    public float getMaxDpi() {
        return maxDpi;
    }

    public void setMaxDpi(float maxDpi) {
        this.maxDpi = maxDpi;
    }

    public float getWidth() {
        return width;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public float getHeight() {
        return height;
    }

    public void setHeight(float height) {
        this.height = height;
    }
}
