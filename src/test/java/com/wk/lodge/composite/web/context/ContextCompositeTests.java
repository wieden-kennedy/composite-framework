
package com.wk.lodge.composite.web.context;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.wk.lodge.composite.config.TestRootConfig;
import com.wk.lodge.composite.config.TestWebSocketConfig;
import com.wk.lodge.composite.model.Device;
import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.registry.DeviceRegistry;
import com.wk.lodge.composite.repository.SessionRepository;
import com.wk.lodge.composite.service.SessionService;
import com.wk.lodge.composite.web.socket.message.inbound.BeanMessage;
import com.wk.lodge.composite.web.socket.message.inbound.JoinMessage;
import com.wk.lodge.composite.web.socket.message.inbound.MapMessage;
import com.wk.lodge.composite.web.socket.message.inbound.PairMessage;
import com.wk.lodge.composite.web.support.TestChannelInterceptor;
import com.wk.lodge.composite.web.support.TestPrincipal;
import org.apache.commons.lang.StringEscapeUtils;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.AbstractSubscribableChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.JsonPathExpectationsHelper;

import java.util.HashMap;
import java.util.UUID;

import static org.junit.Assert.*;

//import com.wk.lodge.composite.config.RootConfig;

/**
 * Tests for CompositeController that rely on the Spring TestContext framework to
 * load the actual Spring configuration.
 *
 * Tests can create a Spring {@link org.springframework.messaging.Message} that
 * represents a STOMP frame and send it directly to the "clientInboundChannel"
 * for processing. In effect this bypasses the phase where a WebSocket message
 * is received and parsed. Instead tests must set the session id and user
 * headers of the Message.
 *
 * Test ChannelInterceptor implementations are installed on the "brokerChannel"
 * and the "clientOutboundChannel" in order to detect messages sent through
 * them. Although not the case here, often a controller method will
 * not send any messages at all. In such cases it might be necessary to inject
 * the controller with "mock" services in order to assert the outcome.
 *
 * Note the (optional) use of TestConfig, which removes MessageHandler
 * subscriptions to MessageChannel's for all handlers found in the
 * ApplicationContext except the one for the one delegating to annotated message
 * handlers. This is done to reduce additional processing and additional
 * messages not related to the test.
 *
 * The test strategy here is to test the behavior of controllers using the
 * actual Spring configuration while using the TestContext framework ensures
 * that Spring configuration is loaded only once per test class. This strategy
 * is not an end-to-end test and does not replace the need for full-on
 * integration testing -- much like we can write integration tests for the
 * persistence layer, tests here ensure the web layer (including controllers
 * and Spring configuration) are well tested using tests that are a little
 * simpler and easier to write and debug than full, end-to-end integration tests.
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestWebSocketConfig.class, TestRootConfig.class })
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class ContextCompositeTests {

    @Autowired
    private AbstractSubscribableChannel clientInboundChannel;

    @Autowired
    private AbstractSubscribableChannel clientOutboundChannel;

    @Autowired
    private AbstractSubscribableChannel brokerChannel;

    @Autowired
    private DeviceRegistry deviceRegistry;

    @Autowired
    private SessionService sessionService;

    @Autowired
    private SessionRepository sessionRepository;

    private TestChannelInterceptor clientOutboundChannelInterceptor;

    private TestChannelInterceptor brokerChannelInterceptor;

    private static final String uuid = UUID.randomUUID().toString();
    private static final String sessionUuid = UUID.randomUUID().toString();

    @Before
    public void setUp() throws Exception {

        this.brokerChannelInterceptor = new TestChannelInterceptor(false);
        this.clientOutboundChannelInterceptor = new TestChannelInterceptor(false);

        this.brokerChannel.addInterceptor(this.brokerChannelInterceptor);
        this.clientOutboundChannel.addInterceptor(this.clientOutboundChannelInterceptor);
    }

    @Test
    public void test01Init() throws Exception {

        //Build the message (with a payload and headers)
        Message<byte[]> message = this.createMessage("init", "/app/init");

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);



        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(reply);

        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure its the same session
        //assertEquals("0", replyHeaders.getSessionId());
        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", this.uuid), replyHeaders.getDestination());
        //get the payload as string
        String json = new String((byte[]) reply.getPayload(), "UTF-8");
        new JsonPathExpectationsHelper("uuid").assertValue(json, this.uuid);
        new JsonPathExpectationsHelper("type").assertValue(json, "init");

    }

    @Test
    public void test02Sync() throws Exception {
        //Build the message (with a payload and headers)
        Message<byte[]> message = this.createMessage("sync", "/app/sync");

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(reply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure its the same session
        //assertEquals("0", replyHeaders.getSessionId());
        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", this.uuid), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = new String((byte[]) reply.getPayload(), "UTF-8");
        //json = json.substring(1, json.length()-1);
        //Make sure the server time is in the payload
        new JsonPathExpectationsHelper("serverTime").exists(json);
        new JsonPathExpectationsHelper("type").assertValue(json, "sync");
    }


    @Test
    public void test03Join() throws Exception {
        //Build the message (with a payload and headers)
        Message<byte[]> message = createMatchMessage("join", "/app/join", "0", UUID.fromString(this.uuid));

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> joinReply = this.brokerChannelInterceptor.awaitMessage(5);
        // Getting back null for joinReply
        assertNotNull(joinReply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(joinReply);
        //Make sure its the same session
        //assertEquals("0", replyHeaders.getSessionId());
        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", this.uuid), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = new String((byte[]) joinReply.getPayload(), "UTF-8");
        //json = json.substring(1, json.length()-1);
        //Make sure the devices are in the payload
        new JsonPathExpectationsHelper("id").exists(json);
        new JsonPathExpectationsHelper("devices").exists(json);
        new JsonPathExpectationsHelper("devices").assertValueIsArray(json);
        new JsonPathExpectationsHelper("type").assertValue(json, "join");
        new JsonPathExpectationsHelper("$devices.[-1:].uuid").assertValue(json, this.uuid);
    }

    @Test
    public void test04Ping() throws Exception {
        //Build the message (with a payload and headers)
        Message<byte[]> message = this.createMessage("ping", "/app/ping");

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        //Ping does not return a response, internal functionality is confirmed in Standalone tests
        assertTrue(reply == null);
        assertNotNull(sessionRepository.findByDeviceUuid(this.uuid));
    }

    @Test
    public void test05Start() throws Exception {
        Session session = this.sessionRepository.findByDeviceUuid(this.uuid);

        //Build the message
        Message<byte[]> message = this.createMessage("start", String.format("/app/%s", session.getUuid()), session.getDevices().get(0).getUuid());

        this.brokerChannelInterceptor.setIncludedDestinations("/topic/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(reply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", session.getUuid()), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = new String((byte[]) reply.getPayload(), "UTF-8");
        //json = json.substring(1, json.length()-1);
        //Verify the data
        new JsonPathExpectationsHelper("type").assertValue(json, "start");
    }

    @Test
    public void test06Update() throws Exception {
        //Build the message
        Message<byte[]> message = this.createMessage("update", String.format("/app/%s", this.sessionUuid), "THIS IS AN UPDATE");

        this.brokerChannelInterceptor.setIncludedDestinations("/topic/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(reply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s",this.sessionUuid), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = new String((byte[]) reply.getPayload(), "UTF-8");
        //json = json.substring(1, json.length()-1);
        //Verify the data
        new JsonPathExpectationsHelper("type").assertValue(json, "update");
        new JsonPathExpectationsHelper("data").assertValue(json, "THIS IS AN UPDATE");
    }

    @Test
    public void test07Data() throws Exception {

        //Build the message
        Message<byte[]> message = this.createMessage("data", String.format("/app/%s",this.sessionUuid), "THIS IS SOME DATA");

        this.brokerChannelInterceptor.setIncludedDestinations("/topic/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(reply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s",this.sessionUuid), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = new String((byte[]) reply.getPayload(), "UTF-8");
        //json = json.substring(1, json.length()-1);
        //Verify the data
        new JsonPathExpectationsHelper("type").assertValue(json, "data");
        new JsonPathExpectationsHelper("data").assertValue(json, "THIS IS SOME DATA");
    }

    @Test
    public void test08Devices() throws Exception {

        //Build the message
        Message<byte[]> message = this.createMessage("devices", String.format("/app/%s",this.sessionUuid));

        this.brokerChannelInterceptor.setIncludedDestinations("/topic/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertTrue(reply == null);
    }

    @Test
    public void test09Stop() throws Exception {
        Session session = this.sessionRepository.findByDeviceUuid(this.uuid);

        //Build the message
        Message<byte[]> message = this.createMessage("stop", String.format("/app/%s", session.getUuid()), session.getDevices().get(0).getUuid());

        this.brokerChannelInterceptor.setIncludedDestinations("/topic/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(reply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(reply);
        //Make sure the message was sent to the correct topic (sessionUuid from above)
        assertEquals(String.format("/topic/%s", session.getUuid()), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = new String((byte[]) reply.getPayload(), "UTF-8");
        //json = json.substring(1, json.length()-1);
        //Verify the data
        new JsonPathExpectationsHelper("type").assertValue(json, "stop");
    }

    @Test
    public void test10Disconnect() throws Exception {
        //Build the message
        Message<byte[]> message = this.createMessage("disconnect", "/app/disconnect");

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> reply = this.brokerChannelInterceptor.awaitMessage(5);
        //Disconnect does not return a response, internal functionality is confirmed in Standalone tests
        assertTrue(reply == null);
        assertTrue(sessionRepository.findByDeviceUuid(this.uuid) == null);
    }

    @Test
    public void test11Pair() throws Exception {
        UUID user = UUID.randomUUID();

        //Build the message (with a payload and headers)
        Message<byte[]> message = createMatchMessage("pair", "/app/pair", "1", user);

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message);

        Message<?> pairReply = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(pairReply);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders = StompHeaderAccessor.wrap(pairReply);
        //Make sure its the same session
        //assertEquals("0", replyHeaders.getSessionId());
        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", user.toString()), replyHeaders.getDestination());
        //Unescape/unquote the string
        String json = StringEscapeUtils.unescapeJava(new String((byte[]) pairReply.getPayload(), "UTF-8"));
        //json = json.substring(1, json.length()-1);
        //Make sure the devices are in the payload
        new JsonPathExpectationsHelper("id").exists(json);
        new JsonPathExpectationsHelper("devices").exists(json);
        new JsonPathExpectationsHelper("devices").assertValueIsArray(json);
        new JsonPathExpectationsHelper("type").assertValue(json, "pair");
        new JsonPathExpectationsHelper("$devices.[0].uuid").assertValue(json, user.toString());

        Thread.sleep(120);

        UUID user2 = UUID.randomUUID();

        //Build the message (with a payload and headers)
        Message<byte[]> message2 = createMatchMessage("pair", "/app/pair", "1", user2);

        this.brokerChannelInterceptor.setIncludedDestinations("/user/**");
        this.brokerChannelInterceptor.startRecording();

        this.clientInboundChannel.send(message2);

        Message<?> pairReply2 = this.brokerChannelInterceptor.awaitMessage(5);
        assertNotNull(pairReply2);
        //Wrap the response in a header accessor
        StompHeaderAccessor replyHeaders2 = StompHeaderAccessor.wrap(pairReply2);
        //Make sure its the same session
        //assertEquals("0", replyHeaders2.getSessionId());
        //Make sure the message was sent to the correct user (uuid from above)
        assertEquals(String.format("/user/%s/queue/device", user2.toString()), replyHeaders2.getDestination());
        //Unescape/unquote the string
        String json2 = new String((byte[]) pairReply2.getPayload(), "UTF-8");
        //json2 = json2.substring(1, json2.length()-1);
        //Make sure the devices are in the payload
        new JsonPathExpectationsHelper("id").exists(json2);
        new JsonPathExpectationsHelper("devices").exists(json2);
        new JsonPathExpectationsHelper("devices").assertValueIsArray(json2);
        new JsonPathExpectationsHelper("type").assertValue(json2, "pair");
        new JsonPathExpectationsHelper("$devices.[0].uuid").assertValue(json2, user.toString());
        new JsonPathExpectationsHelper("$devices.[1].uuid").assertValue(json2, user2.toString());

        this.clientInboundChannel.send(this.createMessage("disconnect", "/app/disconnect"));
        this.clientInboundChannel.send(this.createMessage("disconnect", "/app/disconnect", user2));
    }



    private Message<byte[]> createMessage(String type, String destination) throws Exception {
        return this.createMessage(type, destination, null, null);
    }

    private Message<byte[]> createMessage(String type, String destination, String data) throws Exception {
        return this.createMessage(type, destination, data, null);
    }

    private Message<byte[]> createMessage(String type, String destination, UUID userId) throws Exception {
        return this.createMessage(type, destination, null, userId);
    }

    private Message<byte[]> createMessage(String type, String destination, String data, UUID userId) throws Exception {
        byte[] payload = null;
        if(data != null) {
            //Create a MapMessage object for message payload
            MapMessage mapMessage = new MapMessage();
            mapMessage.setType(type);
            mapMessage.setData(data);
            payload = new ObjectMapper().writeValueAsBytes(mapMessage);
        } else {
            //Create an BeanMessage object for message payload
            BeanMessage beanMessage = new BeanMessage();
            beanMessage.setType(type);
            payload = new ObjectMapper().writeValueAsBytes(beanMessage);
        }

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(destination);
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a user
        headers.setUser(new TestPrincipal(userId == null ? this.uuid : userId.toString()));

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();
        return message;
    }

    private Message<byte[]> createMatchMessage(String type, String destination, String appId, UUID userId) throws Exception {

        //Create a device
        Device device = new Device(userId == null ? this.uuid : userId.toString());
        device.setHeight(400);
        device.setWidth(400);

        byte[] payload = null;
        switch(type) {
            case "join":
                //Create a JoinMessage object for message payload
                JoinMessage joinMessage = new JoinMessage();
                joinMessage.setDevice(device);
                joinMessage.setGeo(new float[]{0, 0});
                joinMessage.setType(JoinMessage.Types.exit);
                joinMessage.setApplicationId(appId);
                payload = new ObjectMapper().writeValueAsBytes(joinMessage);
                break;
            case "pair":
                //Create a PairMessage object for message payload
                PairMessage pairMessage= new PairMessage();
                pairMessage.setDevice(device);
                pairMessage.setGeo(new float[]{0, 0});
                pairMessage.setType(PairMessage.Types.exit);
                pairMessage.setApplicationId(appId);
                payload = new ObjectMapper().writeValueAsBytes(pairMessage);
                break;
            default:
                return null;
        }

        //Create headers
        StompHeaderAccessor headers = StompHeaderAccessor.create(StompCommand.SEND);
        headers.setDestination(destination);
        headers.setSessionId("0");
        headers.setSessionAttributes(new HashMap<String, Object>());

        //Generate a user
        headers.setUser(new TestPrincipal(userId == null ? this.uuid : userId.toString()));

        //Build the message (with a payload and headers)
        Message<byte[]> message = MessageBuilder.withPayload(payload).setHeaders(headers).build();
        return message;
    }
}
