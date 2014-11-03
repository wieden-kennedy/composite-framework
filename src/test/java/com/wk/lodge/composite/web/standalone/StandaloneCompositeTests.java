
package com.wk.lodge.composite.web.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wk.lodge.composite.model.Device;
import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.registry.DeviceRegistry;
import com.wk.lodge.composite.service.SessionService;
import com.wk.lodge.composite.web.CompositeController;
import com.wk.lodge.composite.web.socket.message.inbound.*;
import com.wk.lodge.composite.web.support.TestMessageChannel;
import com.wk.lodge.composite.web.support.TestPrincipal;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.SimpMessageSendingOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.JsonPathExpectationsHelper;

import java.util.Arrays;
import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

/**
 * Tests for CompositeController that instantiate directly the minimum
 * infrastructure necessary to test annotated controller methods and do not load
 * Spring configuration.
 *
 * Tests can create a Spring {@link org.springframework.messaging.Message} that
 * represents a STOMP frame and send it directly to the
 * SimpAnnotationMethodMessageHandler responsible for invoking annotated controller
 * methods
 *
 * The test strategy here is to test the behavior of controllers taking into
 * account controller annotations and nothing more. The tests are simpler to write
 * and faster to executed. They provide the most amount of control and that is good
 * for writing as many controller tests as needed. Separate tests are still required
 * to verify the Spring configuration but those tests should be fewer overall.
 *
 */

public class StandaloneCompositeTests {
    private SimpMessagingTemplate template;
    private DeviceRegistry deviceRegistry;
    private TestMessageChannel brokerTemplateChannel;
    private TestSimpAnnotationMethodMessageHandler annotationMethodMessageHandler;
    @Mock
    SessionService sessionService;

    @Before
    public void setup() {
        MockitoAnnotations.initMocks(this);
        this.deviceRegistry = new DeviceRegistry();

        this.brokerTemplateChannel = new TestMessageChannel();

        this.template = new SimpMessagingTemplate(this.brokerTemplateChannel);
        CompositeController controller = new CompositeController(sessionService,
                template, deviceRegistry);

        this.annotationMethodMessageHandler = new TestSimpAnnotationMethodMessageHandler(
                new TestMessageChannel(), new TestMessageChannel(), this.template);

        this.annotationMethodMessageHandler.registerHandler(controller);
        this.annotationMethodMessageHandler.setDestinationPrefixes(Arrays.asList("/app"));
        this.annotationMethodMessageHandler.setMessageConverter(new MappingJackson2MessageConverter());
        this.annotationMethodMessageHandler.setApplicationContext(new StaticApplicationContext());
        this.annotationMethodMessageHandler.afterPropertiesSet();

    }

