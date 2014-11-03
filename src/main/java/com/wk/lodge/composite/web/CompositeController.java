
package com.wk.lodge.composite.web;

import com.google.gson.Gson;
import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.registry.DeviceRegistry;
import com.wk.lodge.composite.service.SessionService;
import com.wk.lodge.composite.web.socket.message.inbound.JoinMessage;
import com.wk.lodge.composite.web.socket.message.inbound.PairMessage;
import com.wk.lodge.composite.web.socket.message.inbound.SyncMessage;
import com.wk.lodge.composite.web.socket.message.outbound.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Controller
public class CompositeController {
    private static final Log logger = LogFactory.getLog(CompositeController.class);

    private SimpMessagingTemplate template;
    private final SessionService sessionService;
    private final DeviceRegistry deviceRegistry;
    private final Gson gson;

    @Autowired
    public CompositeController(SessionService sessionService,
                               SimpMessagingTemplate template, DeviceRegistry deviceRegistry){
        this.sessionService = sessionService;
        this.template = template;
        this.deviceRegistry = deviceRegistry;
        this.gson = new Gson();
    }

    /**
     * direct message handler for an init message sent by a device. The init message will follow directly after the
     * device has made a successful socket connection to the server, and indicates that the device would like to start
     * interacting with Composite.
     *
     * @param   principal   the device principal that sent the init request
     * @return              stringified InitResponse
     *
     * @see     InitResponse
     */
    @MessageMapping("/init")
    @SendToUser("/queue/device")
    public String init(Principal principal) {
        this.deviceRegistry.addHealthyDevice(principal.getName());
        String initResponse = gson.toJson(new InitResponse(principal.getName()), InitResponse.class);
        if(logger.isDebugEnabled())
            logger.debug(String.format("INIT RESPONSE: %s", initResponse));
        return initResponse;
    }

    /**
     * direct message handler that receives a join message from a device. When a device joins, a session is attempted to
     * be found within a tolerated geo-proximity. if one is found, the device is added to it and returned, if not, a
     * new session is created.
     *
     * @param   j   a stringified JoinMessage sent from the device seeking to join a session
     * @return      stringified JoinResponse
     *
     * @see     JoinMessage
     * @see     JoinResponse
     */
    @MessageMapping("/join")
    @SendToUser("/queue/device")
    public String join(JoinMessage j) {
        String joinType = j.getType().toString();
        switch(joinType){
            case "enter":
                break;
            case "exit":
                Session session = (Session) this.sessionService.get(j);
                String uuid = session.getUuid().toString();
                //Return join
                String joinResponse = gson.toJson(new JoinResponse(session.getApplicationId(), uuid, session.getDevices(), session.getRoom()), JoinResponse.class);
                if(logger.isDebugEnabled())
                    logger.debug(String.format("JOIN RESPONSE: %s", joinResponse));
                return joinResponse;
            default:
                logger.error(String.format("Detected SyncType %s not supported", joinType));
        }
        return null;
    }

    /**
     * Direct message handler that receives pair message from a device. When device joins, session is found or a new
     * pair session is created.
     *
     * @param p     Stringified PairMessage sent from device seeking to pair up with another device
     * @return      Stringified PairResponse
     *
     * @see         PairResponse
     * @see         PairMessage
     */
    @MessageMapping("/pair")
    @SendToUser("/queue/device")
    public String pair(PairMessage p){
        String pairType = p.getType().toString();
        switch(pairType){
            case "enter":
                break;
            case "exit":
                Session session = this.sessionService.pair(p);
                String uuid = session.getUuid().toString();
                // Return pair
                String pairResponse = gson.toJson(new PairResponse(session.getApplicationId(), uuid,session.getDevices()),PairResponse.class);
                if(logger.isDebugEnabled()){
                    logger.debug(String.format("PAIR RESPONSE: %s", pairResponse));
                }
                return pairResponse;
            default:
                logger.error(String.format("Detected SyncType %s not supported", pairType));
        }
        return null;
    }

    /**
     * direct message handler for assisting connected devices in calculating the latency between when messages are sent
     * by the server and when they are received by the client. Each client should hit this endpoint a number of times
     * just after the initial connect response is received, and will calculate an average latency time based off of
     * the server responses
     *
     * @param   s   a stringified SyncMessage sent by the client
     * @return      a stringified SyncResponse
     *
     * @see     SyncMessage
     * @see     SyncResponse
     */
    @MessageMapping("/sync")
    @SendToUser("/queue/device")
    public String sync(SyncMessage s) {
        String syncResponse = gson.toJson(new SyncResponse(s.getTime()), SyncResponse.class);
        if(logger.isDebugEnabled())
            logger.debug(String.format("SYNC RESPONSE: %s", syncResponse));
        return syncResponse;
    }

    /**
     * handles inbound messages from session devices that are sent to the session topic channel. Uses handlers listed
     * below to determine what information to broadcast back across the session topic
     *
     * @param   principal       The device principal sending the inbound message
     * @param   id              the id of the session and topic
     * @param   obj             map of message objects sent to the topic, including what type of message is being sent
     * @return                  string response to topic corresponding to the type of message that was sent
     *
     * @see     private methods below: update, data, start, stop, devices
     */
    @MessageMapping("/{id}")
    public String multiplex(Principal principal, @DestinationVariable String id, Map<String, Object> obj) {
        String type = (String) obj.get("type");
        switch(type) {
            case "update": return this.update(id, obj);
            case "data": return this.data(id, obj);
            case "start": return this.start(principal, id);
            case "stop": return this.stop(principal, id);
            case "devices": return this.devices(id);
            default : return null;
        }
    }

