package org.microbule.decorator.cache;

import java.time.temporal.ChronoUnit;
import java.util.Date;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.utils.HttpUtils;
import org.junit.Test;
import org.microbule.core.JaxrsServerFactoryImpl;
import org.microbule.decorator.cache.resource.CacheResource;
import org.microbule.decorator.cache.resource.CacheResourceImpl;
import org.microbule.test.JaxrsTestCase;

public class CacheServerDecoratorTest extends JaxrsTestCase<CacheResource> {
    @Override
    protected CacheResource createImplementation() {
        return new CacheResourceImpl();
    }

    @Override
    protected void addDecorators(JaxrsServerFactoryImpl factory) {
        factory.addDecorator("cache", new CacheServerDecorator());
    }

    @Test
    public void testEntityTagSupport() {
        final Response response = createWebTarget().path("value").path("etag").request(MediaType.TEXT_PLAIN).get();
        CacheControl cacheControl = CacheControl.valueOf(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
        assertEquals(600, cacheControl.getMaxAge());
        assertTrue(cacheControl.isNoTransform());
        assertFalse(cacheControl.isMustRevalidate());
        final EntityTag entityTag = response.getEntityTag();
        assertNotNull(entityTag);
        assertEquals("12345", entityTag.getValue());
    }

    @Test
    public void testLastUpdatedSupport() {
        final Response response = createWebTarget().path("value").path("lastModified").request(MediaType.TEXT_PLAIN).get();
        CacheControl cacheControl = CacheControl.valueOf(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
        assertEquals(600, cacheControl.getMaxAge());
        assertTrue(cacheControl.isNoTransform());
        assertFalse(cacheControl.isMustRevalidate());
        Date lastModified = response.getLastModified();
        assertEquals(CacheResource.LAST_MODIFIED, lastModified);

    }

    @Test
    public void testLastUpdatedAndEtagSupport() {
        final Response response = createWebTarget().path("value").path("lastModifiedAndEtag").request(MediaType.TEXT_PLAIN).get();
        CacheControl cacheControl = CacheControl.valueOf(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
        assertEquals(600, cacheControl.getMaxAge());
        assertTrue(cacheControl.isNoTransform());
        assertFalse(cacheControl.isMustRevalidate());

        Date lastModified = response.getLastModified();
        assertEquals(CacheResource.LAST_MODIFIED, lastModified);

        final EntityTag entityTag = response.getEntityTag();
        assertNotNull(entityTag);
        assertEquals("12345", entityTag.getValue());

    }

    @Test
    public void testWithNoState() {
        final Response response = createWebTarget().path("value").path("noState").request(MediaType.TEXT_PLAIN).get();
        CacheControl cacheControl = CacheControl.valueOf(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
        assertEquals(600, cacheControl.getMaxAge());
        assertTrue(cacheControl.isNoTransform());
        assertFalse(cacheControl.isMustRevalidate());

        assertNull(response.getLastModified());
        assertNull(response.getEntityTag());
    }

    @Test
    public void testWithResponse() {
        final Response response = createProxy().createResponse();
        CacheControl cacheControl = CacheControl.valueOf(response.getHeaderString(HttpHeaders.CACHE_CONTROL));
        assertEquals(600, cacheControl.getMaxAge());
        assertTrue(cacheControl.isNoTransform());
        assertFalse(cacheControl.isMustRevalidate());

        Date lastModified = response.getLastModified();
        assertEquals(CacheResource.LAST_MODIFIED, lastModified);

        final EntityTag entityTag = response.getEntityTag();
        assertNotNull(entityTag);
        assertEquals("12345", entityTag.getValue());
    }

    @Test
    public void testEmptyResponseWithMatchingEtag() {
        final Response response = createWebTarget().path("value").path("etag")
                .request(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.IF_NONE_MATCH, CacheResource.ENTITY_TAG)
                .get();
        assertEquals(204, response.getStatus());

    }

    @Test
    public void testEmptyResponseWithUnmodified() {
        final Response response = createWebTarget().path("value").path("lastModified")
                .request(MediaType.TEXT_PLAIN)
                .header(HttpHeaders.IF_MODIFIED_SINCE, HttpUtils.getHttpDateFormat().format(Date.from(CacheResource.LAST_MODIFIED.toInstant().plus(5, ChronoUnit.MINUTES))))
                .get();
        assertEquals(204, response.getStatus());

    }
}