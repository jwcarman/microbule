package org.microbule.tracer.decorator;

import org.junit.Assert;
import org.junit.Test;
import org.microbule.core.DefaultJaxrsProxyFactory;
import org.microbule.test.hello.HelloService;
import org.microbule.test.hello.HelloTestCase;
import org.slf4j.MDC;

public class TracerProxyDecoratorTest extends HelloTestCase {
//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected void addDecorators(DefaultJaxrsProxyFactory factory) {
        factory.addDecorator("tracer", new TracerProxyDecorator(TracerConstants.DEFAULT_TRACE_ID_HEADER));
    }

    @Test
    public void testWithCustomTraceId() {
        final HelloService proxy = createProxy();
        MDC.put(TracerConstants.TRACE_ID_KEY, "foobarbaz");
        Assert.assertEquals("Hello, Microbule!", proxy.sayHello("Microbule"));
        MDC.remove(TracerConstants.TRACE_ID_KEY);
    }
}