    /**
     * multiplex handler that broadcasts an update response back to the session when a device principal sends an update
     *
     * @param   id      the uuid of the device that sent the update message
     * @param   update  map of update objects sent by the device
     * @return          stringified UpdateResponse
     *
     * @see     UpdateResponse
     */
    private String update(String id, Map<String, Object> update) {
        if(validUUID(id)) {
            String updateResponse = gson.toJson(new UpdateResponse(update.get("data")), UpdateResponse.class);
            if(logger.isDebugEnabled())
                logger.debug(String.format("UPDATE RESPONSE: %s", updateResponse));
            return updateResponse;
        }
        return null;
    }

    /**
     * multiplex handler that broadcasts a data response back to the session when a device principal sends a data message
     *
     * @param   id      the uuid of the device that sent the data message
     * @param   data    map of data objects that the device sent and that should be sent back to the session topic
     * @return          stringified DataResponse
     *
     * @see     DataResponse
     */
    private String data(String id, Map<String, Object> data) {
        if(validUUID(id)) {
            String dataResponse = gson.toJson(new DataResponse(data.get("data")), DataResponse.class);
            if(logger.isDebugEnabled())
                logger.debug(String.format("DATA RESPONSE: %s", dataResponse));
            return dataResponse;
        }
        return null;
    }

    /**
     * multiplex handler that broadcasts a start event back to the session when a device principal initiates a start event
     *
     * @param   principal   the device principal that initiated the start event
     * @param   id          the id of the session that should be started
     * @return              stringifed StartResponse
     *
     * @see     StartResponse
     */
    private String start(Principal principal, String id) {
        if(validUUID(id)) {
            Session session = (Session) this.sessionService.get(id);
            if(session != null) {
                session.setLocked(true);
                this.sessionService.update(session);
                String startResponse = gson.toJson(new StartResponse(), StartResponse.class);
                if(logger.isDebugEnabled())
                    logger.debug(String.format("START RESPONSE: %s", startResponse));
                return startResponse;
            }
        }
        return null;
    }

    /**
     * multiplex handler that broadcasts a stop event back to the session when a device principal initiates a stop event
     *
     * @param   principal   the device principal that initiated the stop event
     * @param   id          the id of the session that should be stopped
     * @return              stringified StopResponse
     *
     * @see     StopResponse
     */
    private String stop(Principal principal, String id) {
        if(validUUID(id)) {
            Session session = (Session) this.sessionService.get(id);
            if(session != null) {
                session.setLocked(false);
                session.setSessionEnded(new Date().getTime());
                this.sessionService.update(session);
                String stopResponse = gson.toJson(new StopResponse(), StopResponse.class);
                if(logger.isDebugEnabled())
                    logger.debug(String.format("STOP RESPONSE: %s", stopResponse));
                return stopResponse;
            }
        }
        return null;
    }

    /**
     * multiplex handler that broadcasts a list of devices found in a session back to the session topic
     *
     * @param   id      the id of the session for which a devices broadcast should be sent
     * @return          stringified DevicesResponse object containing the session's devices
     *
     * @see     DevicesResponse
     */
    private String devices(String id) {
        if(validUUID(id)) {
            Session session = (Session) this.sessionService.get(id);
            //Broadcast updated device list
            if(session != null) {
                if (session.getDevices().size() > 1) {
                    String devicesResponse = gson.toJson(new DevicesResponse(session.getDevices()), DevicesResponse.class);
                    if (logger.isDebugEnabled())
                        logger.debug(String.format("DEVICES RESPONSE: %s", devicesResponse));
                    return devicesResponse;
                }
            }
        }
        return null;
    }

    /**
     * message handler that receives a ping from a connected client device, and in turn adds the device to a list of
     * "healthy" session devices, thereby preventing it from being automatically deleted from the session. If a device
     * fails to ping the server within a specified timeframe, it will be marked as unhealthy, and subsequently deleted.
     *
     * @param   principal   the device principal pinging the server
     */
    @MessageMapping("/ping")
    public void ping(Principal principal) {
        this.deviceRegistry.addHealthyDevice(principal.getName());
    }

    /**
     * handles disconnect messages sent by a client device by removing the device from its associated session.
     * if there are still devices in its session, they are notified of the disconnect, otherwise, the session is removed.
     *
     * @param   principal   the device principal that has disconnected
     */
    @MessageMapping("/disconnect")
    public void disconnect(Principal principal) {
        this.sessionService.deleteDeviceFromSessionByDeviceUuid(principal.getName());
    }

    /**
     * direct-message handler to send messaging errors to a device in session
     *
     * @param   exception   the exception that was thrown by the messaging error
     * @return              String, the message sent back to the device containing the error
     */
    @MessageExceptionHandler
    @SendToUser("/queue/errors")
    public String handleException(Throwable exception) {
        logger.error("Handling exception: ", exception);
        return exception.getMessage();
    }

    /**
     * Returns a boolean value that identifies if a UUID sent from a client device is actually a UUID. Method included
     * as a means of protection against spamming or otherwise invalid attempts to get messages from the service.
     *
     * @param   uuid    the UUID to be checked for validity
     * @return          boolean, whether the UUID is valid
     */
    private Boolean validUUID(String uuid) {
        try {
            UUID.fromString(uuid);
        } catch (Exception e) {
            return false;
        }
        return true;
    }
}
