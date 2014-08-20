package io.undertow.server.handlers;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.testutils.DefaultServer;
import io.undertow.testutils.HttpClientUtils;
import io.undertow.testutils.TestHttpClient;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import io.undertow.util.Methods;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(DefaultServer.class)
public class CompositeHandlerBuilderTestCase {

    @Test
    public void testComposition() throws IOException {
        TestHttpClient client = new TestHttpClient();
        try {
            final HttpHandler handler = Handlers.compositeBuilder(new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.getResponseSender().send("<div>418 I'm a teapot</div>");
                }
            })
            .header(Headers.CONTENT_TYPE_STRING, "text/html")
            .allowedMethods(Methods.GET)
            .responseCode(418)
            .build();
            DefaultServer.setRootHandler(handler);

            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(418, result.getStatusLine().getStatusCode());
            Assert.assertEquals("<div>418 I'm a teapot</div>", HttpClientUtils.readResponse(result));
            Assert.assertEquals("text/html", result.getFirstHeader(Headers.CONTENT_TYPE_STRING).getValue());

            HttpPost post = new HttpPost(DefaultServer.getDefaultServerURL() + "/");
            post.setEntity(new StringEntity("foo"));
            result = client.execute(post);
            Assert.assertEquals(405, result.getStatusLine().getStatusCode());
            HttpClientUtils.readResponse(result);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    @Test
    public void testCompositionWithCast() throws IOException {
        TestHttpClient client = new TestHttpClient();
        try {
            final HttpHandler handler = Handlers.compositeBuilder(new HttpHandler() {
                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.getResponseSender().send("<div>418 I'm a teapot</div>");
                }
            })
            .header(Headers.CONTENT_TYPE_STRING, "text/html")
            .allowedMethods(Methods.GET)
            .responseCode(418)
            .as(MyCustomBuilder.class)
            .custom1()
            .build();
            DefaultServer.setRootHandler(handler);

            HttpGet get = new HttpGet(DefaultServer.getDefaultServerURL() + "/");
            HttpResponse result = client.execute(get);
            Assert.assertEquals(418, result.getStatusLine().getStatusCode());
            Assert.assertEquals("<div>418 I'm a teapot</div>", HttpClientUtils.readResponse(result));
            Assert.assertEquals("text/html", result.getFirstHeader(Headers.CONTENT_TYPE_STRING).getValue());
            Assert.assertEquals("bar", result.getFirstHeader("foo").getValue());

            HttpPost post = new HttpPost(DefaultServer.getDefaultServerURL() + "/");
            post.setEntity(new StringEntity("foo"));
            result = client.execute(post);
            Assert.assertEquals(405, result.getStatusLine().getStatusCode());
            HttpClientUtils.readResponse(result);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private static class MyCustomBuilder extends CompositeHandlerBuilder {

        public MyCustomBuilder(HttpHandler handler) {
            super(handler);
        }

        public MyCustomBuilder custom1() {
            return new MyCustomBuilder(new HttpHandler() {

                @Override
                public void handleRequest(HttpServerExchange exchange) throws Exception {
                    exchange.getResponseHeaders().put(new HttpString("foo"), "bar");
                    handler.handleRequest(exchange);
                }
            });
        }

    }
}
