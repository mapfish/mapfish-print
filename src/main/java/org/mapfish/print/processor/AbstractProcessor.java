package org.mapfish.print.processor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mapfish.print.output.Values;

public abstract class AbstractProcessor implements Processor {
    private Map<String, String> outputMapper;

//    private static final Pattern FULL_VARIABLE_REGEXP = Pattern.compile("^\\$\\{([^}]+)\\}$");
    private static final Pattern VARIABLE_REGEXP = Pattern.compile("\\$\\{([^}]+)\\}");
/*
    Integer getInteger(String config, Values values) {
        Matcher matcher = FULL_VARIABLE_REGEXP.matcher(config);
        if (matcher.find()) {
            return values.getInteger(matcher.group(1));
        }
        else {
            return Integer.parseInt(config);
        }
    }

    Double getDouble(String config, Values values) {
        Matcher matcher = FULL_VARIABLE_REGEXP.matcher(config);
        if (matcher.find()) {
            return values.getDouble(matcher.group(1));
        }
        else {
            return Double.parseDouble(config);
        }
    }

    Object getObject(String config, Values values) {
        Matcher matcher = FULL_VARIABLE_REGEXP.matcher(config);
        if (matcher.find()) {
            return values.getDouble(matcher.group(1));
        }
        else {
            return config;
        }
    }
*/
    String getString(String config, Values values) {
        String actualValue = config;
        StringBuffer result = new StringBuffer();
        while (true) {
            Matcher matcher = VARIABLE_REGEXP.matcher(actualValue);
            if (matcher.find()) {
                result.append(actualValue.substring(0, matcher.start()));
                result.append(values.getString(matcher.group(1)));
                actualValue = actualValue.substring(matcher.end());
            } else {
                break;
            }
        }
        return result.toString();
    }

    public Map<String, String> getOutputMapper() {
        return outputMapper;
    }

    public void setOutputs(Map<String, String> outputMapper) {
        this.outputMapper = outputMapper;
    }
}
