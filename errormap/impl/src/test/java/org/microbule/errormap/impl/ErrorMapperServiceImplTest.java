package org.microbule.errormap.impl;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.junit.Before;
import org.junit.Test;
import org.microbule.container.core.SimpleContainer;
import org.microbule.test.core.MockObjectTestCase;

public class ErrorMapperServiceImplTest extends MockObjectTestCase {

    private ErrorMapperServiceImpl service;

    @Before
    public void initService() {
        SimpleContainer container = new SimpleContainer();
        container.addBean(new WebApplicationExceptionErrorMapper());
        service = new ErrorMapperServiceImpl(container);
        container.initialize();
    }

    @Test
    public void testCreateResponse() {
        final Response response = service.createResponse(new NotFoundException("Didn't find it!"));
        assertEquals(404, response.getStatus());
        assertEquals("Didn't find it!", response.readEntity(String.class));
    }

    @Test
    public void testCreateResponseWithNoMapper() {
        final Response response = service.createResponse(new RuntimeException("Didn't find it!"));
        assertEquals(500, response.getStatus());
        assertEquals("Didn't find it!", response.readEntity(String.class));
    }

    @Test
    public void testCreateException() {
        final Exception exception = service.createException(Response.status(Response.Status.NOT_FOUND).build());
        assertEquals(NotFoundException.class, exception.getClass());
    }


}