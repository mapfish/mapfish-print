package org.mapfish.print.attribute;

import org.mapfish.print.json.PJsonObject;

public class TableListAttribute extends AbstractAttribute {

    @Override
    public Object getValue(PJsonObject values, String name) {
        return values.getJSONObject(name);
    }

    @Override
    protected String getType() {
        return "tablelist";
    }

}
