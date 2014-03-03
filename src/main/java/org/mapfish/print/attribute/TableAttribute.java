package org.mapfish.print.attribute;

import org.mapfish.print.json.PJsonObject;

public class TableAttribute extends AbstractAttribute {

    @Override
    public Object getValue(PJsonObject values, String name) {
        return values.getJSONObject(name);
    }

    @Override
    protected String getType() {
        return "table";
    }
}