    @Test
    public void init() throws Exception {
        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/init");
        headers.setSessionId("0");

        // test were complaining about no session id and session attributes
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create a BeanMessage object for message payload
        BeanMessage init = new BeanMessage();
        init.setType("init");
        byte[] payload = new ObjectMapper().writeValueAsBytes(init);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);

        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", uuid), replyHeaders.getDestination());
        //Make sure the uuid from above is in the payload
        new JsonPathExpectationsHelper("uuid").assertValue(reply.getPayload().toString(), uuid);
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "init");

    }

    @Test
    public void join() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/join");
        headers.setSessionId("0");

        // test were complaining about no session id and session attributes
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create 2 devices
        Device deviceA = new Device(uuid);
        deviceA.setHeight(400);
        deviceA.setWidth(400);
        Device deviceB = new Device(uuid);
        deviceB.setHeight(300);
        deviceB.setWidth(300);

        //Create a JoinMessage (touch exit event)
        JoinMessage join = new JoinMessage();
        join.setDevice(deviceB);
        join.setGeo(new float [] {45, 45});
        join.setType(JoinMessage.Types.exit);
        byte[] payload = new ObjectMapper().writeValueAsBytes(join);

        //Now we will mock the session service to return a session
        Session session = new Session();
        session.setUuid(sessionUuid);
        session.addDevice(deviceA);
        session.addDevice(deviceB);
        when(this.sessionService.get(Mockito.any(Object.class))).thenReturn(session);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Get second message
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);

        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", uuid), replyHeaders.getDestination());
        //Make sure the devices are in the payload
        new JsonPathExpectationsHelper("id").exists(reply.getPayload().toString());
        new JsonPathExpectationsHelper("devices").exists(reply.getPayload().toString());
        new JsonPathExpectationsHelper("devices").assertValueIsArray(reply.getPayload().toString());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "join");
        new JsonPathExpectationsHelper("$devices.[0].uuid").assertValue(reply.getPayload().toString(), deviceA.getUuid().toString());
        new JsonPathExpectationsHelper("$devices.[1].uuid").assertValue(reply.getPayload().toString(), deviceB.getUuid().toString());
    }

    @Test
    public void pair() throws Exception {
        UUID sessionUuid = UUID.randomUUID();
        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/pair");
        headers.setSessionId("0");

        // test were complaining about no session id and session attributes
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create 2 devices
        Device deviceA = new Device(uuid);
        deviceA.setHeight(400);
        deviceA.setWidth(400);
        Device deviceB = new Device(uuid);
        deviceB.setHeight(300);
        deviceB.setWidth(300);

        //Create a PairMessage
        PairMessage pair = new PairMessage();
        pair.setDevice(deviceB);
        pair.setType(PairMessage.Types.exit);
        pair.setGeo(new float[]{45, 45});
        byte[] payload = new ObjectMapper().writeValueAsBytes(pair);

        //Now we will mock the session service to return a session
        Session session = new Session();
        session.setUuid(sessionUuid);
        session.addDevice(deviceA);
        session.addDevice(deviceB);
        when(this.sessionService.pair(Mockito.any(PairMessage.class))).thenReturn(session);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Get message
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);

        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", uuid), replyHeaders.getDestination());
        //Make sure the devices are in the payload
        new JsonPathExpectationsHelper("id").exists(reply.getPayload().toString());
        new JsonPathExpectationsHelper("devices").exists(reply.getPayload().toString());
        new JsonPathExpectationsHelper("devices").assertValueIsArray(reply.getPayload().toString());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "pair");
        new JsonPathExpectationsHelper("$devices.[0].uuid").assertValue(reply.getPayload().toString(), deviceA.getUuid().toString());
        new JsonPathExpectationsHelper("$devices.[1].uuid").assertValue(reply.getPayload().toString(), deviceB.getUuid().toString());
    }

    @Test
    public void devices() throws Exception {
        UUID sessionUuid = UUID.randomUUID();

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(String.format("/app/%s", sessionUuid));

        // test were complaining about no session id and session attributes
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create 2 devices
        Device deviceA = new Device(uuid);
        deviceA.setHeight(400);
        deviceA.setWidth(400);
        Device deviceB = new Device(uuid);
        deviceB.setHeight(300);
        deviceB.setWidth(300);

        //Create a MapMessage object for message payload
        MapMessage devices = new MapMessage();
        devices.setType("devices");
        byte[] payload = new ObjectMapper().writeValueAsBytes(devices);

        //Now we will mock the session service to return a session
        Session session = new Session();
        session.setUuid(sessionUuid);
        session.addDevice(deviceA);
        session.addDevice(deviceB);
        when(this.sessionService.get(Mockito.any(String.class))).thenReturn(session);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", sessionUuid), replyHeaders.getDestination());
        //Make sure the devices are in the payload
        new JsonPathExpectationsHelper("devices").exists(reply.getPayload().toString());
        new JsonPathExpectationsHelper("devices").assertValueIsArray(reply.getPayload().toString());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "devices");
        new JsonPathExpectationsHelper("$devices.[0].uuid").assertValue(reply.getPayload().toString(), deviceA.getUuid().toString());
        new JsonPathExpectationsHelper("$devices.[1].uuid").assertValue(reply.getPayload().toString(), deviceB.getUuid().toString());
    }

    @Test
    public void start() throws Exception {
        UUID sessionUuid = UUID.randomUUID();

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(String.format("/app/%s", sessionUuid));

        // test were complaining about no session id and session attributes
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create a device
        Device device = new Device(uuid);
        device.setHeight(300);
        device.setWidth(300);

        //Create a BeanMessage object for message payload
        BeanMessage start = new BeanMessage();
        start.setType("start");
        byte[] payload = new ObjectMapper().writeValueAsBytes(start);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Now we will mock the session service to return a session
        Session session = new Session();
        session.setUuid(sessionUuid);
        session.addDevice(device);
        when(this.sessionService.get(Mockito.any(String.class))).thenReturn(session);

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", sessionUuid), replyHeaders.getDestination());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "start");

    }

    @Test
    public void stop() throws Exception {
        UUID sessionUuid = UUID.randomUUID();

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(String.format("/app/%s", sessionUuid));

        // test were complaining about no session id and session attributes
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create a device
        Device device = new Device(uuid);
        device.setHeight(300);
        device.setWidth(300);

        //Create a Stop object for message payload
        BeanMessage stop = new BeanMessage();
        stop.setType("stop");
        byte[] payload = new ObjectMapper().writeValueAsBytes(stop);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Now we will mock the session service to return a session
        Session session = new Session();
        session.setUuid(sessionUuid);
        session.addDevice(device);
        when(this.sessionService.get(Mockito.any(String.class))).thenReturn(session);

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", sessionUuid), replyHeaders.getDestination());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "stop");

    }

    @Test
    public void sync() throws Exception {
        //Create a SyncMessage object for message payload
        SyncMessage sync = new SyncMessage();
        sync.setType("sync");
        byte[] payload = new ObjectMapper().writeValueAsBytes(sync);

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/sync");
        headers.setSessionId("0");

        // test were complaining about no session id and session attributes
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);


        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", uuid), replyHeaders.getDestination());
        //Make sure the server time is in the payload
        new JsonPathExpectationsHelper("serverTime").exists(reply.getPayload().toString());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "sync");
    }

    @Test
    public void update() throws Exception {
        UUID sessionUuid = UUID.randomUUID();

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(String.format("/app/%s", sessionUuid));

        // test were complaining about no session id and session attributes
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create an BeanMessageobject for message payload
        MapMessage update = new MapMessage();
        update.setType("update");
        update.setData("THIS IS AN UPDATE");
        byte[] payload = new ObjectMapper().writeValueAsBytes(update);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", sessionUuid), replyHeaders.getDestination());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "update");
        new JsonPathExpectationsHelper("data").assertValue(reply.getPayload().toString(), "THIS IS AN UPDATE");
    }

    @Test
    public void data() throws Exception {
        UUID sessionUuid = UUID.randomUUID();

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(String.format("/app/%s", sessionUuid));

        // test were complaining about no session id and session attributes
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Create a MapMessage object for message payload
        MapMessage data = new MapMessage();
        data.setType("data");
        data.setData("THIS IS SOME DATA");
        byte[] payload = new ObjectMapper().writeValueAsBytes(data);

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have a broker message and get it
        assertEquals(1, this.brokerTemplateChannel.getMessages().size());
        Message<?> reply = this.brokerTemplateChannel.getMessages().get(0);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", sessionUuid), replyHeaders.getDestination());
        new JsonPathExpectationsHelper("type").assertValue(reply.getPayload().toString(), "data");
        new JsonPathExpectationsHelper("data").assertValue(reply.getPayload().toString(), "THIS IS SOME DATA");
    }

    @Test
    public void ping() throws Exception {
        //Create a BeanMessage object for message payload
        BeanMessage ping = new BeanMessage();
        ping.setType("ping");
        byte[] payload = new ObjectMapper().writeValueAsBytes(ping);

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/ping");
        headers.setSessionId("0");

        // test were complaining about no session id and session attributes
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have no broker message
        assertEquals(0, this.brokerTemplateChannel.getMessages().size());
        //Assert that the device was added to the healthy device registry
        assertTrue(this.deviceRegistry.isHealthyDevice(uuid));
    }

    @Test
    public void disconnect() throws Exception {

        //Create a BeanMessage object for message payload
        BeanMessage disconnect = new BeanMessage();
        disconnect.setType("disconnect");
        byte[] payload = new ObjectMapper().writeValueAsBytes(disconnect);

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination("/app/disconnect");
        headers.setSessionId("0");

        // test were complaining about no session id and session attributes
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a random user
        String uuid = UUID.randomUUID().toString();
        headers.setUser(new TestPrincipal(uuid));

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();

        //Call the annotated function on the controller
        this.annotationMethodMessageHandler.handleMessage(message);

        //Assert we have no broker message
        assertEquals(0, this.brokerTemplateChannel.getMessages().size());
        //Assert that the device was added to the healthy device registry
        Mockito.verify(this.sessionService, Mockito.times(1)).deleteDeviceFromSessionByDeviceUuid(uuid);
    }

    /**
     * An extension of SimpAnnotationMethodMessageHandler that exposes a (public)
     * method for manually registering a controller, rather than having it
     * auto-discovered in the Spring ApplicationContext.
     */
    private static class TestSimpAnnotationMethodMessageHandler extends SimpAnnotationMethodMessageHandler {

        public TestSimpAnnotationMethodMessageHandler(SubscribableChannel clientInboundChannel,
                                                      MessageChannel clientOutboundChannel, SimpMessageSendingOperations brokerTemplate) {

            super(clientInboundChannel, clientOutboundChannel, brokerTemplate);
        }

        public void registerHandler(Object handler) {
            super.detectHandlerMethods(handler);
        }

    }

}
