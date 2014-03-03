package org.mapfish.print.output;

import java.util.HashMap;
import java.util.Map;

import org.mapfish.print.attribute.Attribute;

public class Values {
    private final Map<String, Object> values;
    
    protected Values() {
        values = new HashMap<String, Object>();
    }
    
    public Values(Map<String, Object> values) {
        this.values = values;
    }
    
    protected void put(String key, Object value) {
        values.put(key, value);
    }
    
    protected Map<String, Object>  getParamters() {
        return values;
    }
    
    public String getString(String key) {
        return (String)values.get(key);
    }
    
    public Double getDouble(String key) {
        return (Double)values.get(key);
    }
    
    public Integer getInteger(String key) {
        return (Integer)values.get(key);
    }
    
    public Object getObject(String key) {
        return values.get(key);
    }

    protected Iterable<Values> getIterator(String key) {
        return (Iterable<Values>)values.get(key);
    }
}
