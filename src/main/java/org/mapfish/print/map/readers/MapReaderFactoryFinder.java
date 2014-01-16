package org.mapfish.print.map.readers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mapfish.print.InvalidJsonValueException;
import org.mapfish.print.RenderingContext;
import org.mapfish.print.utils.PJsonObject;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class MapReaderFactoryFinder implements ApplicationContextAware {
    private Map<String, MapReaderFactory> factories = new HashMap<String, MapReaderFactory>();

    @Override
    public void setApplicationContext(ApplicationContext applicationContext)
            throws BeansException {
        Map<String, MapReaderFactory> tmpFac = applicationContext.getBeansOfType(MapReaderFactory.class);

        for (Map.Entry<String, MapReaderFactory> entry : tmpFac.entrySet()) {
            if(!entry.getKey().contains("-")) {
                throw new BeanInitializationException("All MapFactoryReaders must have an id with format:  type-MapReaderFactory");
            }
            factories.put(entry.getKey().split("-")[0].toLowerCase(), entry.getValue());
        }
    }

    public void create(List<MapReader> readers, String type,
            RenderingContext context, PJsonObject params) {

        MapReaderFactory factory = getFactory(params, type);

        readers.addAll(factory.create(type, context, params));
    }

    public MapReaderFactory getFactory(PJsonObject params, String type) {
        final MapReaderFactory factory = factories.get(type.toLowerCase());

        if (factory==null) {
            throw new InvalidJsonValueException(params, "type", type);
        }

        return factory;
    }
}
