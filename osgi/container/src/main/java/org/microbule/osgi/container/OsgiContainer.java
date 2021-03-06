/*
 * Copyright (c) 2017 The Microbule Authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package org.microbule.osgi.container;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.ws.rs.Path;

import org.microbule.container.api.PluginListener;
import org.microbule.container.api.ServerListener;
import org.microbule.container.core.AbstractContainer;
import org.microbule.container.core.DefaultServerDefinition;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceEvent;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class OsgiContainer extends AbstractContainer {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final String MICROBULE_SERVER_FILTER = "(microbule.server=*)";

    private static final Logger LOGGER = LoggerFactory.getLogger(OsgiContainer.class);

    private final BundleContext bundleContext;

    private final List<ServerListener> serverListeners = new CopyOnWriteArrayList<>();
    private final AtomicLong lastUpdated;
    private final ScheduledExecutorService scheduler;
    private final long quietPeriod;

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    public OsgiContainer(BundleContext bundleContext, long quietPeriodInMs) {
        this.bundleContext = bundleContext;
        this.quietPeriod = TimeUnit.MILLISECONDS.toNanos(quietPeriodInMs);
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.lastUpdated = new AtomicLong(System.nanoTime());
        scheduler.schedule(this::checkLastUpdate, quietPeriodInMs, TimeUnit.NANOSECONDS);
    }

//----------------------------------------------------------------------------------------------------------------------
// MicrobuleContainer Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public <B> void addPluginListener(Class<B> beanType, PluginListener<B> listener) {
        new PluginListenerWhiteboard<>(bundleContext, beanType, listener, lastUpdated).start();
    }

    @Override
    public void addServerListener(ServerListener listener) {
        serverListeners.add(listener);
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    private void checkLastUpdate() {
        final long elapsed = System.nanoTime() - lastUpdated.get();
        if (elapsed >= quietPeriod) {
            LOGGER.info("Quiet period ({} ms) expired, searching for servers.", TimeUnit.NANOSECONDS.toMillis(quietPeriod));
            registerServiceListener();
            findExistingServices();
            scheduler.shutdownNow();
        } else {
            final long newDelay = quietPeriod - elapsed;
            LOGGER.debug("Only {} ms have elapsed since last update time, checking again in {} ms...", TimeUnit.NANOSECONDS.toMillis(elapsed), TimeUnit.NANOSECONDS.toMillis(newDelay));
            scheduler.schedule(this::checkLastUpdate, newDelay, TimeUnit.NANOSECONDS);
        }
    }

    private void registerServiceListener() {
        try {
            bundleContext.addServiceListener(event -> {
                if (event.getType() == ServiceEvent.REGISTERED) {
                    registerService(event.getServiceReference());
                } else if (event.getType() == ServiceEvent.UNREGISTERING) {
                    final Long serviceId = serviceId(event.getServiceReference());
                    serverListeners.forEach(listener -> listener.unregisterServer(serviceId.toString()));
                    bundleContext.ungetService(event.getServiceReference());
                }
            }, MICROBULE_SERVER_FILTER);
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Unable to add service listener JAX-RS services using filter \"{}\".", MICROBULE_SERVER_FILTER, e);
        }
    }

    private void registerService(ServiceReference<?> ref) {
        final String[] serviceInterfaceNames = (String[]) ref.getProperty(Constants.OBJECTCLASS);
        Arrays.stream(serviceInterfaceNames).forEach(serviceInterfaceName -> {
            try {
                final Class<?> serviceInterface = ref.getBundle().loadClass(serviceInterfaceName);
                if (serviceInterface.isAnnotationPresent(Path.class)) {
                    final Long serviceId = serviceId(ref);
                    final Object serviceImplementation = bundleContext.getService(ref);
                    final DefaultServerDefinition definition = new DefaultServerDefinition(serviceId.toString(), serviceInterface, serviceImplementation);
                    serverListeners.forEach(listener -> listener.registerServer(definition));
                }
            } catch (ClassNotFoundException e) {
                LOGGER.error("Unable to load service interface from target bundle!", e);
            }
        });
    }

    private Long serviceId(ServiceReference<?> ref) {
        return (Long) ref.getProperty(Constants.SERVICE_ID);
    }

    private void findExistingServices() {
        try {
            final ServiceReference<?>[] serviceReferences = bundleContext.getAllServiceReferences(null, MICROBULE_SERVER_FILTER);
            if (serviceReferences != null) {
                Arrays.stream(serviceReferences).forEach(this::registerService);
            }
        } catch (InvalidSyntaxException e) {
            LOGGER.error("Unable to search for JAX-RS services using filter \"{}\".", MICROBULE_SERVER_FILTER, e);
        }
    }
}
