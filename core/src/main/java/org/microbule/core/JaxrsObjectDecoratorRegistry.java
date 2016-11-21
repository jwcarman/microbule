package org.microbule.core;

import java.util.Map;

import com.google.common.collect.MapMaker;
import org.microbule.spi.JaxrsObjectConfig;
import org.microbule.spi.JaxrsObjectDecorator;

public abstract class JaxrsObjectDecoratorRegistry<C extends JaxrsObjectConfig,T extends JaxrsObjectDecorator<C>> {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final String ENABLED_PROP_PATTERN = "microbule.%s.enabled";
    private final Map<String, T> decoratorsMap = new MapMaker().makeMap();

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    public boolean addDecorator(String name, T decorator) {
        return decoratorsMap.putIfAbsent(name, decorator) == null;
    }

    public void decorate(C object) {
        decoratorsMap.forEach((name, decorator) -> {
            final String enabledProperty = String.format(ENABLED_PROP_PATTERN, name);
            final Boolean enabled = object.getProperty(enabledProperty, Boolean::parseBoolean, Boolean.TRUE);
            if (enabled) {
                decorator.decorate(object);
            }
        });
    }

    public T getDecorator(String name) {
        return decoratorsMap.get(name);
    }

    public int getDecoratorCount() {
        return decoratorsMap.size();
    }

    public void removeDecorator(String name) {
        decoratorsMap.remove(name);
    }
}