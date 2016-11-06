package org.microbule.osgi;

import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.microbule.api.JaxrsServer;
import org.microbule.api.JaxrsServerFactory;
import org.microbule.test.MockObjectTestCase;
import org.microbule.test.hello.HelloService;
import org.microbule.test.hello.HelloServiceImpl;
import org.microbule.test.osgi.OsgiRule;
import org.microbule.test.osgi.ServicePropsBuilder;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.osgi.framework.ServiceRegistration;

import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JaxrsServerManagerTest extends MockObjectTestCase {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    public static final String BASE_ADDRESS = "http://localhost:8383/test";
    @Rule
    public final OsgiRule osgiRule = new OsgiRule();

    @Mock
    private JaxrsServerFactory factory;

    @Mock
    private JaxrsServer jaxrsServer;

    @Captor
    private ArgumentCaptor<Map<String, Object>> propertiesCaptor;

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Test
    public void testDestroyServersUponShutdown() {
        final HelloServiceImpl impl = new HelloServiceImpl();
        registerServer(impl);
        final JaxrsServerManager manager = createManager();
        manager.destroy();
        verify(jaxrsServer).shutdown();
    }

    @Test
    public void testServiceDiscovery() {
        JaxrsServerManager manager = createManager();

        final HelloServiceImpl impl = new HelloServiceImpl();
        final ServiceRegistration<HelloServiceImpl> registration = registerServer(impl);

        verify(factory).createJaxrsServer(eq(HelloService.class), same(impl), eq(BASE_ADDRESS), propertiesCaptor.capture());
        Map<String, Object> properties = propertiesCaptor.getValue();
        assertEquals("bar", properties.get("foo"));
        assertEquals(BASE_ADDRESS, properties.get(JaxrsServerManager.ADDRESS_PROP));

        registration.unregister();
        verify(jaxrsServer).shutdown();
    }

    private JaxrsServerManager createManager() {
        JaxrsServerManager manager = new JaxrsServerManager(osgiRule.getBundleContext(), factory);
        manager.init();
        return manager;
    }

    private ServiceRegistration<HelloServiceImpl> registerServer(HelloServiceImpl impl) {
        when(factory.createJaxrsServer(eq(HelloService.class), same(impl), eq(BASE_ADDRESS), anyMap())).thenReturn(jaxrsServer);
        return osgiRule.registerService(HelloService.class, impl, ServicePropsBuilder.props().with(JaxrsServerManager.ADDRESS_PROP, BASE_ADDRESS).with("foo", "bar"));
    }

    @Test
    public void testServiceDiscoveryWithExistingService() {
        final HelloServiceImpl impl = new HelloServiceImpl();
        final ServiceRegistration<HelloServiceImpl> registration = registerServer(impl);

        JaxrsServerManager manager = createManager();

        verify(factory).createJaxrsServer(eq(HelloService.class), same(impl), eq(BASE_ADDRESS), propertiesCaptor.capture());
        Map<String, Object> properties = propertiesCaptor.getValue();
        assertEquals("bar", properties.get("foo"));
        assertEquals(BASE_ADDRESS, properties.get(JaxrsServerManager.ADDRESS_PROP));

        registration.unregister();
        verify(jaxrsServer).shutdown();
    }
}