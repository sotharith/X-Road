/**
 * The MIT License
 * Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.common.util.healthcheck;

import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.util.StartStop;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.nio.SelectChannelConnector;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringReader;

import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;


/**
 * Service that listens for health check requests on a specific port and interface
 */
@Slf4j
public class HealthCheckPort implements StartStop {

    private final Server server = new Server();

    private final int portNumber;

    private final StoppableHealthCheckProvider stoppableHealthCheckProvider;

    private static final int SOCKET_MAX_IDLE_MILLIS = 30000;
    private static final int THREAD_POOL_SIZE = 3;
    private static final int ACCEPTOR_THREAD_COUNT = 1;
    private static final int SO_LINGER = 3;


    /**
     * Create a new {@link HealthCheckPort} use the implemented {@link StartStop} interface to start/stop it.
     */
    public HealthCheckPort() {
        stoppableHealthCheckProvider = new StoppableCombinationHealthCheckProvider();
        portNumber = SystemProperties.getHealthCheckPort();
        createHealthCheckConnector();
    }

    private void createHealthCheckConnector() {
        SelectChannelConnector connector = new SelectChannelConnector();

        // use minimal resources, this is not supposed to be a resource hog

        connector.setName("HealthCheckPort");
        connector.setHost(SystemProperties.getHealthCheckInterface());
        connector.setPort(portNumber);
        connector.setMaxIdleTime(SOCKET_MAX_IDLE_MILLIS);
        connector.setThreadPool(new QueuedThreadPool(THREAD_POOL_SIZE));
        connector.setSoLingerTime(SO_LINGER);
        connector.setAcceptors(ACCEPTOR_THREAD_COUNT);

        server.addConnector(connector);

        HandlerCollection handlerCollection = new HandlerCollection();
        handlerCollection.addHandler(new HealthCheckHandler(stoppableHealthCheckProvider));

        server.setHandler(handlerCollection);
    }

    @Override
    public void start() throws Exception {
        log.info("Started HealthCheckPort on port {}", portNumber);
        server.start();
    }

    @Override
    public void join() throws InterruptedException {
        if (server.getThreadPool() != null) {
            log.info("Waiting for the HealthCheckPort thread pool to stop.");
            server.join();
            log.info("HealthCheckPort thread pool was stopped.");
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping HealthCheckPort on port {}", portNumber);
        server.stop();
        stoppableHealthCheckProvider.stop();
    }


    /**
     * A {@link org.eclipse.jetty.server.Handler} for health check requests. It responds to all requests the same way
     * based on the health check results and does not filter out requests at all. Blocking requests is left for the
     * firewall.
     */
    @RequiredArgsConstructor
    public static class HealthCheckHandler extends AbstractHandler {

        private final HealthCheckProvider healthCheckProvider;

        @Override
        public void handle(String target, Request baseRequest,
                           HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {

            HealthCheckResult result = healthCheckProvider.get();

            if (result.isOk()) {
                response.setStatus(SC_OK);
            } else {
                response.setStatus(SC_INTERNAL_SERVER_ERROR);
                IOUtils.copy(new StringReader(result.getErrorMessage().concat("\n")),
                        response.getOutputStream());
            }

            baseRequest.setHandled(true);
        }
    }
}
