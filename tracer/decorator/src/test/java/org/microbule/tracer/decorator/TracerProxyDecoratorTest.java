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

package org.microbule.tracer.decorator;

import org.junit.Assert;
import org.junit.Test;
import org.microbule.container.core.SimpleContainer;
import org.microbule.test.core.hello.HelloService;
import org.microbule.test.server.hello.HelloTestCase;
import org.slf4j.MDC;

public class TracerProxyDecoratorTest extends HelloTestCase {
//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected void addBeans(SimpleContainer container) {
        container.addBean(new TracerProxyDecorator());
    }

    @Test
    public void testWithCustomTraceId() {
        final HelloService proxy = createProxy();
        MDC.put(AbstractTracerDecorator.TRACE_ID_KEY, "foobarbaz");
        Assert.assertEquals("Hello, Microbule!", proxy.sayHello("Microbule"));
        MDC.remove(AbstractTracerDecorator.TRACE_ID_KEY);
    }
}