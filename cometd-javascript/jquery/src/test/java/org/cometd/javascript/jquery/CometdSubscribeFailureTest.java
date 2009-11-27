package org.cometd.javascript.jquery;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.eclipse.jetty.servlet.FilterHolder;
import org.eclipse.jetty.servlet.FilterMapping;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.mozilla.javascript.ScriptableObject;

/**
 * @version $Revision: 1453 $ $Date: 2009-02-25 12:57:20 +0100 (Wed, 25 Feb 2009) $
 */
public class CometdSubscribeFailureTest extends AbstractCometdJQueryTest
{
    @Override
    protected void customizeContext(ServletContextHandler context) throws Exception
    {
        super.customizeContext(context);
        SubscribeThrowingFilter filter = new SubscribeThrowingFilter();
        FilterHolder filterHolder = new FilterHolder(filter);
        context.addFilter(filterHolder, cometServletPath + "/*", FilterMapping.REQUEST);
    }

    public void testSubscribeFailure() throws Exception
    {
        defineClass(Listener.class);
        evaluateScript("$.cometd.init({url: '" + cometdURL + "', logLevel: 'debug'})");

        // Wait for the long poll
        Thread.sleep(1000);

        evaluateScript("var subscribeListener = new Listener();");
        Listener subscribeListener = get("subscribeListener");
        evaluateScript("var failureListener = new Listener();");
        Listener failureListener = get("failureListener");
        String script = "$.cometd.addListener('/meta/subscribe', subscribeListener, subscribeListener.handle);";
        script += "$.cometd.addListener('/meta/unsuccessful', failureListener, failureListener.handle);";
        evaluateScript(script);

        subscribeListener.jsFunction_expect(1);
        failureListener.jsFunction_expect(1);
        evaluateScript("$.cometd.subscribe('/echo', subscribeListener, subscribeListener.handle);");
        assertTrue(subscribeListener.await(1000));
        assertTrue(failureListener.await(1000));

        // Be sure there is no backoff
        evaluateScript("var backoff = $.cometd.getBackoffPeriod();");
        int backoff = ((Number)get("backoff")).intValue();
        assertEquals(0, backoff);

        evaluateScript("var disconnectListener = new Listener();");
        Listener disconnectListener = get("disconnectListener");
        disconnectListener.jsFunction_expect(1);
        script = "$.cometd.addListener('/meta/disconnect', disconnectListener, disconnectListener.handle);";
        script += "$.cometd.disconnect();";
        evaluateScript(script);
        assertTrue(disconnectListener.await(1000));
        String status = evaluateScript("$.cometd.getStatus();");
        assertEquals("disconnected", status);
    }

    public static class Listener extends ScriptableObject
    {
        private CountDownLatch latch;

        public void jsFunction_expect(int messageCount)
        {
            latch = new CountDownLatch(messageCount);
        }

        public String getClassName()
        {
            return "Listener";
        }

        public void jsFunction_handle(Object message)
        {
            latch.countDown();
        }

        public boolean await(long timeout) throws InterruptedException
        {
            return latch.await(timeout, TimeUnit.MILLISECONDS);
        }
    }

    public static class SubscribeThrowingFilter implements Filter
    {
        private int messages;

        public void init(FilterConfig filterConfig) throws ServletException
        {
        }

        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException
        {
            doFilter((HttpServletRequest)request, (HttpServletResponse)response, chain);
        }

        private void doFilter(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws IOException, ServletException
        {
            ++messages;
            // The fourth message will be the subscribe, throw
            if (messages == 4) throw new IOException();
            chain.doFilter(request, response);
        }

        public void destroy()
        {
        }
    }
}