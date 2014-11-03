/*
 * Copyright 2002-2014 the original author or authors.
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
 */

package com.wk.lodge.composite.web.tomcat;

import com.wk.lodge.composite.config.Initializer;
import com.wk.lodge.composite.config.TestRootConfig;
import com.wk.lodge.composite.config.TestWebSocketConfig;
import com.wk.lodge.composite.config.WebConfig;
import com.wk.lodge.composite.model.Device;
import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.repository.SessionRepository;
import com.wk.lodge.composite.web.socket.message.inbound.JoinMessage;
import com.wk.lodge.composite.web.socket.message.inbound.PairMessage;
import com.wk.lodge.composite.web.socket.message.inbound.SyncMessage;
import com.wk.lodge.composite.web.support.client.StompMessageHandler;
import com.wk.lodge.composite.web.support.client.StompSession;
import com.wk.lodge.composite.web.support.client.WebSocketStompClient;
import com.wk.lodge.composite.web.support.server.TomcatWebSocketTestServer;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.JsonPathExpectationsHelper;
import org.springframework.util.SocketUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.RestTemplateXhrTransport;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.Assert.fail;

/**
 * End-to-end integration tests that run an embedded Tomcat server and establish
 * an actual WebSocket session using
 * {@link org.springframework.web.socket.client.standard.StandardWebSocketClient}.
 * as well as a simple STOMP/WebSocket client created to support these tests.
 *
 * The test strategy here is to test from the perspective of a client connecting
 * to a server and therefore it is a much more complete test. However, writing
 * and maintaining these tests is a bit more involved.
 *
 * An all-encapsulating strategy might be to write the majority of tests using
 * server-side testing (either standalone or with Spring configuration) with
 * end-to-end integration tests serving as a higher-level verification but
 * overall fewer in number.
 *
 * @author Rossen Stoyanchev
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestWebSocketConfig.class, TestRootConfig.class })
public class IntegrationCompositeTests {

    private static Log logger = LogFactory.getLog(IntegrationCompositeTests.class);
    private static int port;
    private static TomcatWebSocketTestServer server;
    private static SockJsClient sockJsClient;
    private final static WebSocketHttpHeaders headers = new WebSocketHttpHeaders();
    private static String webSocketUri;
    private static float lat = 0F;
    private static float lon = 0F;

    @Autowired
    private SessionRepository sessionRepository;

    @BeforeClass
    public static void setup() throws Exception {

        System.setProperty("spring.profiles.active", "test.tomcat");

        port = SocketUtils.findAvailableTcpPort();

        server = new TomcatWebSocketTestServer(port);
        server.deployConfig(TestDispatcherServletInitializer.class);
        server.start();

        webSocketUri = "ws://localhost:" + port + "/composite/websocket";

        List<Transport> transports = new ArrayList<>();
        transports.add(new WebSocketTransport(new StandardWebSocketClient()));
        RestTemplateXhrTransport xhrTransport = new RestTemplateXhrTransport(new RestTemplate());
        xhrTransport.setRequestHeaders(headers);
        transports.add(xhrTransport);

        sockJsClient = new SockJsClient(transports);

    }

    @AfterClass
    public static void teardown() throws Exception {
        if (server != null) {
            try {
                server.undeployConfig();
            }
            catch (Throwable t) {
                logger.error("Failed to undeploy application", t);
            }

            try {
                server.stop();
            }
            catch (Throwable t) {
                logger.error("Failed to stop server", t);
            }
        }
    }

    // Used to simulate a join event from the device that inits a connect to the socket broker
    private String simulateJoinEvent() {
        Device device = new Device();
        device.setUuid(UUID.randomUUID());
        device.setWidth(1282);
        device.setHeight(849);

        JoinMessage join = new JoinMessage();
        join.setDevice(device);
        join.setGeo(new float[]{lat, lon});
        join.setPoint(new int[]{0, 0});
        join.setVector(new float[]{1, 1});
        join.setType(JoinMessage.Types.exit);
        join.setApplicationId("appOne");

        Session session = sessionRepository.getOrCreate(join);
        return session.getUuid().toString();

    }

    @Test
    public void testInit() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                this.stompSession.subscribe("/user/queue/device", null);

                try {
                    this.stompSession.send("/app/init", "");
                }
                catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
                if (!"/user/queue/device".equals(headers.getDestination())) {
                    failure.set(new IllegalStateException("Unexpected message: " + message));
                }
                logger.debug("Got " + new String((byte[]) message.getPayload()));
                try{
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("type").assertValue(json, "init");
                    new JsonPathExpectationsHelper("uuid").exists(json);
                }
                catch (Throwable t) {
                    failure.set(t);
                }
                finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {}

            @Override
            public void afterDisconnected() {}
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Init response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }
    }

    @Test
    public void testJoin() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                this.stompSession.subscribe("/user/queue/device", null);

                try {
                    JoinMessage join = new JoinMessage();
                    Device d = new Device();
                    d.setUuid(UUID.randomUUID());
                    join.setDevice(d);
                    join.setGeo(new float[]{lat, lon});
                    join.setType(JoinMessage.Types.exit);
                    join.setPoint(new int[]{0, 0});
                    join.setVector(new float[]{1, 1});

                    stompSession.send("/app/join", join);
                } catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                try{
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("devices").exists(json);
                    new JsonPathExpectationsHelper("devices").assertValueIsArray(json);
                    new JsonPathExpectationsHelper("type").assertValue(json, "join");
                }
                catch (Throwable t) {
                    failure.set(t);
                }
                finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {}

            @Override
            public void afterDisconnected() {}
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Join response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }
    }

    @Test
    public void testPair() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<Throwable>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {

                this.stompSession = stompSession;
                this.stompSession.subscribe("/user/queue/device", null);

                try {
                    PairMessage pair = new PairMessage();
                    Device d = new Device();
                    d.setUuid(UUID.randomUUID());
                    pair.setDevice(d);
                    pair.setType(PairMessage.Types.exit);
                    pair.setGeo(new float[]{lat, lon});
                    pair.setPoint(new int[]{0, 0});
                    pair.setVector(new float[]{1, 1});

                    stompSession.send("/app/pair", pair);
                }
                catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                try{
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("devices").exists(json);
                    new JsonPathExpectationsHelper("devices").assertValueIsArray(json);
                    new JsonPathExpectationsHelper("type").assertValue(json, "pair");
                }
                catch (Throwable t) {
                    failure.set(t);
                }
                finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {}

            @Override
            public void afterDisconnected() {}
        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Pair response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }

    }

    @Test
    public void testDevices() throws Exception {
    final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler(){

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                String topicUuid = simulateJoinEvent();

                // Call again to get a second device in the session
                simulateJoinEvent();

                this.stompSession.subscribe("/user/queue/device", null);
                this.stompSession.subscribe(String.format("/topic/%s", topicUuid), null);

                try {
                    // send a message to the devices endpoint
                    HashMap<String,Object> devices = new HashMap<String, Object>();
                    devices.put("type", "devices");
                    this.stompSession.send(String.format("/app/%s", topicUuid), devices);
                }
                catch (Throwable t){
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                try{
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("devices").exists(json);
                    new JsonPathExpectationsHelper("type").exists(json);
                    new JsonPathExpectationsHelper("type").assertValue(json, "devices");
                    new JsonPathExpectationsHelper("serverTime").exists(json);
                }
                catch (Throwable t) {
                    failure.set(t);
                }
                finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {}

            @Override
            public void afterDisconnected() {}
        });

        if(!latch.await(10, TimeUnit.SECONDS)){
            fail("Devices response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }
    }

    @Test
    public void testSync() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                this.stompSession.subscribe("/user/queue/device", null);

                try {
                    SyncMessage sync = new SyncMessage();
                    sync.setType("sync");
                    sync.setTime(new Date().getTime());
                    stompSession.send("/app/sync", sync);
                } catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }


            @Override
            public void handleMessage(Message<byte[]> message) {
                try {
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("type").assertValue(json, "sync");
                    new JsonPathExpectationsHelper("time").exists(json);
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {
            }

            @Override
            public void afterDisconnected() {
            }

        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Sync response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }

    }

    @Test
    public void testStart() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler(){

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                String topicUuid = simulateJoinEvent();

                this.stompSession.subscribe("/user/queue/device", null);
                this.stompSession.subscribe(String.format("/topic/%s", topicUuid), null);

                try {
                    // send a message to the start endpoint
                    HashMap<String,Object> start = new HashMap<String, Object>();
                    start.put("data","Some Data");
                    start.put("type", "start");
                    this.stompSession.send(String.format("/app/%s", topicUuid), start);
                }
                catch (Throwable t){
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                try{
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("type").exists(json);
                    new JsonPathExpectationsHelper("type").assertValue(json,"start");
                    new JsonPathExpectationsHelper("serverTime").exists(json);
                }
                catch (Throwable t) {
                    failure.set(t);
                }
                finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {}

            @Override
            public void afterDisconnected() {}
        });

        if(!latch.await(10, TimeUnit.SECONDS)){
            fail("Start response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }
    }

    @Test
    public void testStop() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                String topicUuid = simulateJoinEvent();

                this.stompSession.subscribe("/user/queue/device", null);
                this.stompSession.subscribe(String.format("/topic/%s", topicUuid), null);

                try {
                    HashMap<String, Object> stop = new HashMap<String, Object>();
                    stop.put("type", "stop");
                    this.stompSession.send(String.format("/app/%s", topicUuid), stop);

                } catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                try {
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("type").exists(json);
                    new JsonPathExpectationsHelper("type").assertValue(json, "stop");
                    new JsonPathExpectationsHelper("serverTime").exists(json);
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {
            }

            @Override
            public void afterDisconnected() {
            }

        });

        if(!latch.await(10, TimeUnit.SECONDS)){
            fail("Stop response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }

    }

    @Test
    public void testUpdate() throws Exception {
        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                String topicUuid = simulateJoinEvent();

                this.stompSession.subscribe("/user/queue/device", null);
                this.stompSession.subscribe(String.format("/topic/%s", topicUuid), null);

                try {
                    HashMap<String, Object> update = new HashMap<String, Object>();
                    update.put("data", "TEST-UPDATE Data");
                    update.put("type", "update");
                    this.stompSession.send(String.format("/app/%s", topicUuid), update);
                } catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }

            }

            @Override
            public void handleMessage(Message<byte[]> message) throws MessagingException {
                try {
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("type").exists(json);
                    new JsonPathExpectationsHelper("type").assertValue(json, "update");
                    new JsonPathExpectationsHelper("serverTime").exists(json);
                    new JsonPathExpectationsHelper("data").assertValue(json, "TEST-UPDATE Data");
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {
            }

            @Override
            public void afterDisconnected() {
            }

        });

        if(!latch.await(10, TimeUnit.SECONDS)){
            fail("Update response not received");
        }
        else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }
    }

    @Test
    public void testData() throws Exception {

        final CountDownLatch latch = new CountDownLatch(1);
        final AtomicReference<Throwable> failure = new AtomicReference<>();

        URI uri = new URI("ws://localhost:" + port + "/composite");
        WebSocketStompClient stompClient = new WebSocketStompClient(uri, this.headers, sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter());

        stompClient.connect(new StompMessageHandler() {

            private StompSession stompSession;

            @Override
            public void afterConnected(StompSession stompSession, StompHeaderAccessor headers) {
                this.stompSession = stompSession;
                String topicUuid = simulateJoinEvent();

                this.stompSession.subscribe("/user/queue/device", null);
                this.stompSession.subscribe(String.format("/topic/%s", topicUuid), null);

                try {
                    // send a simple hashmap to the data endpoint
                    HashMap<String, Object> data = new HashMap<String, Object>();
                    data.put("data", "TEST-DATA Data");
                    data.put("type", "data");
                    this.stompSession.send(String.format("/app/%s", topicUuid), data);
                } catch (Throwable t) {
                    failure.set(t);
                    latch.countDown();
                }
            }

            @Override
            public void handleMessage(Message<byte[]> message) {
                try {
                    String json = parseMessageJson(message);
                    new JsonPathExpectationsHelper("type").exists(json);
                    new JsonPathExpectationsHelper("type").assertValue(json, "data");
                    new JsonPathExpectationsHelper("serverTime").exists(json);
                    new JsonPathExpectationsHelper("data").assertValue(json, "TEST-DATA Data");
                } catch (Throwable t) {
                    failure.set(t);
                } finally {
                    this.stompSession.disconnect();
                    latch.countDown();
                }
            }

            @Override
            public void handleError(Message<byte[]> message) {
                StompHeaderAccessor accessor = StompHeaderAccessor.wrap(message);
                String error = "[Producer] " + accessor.getShortLogMessage(message.getPayload());
                logger.error(error);
                failure.set(new Exception(error));
            }

            @Override
            public void handleReceipt(String receiptId) {
            }

            @Override
            public void afterDisconnected() {
            }

        });

        if (!latch.await(10, TimeUnit.SECONDS)) {
            fail("Data response not received");
        } else if (failure.get() != null) {
            throw new AssertionError("", failure.get());
        }
    }

    private String parseMessageJson(Message<?> message) throws UnsupportedEncodingException {
        return new String((byte[]) message.getPayload(), "UTF-8");
    }

    public static class TestDispatcherServletInitializer extends Initializer {
        @Override
        protected Class<?>[] getServletConfigClasses() {
            return new Class[] { WebConfig.class, TestWebSocketConfig.class };
        }
    }

}
