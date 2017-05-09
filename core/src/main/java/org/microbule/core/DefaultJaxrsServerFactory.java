package org.microbule.core;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.cxf.BusFactory;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.microbule.api.JaxrsServer;
import org.microbule.api.JaxrsServerFactory;
import org.microbule.config.api.Config;
import org.microbule.config.api.ConfigurationException;
import org.microbule.container.api.MicrobuleContainer;
import org.microbule.spi.JaxrsServerDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
@Named("jaxrsServerFactory")
public class DefaultJaxrsServerFactory extends JaxrsServiceDecoratorRegistry<JaxrsServerDecorator> implements JaxrsServerFactory {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultJaxrsServerFactory.class);

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    @Inject
    public DefaultJaxrsServerFactory(MicrobuleContainer container) {
        super(JaxrsServerDecorator.class, container);
    }

//----------------------------------------------------------------------------------------------------------------------
// JaxrsServerFactory Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public JaxrsServer createJaxrsServer(Class<?> serviceInterface, Object serviceImplementation, Config config) {
        final String address = config.value(ADDRESS_PROP).orElseThrow(() -> new ConfigurationException("Missing '%s' property.", ADDRESS_PROP));
        LOGGER.info("Starting {} JAX-RS server ({})...",serviceInterface.getSimpleName(), address);
        final JaxrsServiceDescriptorImpl descriptor = new JaxrsServiceDescriptorImpl(serviceInterface);
        decorate(descriptor, config);
        final JAXRSServerFactoryBean sf = new JAXRSServerFactoryBean();
        sf.setBus(BusFactory.getDefaultBus(true));
        sf.setServiceBean(serviceImplementation);
        sf.setAddress(address);
        sf.setFeatures(descriptor.getFeatures());
        sf.setProviders(descriptor.getProviders());
        final Server server = sf.create();
        LOGGER.info("Successfully started {} JAX-RS server ({}).",serviceInterface.getSimpleName(), address);
        return server::destroy;
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected Logger getLogger() {
        return LOGGER;
    }
}
