package io.undertow.server.handlers;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.accesslog.AccessLogHandler;
import io.undertow.server.handlers.accesslog.AccessLogReceiver;
import io.undertow.util.AttachmentKey;
import io.undertow.util.HttpString;

/**
 * A handler which can fluently construct complex handlers.
 * Outside libraries can extend the abstract class in order to add additional custom handlers.
 * @author bill
 *
 * @param <T>
 */
public class CompositeHandlerBuilder {
    protected final HttpHandler handler;

    public CompositeHandlerBuilder(HttpHandler handler) {
        Handlers.handlerNotNull(handler);
        this.handler = handler;
    }

    public HttpHandler build() {
        return handler;
    }

    public <T extends CompositeHandlerBuilder> T as(Class<T> clazz) {
        try {
            return clazz.getConstructor(HttpHandler.class).newInstance(handler);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    public CompositeHandlerBuilder trace() {
        return new CompositeHandlerBuilder(Handlers.trace(handler));
    }

    public CompositeHandlerBuilder header(String headerName, String headerValue) {
        return new CompositeHandlerBuilder(Handlers.header(handler, headerName, headerValue));
    }

    public CompositeHandlerBuilder requestLimiting(final int maxRequest, final int queueSize) {
        return new CompositeHandlerBuilder(Handlers.requestLimitingHandler(maxRequest, queueSize, handler));
    }

    public CompositeHandlerBuilder requestLimiting(final RequestLimit requestLimit) {
        return new CompositeHandlerBuilder(Handlers.requestLimitingHandler(requestLimit, handler));
    }

    public CompositeHandlerBuilder allowedMethods(HttpString... allowedMethods) {
        return new CompositeHandlerBuilder(new AllowedMethodsHandler(handler, allowedMethods));
    }

    public CompositeHandlerBuilder disallowedMethods(HttpString... allowedMethods) {
        return new CompositeHandlerBuilder(new DisallowedMethodsHandler(handler, allowedMethods));
    }

    public CompositeHandlerBuilder blocking() {
        return new CompositeHandlerBuilder(new BlockingHandler(handler));
    }

    public <T> CompositeHandlerBuilder attachment(AttachmentKey<T> key) {
        return new CompositeHandlerBuilder(new AttachmentHandler<>(key, handler));
    }

    public <T> CompositeHandlerBuilder attachment(AttachmentKey<T> key, T instance) {
        return new CompositeHandlerBuilder(new AttachmentHandler<>(key, handler, instance));
    }

    public CompositeHandlerBuilder metrics() {
        return new CompositeHandlerBuilder(new MetricsHandler(handler));
    }

    public CompositeHandlerBuilder requestDumping() {
        return new CompositeHandlerBuilder(new RequestDumpingHandler(handler));
    }

    /*
     * I didn't want to modify the existing status code handler.
     * Maybe there should be a second that delegates to an internal handler?
     * Possibly an optional handler and all the singletons in the class pass in null/
     */
    public CompositeHandlerBuilder responseCode(final int responseCode) {
        return new CompositeHandlerBuilder(new HttpHandler() {
            @Override
            public void handleRequest(HttpServerExchange exchange) throws Exception {
                exchange.setResponseCode(responseCode);
                handler.handleRequest(exchange);
            }
        });
    }

    public CompositeHandlerBuilder requestLogging(final AccessLogReceiver accessLogReceiver, final String formatString, ClassLoader classLoader) {
        return new CompositeHandlerBuilder(new AccessLogHandler(handler, accessLogReceiver, formatString, classLoader));
    }
}
