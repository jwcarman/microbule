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

package org.microbule.config.consul;

import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import javax.json.bind.JsonbBuilder;

import com.google.common.base.Charsets;
import com.google.common.reflect.TypeToken;
import org.junit.Before;
import org.junit.Test;
import org.microbule.config.api.Config;
import org.microbule.config.core.ConfigUtils;
import org.microbule.container.core.SimpleContainer;
import org.microbule.jsonb.api.JsonbFactory;
import org.microbule.jsonb.core.DefaultJsonbFactory;
import org.microbule.jsonb.decorator.JsonbProxyDecorator;
import org.microbule.jsonb.decorator.JsonbServerDecorator;
import org.microbule.test.server.JaxrsServerTestCase;

public class ConsulConfigProviderTest extends JaxrsServerTestCase<MockConsulService> {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final TypeToken<List<ConsulNode>> TYPE_TOKEN = new TypeToken<List<ConsulNode>>() {
    };
    private final AtomicReference<List<ConsulNode>> response = new AtomicReference<>();

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected void addBeans(SimpleContainer container) {
        final JsonbFactory jsonbFactory = new DefaultJsonbFactory(container);
        container.addBean(new JsonbServerDecorator(jsonbFactory));
        container.addBean(new JsonbProxyDecorator(jsonbFactory));
    }

    @Override
    protected MockConsulService createImplementation() {
        return new MockConsulServiceImpl(response);
    }

    @Before
    public void setBaseAddress() {
        System.setProperty("microbule.consul.baseAddress", getBaseAddress());
    }

    @Test
    public void testGetConfig() {
        final ConsulConfigProvider provider = new ConsulConfigProvider();
        assertEquals("consul", provider.name());
        assertEquals(ConfigUtils.PRIORITY_EXTERNAL, provider.priority());

        response.set(parseResponse("/response.json", TYPE_TOKEN));

        final Config config = provider.getConfig("one", "two");
        assertEquals("Microbule", config.value("three").get());
        assertEquals("three, sir", config.value("five").get());
    }

    private <T> T parseResponse(String resourceName, TypeToken<T> type) {
        return JsonbBuilder.create().fromJson(new InputStreamReader(getClass().getResourceAsStream(resourceName), Charsets.UTF_8), type.getType());
    }
}