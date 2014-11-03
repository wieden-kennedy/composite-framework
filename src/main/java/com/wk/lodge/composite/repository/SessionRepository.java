package com.wk.lodge.composite.repository;

import com.google.gson.Gson;
import com.javadocmd.simplelatlng.LatLng;
import com.javadocmd.simplelatlng.LatLngTool;
import com.javadocmd.simplelatlng.util.LengthUnit;
import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.service.RoomService;
import com.wk.lodge.composite.web.socket.message.inbound.JoinMessage;
import com.wk.lodge.composite.web.socket.message.inbound.PairMessage;
import com.wk.lodge.composite.web.socket.message.outbound.DisconnectResponse;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.lightcouch.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.messaging.simp.broker.BrokerAvailabilityEvent;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;

@Repository
public class SessionRepository implements ApplicationListener<BrokerAvailabilityEvent> {

    private static final Log logger = LogFactory.getLog(SessionRepository.class);
    private Random random;
    private Gson gson;
    private DeviceLimiter deviceLimiter;
    private RoomService roomService;
    private CouchDbClient couchDbSessionClient;
    private MessageSendingOperations<String> messagingTemplate;
    private AtomicBoolean brokerAvailable;

    @Autowired
    public SessionRepository(
        CouchDbClient dbClient,
        DeviceLimiter dl,
        RoomService rr,
        MessageSendingOperations<String> messagingTemplate
    ){
        this.couchDbSessionClient = dbClient;
        this.deviceLimiter = dl;
        this.roomService = rr;
        this.messagingTemplate = messagingTemplate;
        this.random = new Random();
        this.gson = new Gson();
        this.brokerAvailable = new AtomicBoolean();
    }

    @Override
    public void onApplicationEvent(BrokerAvailabilityEvent brokerAvailabilityEvent) {
        this.brokerAvailable.set(brokerAvailabilityEvent.isBrokerAvailable());
    }

    private String assignRoom(String applicationId){
        String[] roomNames = roomService.getRoomNamesForApplication(applicationId);
        if(roomNames != null){
            return roomNames[random.nextInt(roomNames.length)];
        }
        else { return null; }
    }

    private List<Session> getUnlockedSessionsByApplicationId(String applicationId){
        try{
            List<Session> sessions = this.couchDbSessionClient.view("app/application-id")
                .key(applicationId)
                .includeDocs(true)
                .query(Session.class);

            if(!sessions.isEmpty()){
                return sessions;
            }
        }
        catch(NoDocumentException | IndexOutOfBoundsException e){
            logger.error(String.format("Sessions not found. Error message: %s", e.getMessage()));
        }

        return new ArrayList<Session>();

    }

    /**
     * Persists a new session object to the CouchDB document store
     *
     * @param  session The session to be persisted to the document store
     */
    public Session create(Session session)  {
        try{
            if(session.getInserted() == 0){
                session.setInserted(new Date().getTime());
                session.setUpdated(session.getInserted());
            }
            Response res = this.couchDbSessionClient.save(session);
            session.set_id(res.getId());
            session.set_rev(res.getRev());
            return session;
        }
        catch(DocumentConflictException conflictException){
            logger.error(String.format("Error saving session to document store: %s", conflictException.getMessage()));
        }

        return null;
    }

    /**
     * Based on a join message's geo location, creates a new session or finds an existing session within a tolerated
     * geo-proximity from the new device that has just attempted to join a session.
     *
     * @param   o       A generic object in the form of a JoinMessage object that has just attempted to join a session
     * @return          Session A session that is either new or existing
     */
    public Session getOrCreate(Object o)  {
        JoinMessage j = (JoinMessage)o;
        Session session = null;
        //List<Session> unlockedSessions = this.getSessionsByLocked(false);
        List<Session> unlockedSessions = this.getUnlockedSessionsByApplicationId(j.getApplicationId());

        // if unlocked sessions is not null and has members, try to find a session within the minimum threshold distance from
        // the device. if a session is found, return it. if not, try to find a session within the maximum threshold distance
        // from the device. if a session is found there, return it.
        if(unlockedSessions != null){
            if(!unlockedSessions.isEmpty()){
                session = getSessionInRange(unlockedSessions, deviceLimiter.getMinDistanceThresholdBetweenDevices(),j);
                if(session == null){
                    session = getSessionInRange(unlockedSessions, deviceLimiter.getMaxDistanceThresholdBetweenDevices(),j);
                }
            }
        }

        // if a session was found in either the min threshold or max threshold distance, update it with the new device information
        if(session != null) {
            if(session.deviceInSession(j.getDevice().getUuid())) {
                return session;
            } else if(session.getDevices().size() < deviceLimiter.getMaxDevicesPerSessionForGame(j.getApplicationId())){
                session.setGeoLocation(j.getGeo());
                session.addDevice(j.getDevice());

                try {
                    return update(session);
                } catch (DocumentConflictException documentConflictException) {
                    //No op - fall through to a new session
                }
            }
        }

        // if no sessions found that are in proximity to new device, create a new session and
        // update it accordingly before adding.
        session = new Session();
        session.setApplicationId(j.getApplicationId());
        session.setRoom(assignRoom(j.getApplicationId()));
        session.setGeoLocation(j.getGeo());
        session.setInserted(new Date().getTime());
        session.setUpdated(session.getInserted());
        session.addDevice(j.getDevice());

        return this.create(session);
    }

    /**
     * Returns a Session from the document store if a session within the range, in meters, exists. Range is determined
     * by calculating the distance between two geopoints, the geopoint of the incoming device, and the geopoint of an
     * existing Session
     *
     * @param   sessions    a list of unlocked/available sessions to check against
     * @param   range       the maximum range from a session for the device to be considered for inclusion
     * @param   j           the JoinMessage object sent by the device that includes its geolocation
     *
     * @return              a session within the desired range, if found
     */
    public Session getSessionInRange(List<Session> sessions, float range, JoinMessage j){
        // iterate over the list of sessions and try to find one within proximity of the
        // inbound device. If one is found, create the device to it, and update its Geolocation with the device's
        // geolocation. This way when the next device comes in it will only need to be within proximity
        // to the last device to join the session to be allowed into session
        float[] deviceGeo= j.getGeo();
        LatLng dGeo = new LatLng(new Double(deviceGeo[0]), new Double(deviceGeo[1]));

        for(Session session: sessions){
            float[] sGeo = session.getGeoLocation();
            if(sGeo != null){
                LatLng sessionGeo =new LatLng(new Double(sGeo[0]), new Double(sGeo[1]));
                double distanceFromInboundDevice = LatLngTool.distance(dGeo,sessionGeo, LengthUnit.METER);

                if(distanceFromInboundDevice <= range){
                    return session;
                }
            }
        }
        return null;
    }

    /**
     * Finds a session by its UUID
     *
     * @param   uuid    a string representation of a session's UUID
     * @return          a session, if found, which has a matching UUID
     */
    public Session find(String uuid){
        try{
            List<Session> sessions = this.couchDbSessionClient.view("app/uuid")
                .key(uuid)
                .limit(1)
                .includeDocs(true)
                .query(Session.class);

            if(!sessions.isEmpty()){
                return sessions.get(0);
            }
        }
        catch(NoDocumentException noDocumentException){
            logger.error(String.format("Design/View or document not found in document store. Error message: \n%s", noDocumentException.getMessage()));
        }
        catch(IndexOutOfBoundsException indexOutOfBoundsException){
            logger.error(String.format("Session not found in document store. Error message: %s",
                indexOutOfBoundsException.getMessage()));
        }

        return null;
    }

    /**
     * Finds and returns stale sessions, i.e., sessions which have existed for longer than the maximum session lifespan.
     *
     * @param   staleSessionThresholdMinutes    int, number of minutes at which point a session is considered stale
     * @returns                                 List of session objects that are considered stale
     */
    public List<Session> findStale(int staleSessionThresholdMinutes){
        long staleSessionThresholdMilliseconds=60000*staleSessionThresholdMinutes;
        long nowMilliseconds = new Date().getTime();
        long staleSessionCutoff = nowMilliseconds - staleSessionThresholdMilliseconds;

        try{
            List<Session> sessions = this.couchDbSessionClient.view("app/session-by-timestamp")
                .endKey(staleSessionCutoff)
                .includeDocs(true)
                .query(Session.class);

            if(!sessions.isEmpty()){
                return sessions;
            }
        }
        catch(NoDocumentException noDocumentException){
            logger.info("Tried to fetch stale sessions, but none were present.");
        }
        catch(IndexOutOfBoundsException indexOutOfBoundsException){
            logger.error(String.format("Session not found in document store. Error message: %s", indexOutOfBoundsException.getMessage()));
        }

        return null;
    }

    /**
     * Finds a session by the UUID of a device that is in the session
     *
     * @param   uuid    String representation of UUID for a device that is assumed to be in a session
     * @return          the session containing the device UUID
     */
    public Session findByDeviceUuid(String uuid){
        try{
            List<Session> sessions = this.couchDbSessionClient.view("app/session-by-device")
                .key(uuid)
                .limit(1)
                .includeDocs(true)
                .query(Session.class);

            if(!sessions.isEmpty()){
                return sessions.get(0);
            }
        }
        catch(NoDocumentException noDocumentException){
            logger.error(String.format("Design/View or document not found in document store. Error message: %s", noDocumentException.getMessage()));
        }
        catch(IndexOutOfBoundsException indexOutOfBoundsException){
            logger.error(String.format("Session not found in document store. Error message: %s", indexOutOfBoundsException.getMessage()));
        }

        return null;
    }

    /**
     * Returns a list of all sessions currently in the CouchDB document store
     *
     * @return      List of all sessions in the document store
     */
    public List<Session> getAll() {
        try{
            List<Session> sessions = this.couchDbSessionClient.view("app/uuid")
                .includeDocs(true)
                .query(Session.class);

            if(!sessions.isEmpty()){
                return sessions;
            }
        }
        catch(NoDocumentException noDocumentException){
            logger.error("Design/View or document not found in document store");
        }

        return null;
    }

    /**
     * Updates a session
     *
     * @param   session the session to be updated
     * @return          the session once it's been updated
     */
    public Session update(Session session)  {
        try {
            session.setUpdated(new Date().getTime());
            Response res = this.couchDbSessionClient.update(session);
            session.set_rev(res.getRev());
            return session;
        }
        catch(DocumentConflictException conflictException) {
            logger.error(String.format("Error updating the session document: %s", conflictException.getMessage()));
            throw conflictException;
        }
    }

    /**
     * Updates a set of session objects using the CouchDB bulk update API
     *
     * @param   sessions    List of sessions to update
     * @return              boolean, whether the update completed successfully
     */
    public boolean bulkUpdate(List<Session> sessions) {
        try{
            this.couchDbSessionClient.bulk(sessions, true);
            System.out.println(String.format("%d sessions marked for deletion", sessions.size()));
            return true;
        }
        catch(CouchDbException dbException){

            logger.error(String.format("Bulk update failed with the following exception: %s", dbException.getMessage()));
        }

        return false;
    }

    /**
     * Deletes a session object from the CouchDB document store by UUID
     *
     * @param   uuid    String representation of the session's UUID
     * @return          boolean, whether the delete op completed successfully
     */
    public boolean delete(String uuid)  {
        Session session = this.find(uuid);
        if(session != null){
            try{
                this.couchDbSessionClient.remove(session);
                return true;
            }
            catch(DocumentConflictException conflictException){
                logger.error(String.format("Error deleting the session from the document store: %s",
                    conflictException.getMessage()));
                throw conflictException;
            }
        }

        return false;
    }

    /**
     * Removes a device from a session using the device's UUID for identification. If a device is removed and the session
     * contains more devices, a device removal message is broadcasted to the remaining devices in session. If the device
     * being removed is the last device in the session, the session is deleted from the CouchDB document store.
     *
     * @param   uuid    String representation of the device UUID that should be removed
     * @return          boolean, whether the removal completed successfully
     */
    public boolean removeDeviceFromSessionByDeviceUuid(String uuid) {
        boolean noConflict = true;
        Session session = this.findByDeviceUuid(uuid);
        if(session != null) {
            session.removeDeviceByUuid(uuid);

            // if there are still devices in the array, update the session and notify the remaining devices
            if(!session.getDevices().isEmpty()){
                try{
                    this.update(session);
                }
                catch(DocumentConflictException documentConflictException){
                    noConflict = false;
                }
                if(this.brokerAvailable.get()){
                    String disconnectResponse = gson.toJson(new DisconnectResponse(session.getDevices()), DisconnectResponse.class);
                    this.messagingTemplate.convertAndSend(String.format("/topic/%s", session.getUuid().toString()), disconnectResponse);
                    if(logger.isDebugEnabled()) {
                        logger.debug(disconnectResponse);
                    }
                }
            }
            // if the session devices array is empty, attempt to delete the session
            else {
                try{
                    this.delete(session.getUuid().toString());
                }
                catch(DocumentConflictException documentConflictException){
                    // if session cant' be deleted, just pass and wait for the cleanup task to pick it up
                }
            }
        }

        // if session is null, device was not present, so return true
        return noConflict;
    }

    /**
     * Finds all un/locked sessions based on a boolean value. A session is locked once all devices in the session have
     * joined, and the first device in the session initiates a start command. If a session has not received a start
     * command, or if devices are still joining, it will be unlocked.
     *
     * @param   isLocked    boolean, whether the session is locked
     * @return              list of sessions that whose lock status corresponds to the parameter passed
     */
    public List<Session> getSessionsByLocked(boolean isLocked) {
        try{
            List<Session> sessions = this.couchDbSessionClient.view("app/locked-sessions")
                .key(isLocked)
                .includeDocs(true)
                .query(Session.class);

            if(!sessions.isEmpty()){
                return sessions;
            }
        }
        catch(NoDocumentException noDocumentException){
            logger.error(String.format("Design/View or document not found in document store. Error message: %s", noDocumentException.getMessage()));
        }
        catch(IndexOutOfBoundsException indexOutOfBoundsException){
            logger.error(String.format("Locked Sessions not found in document store. Error message: %s",
                indexOutOfBoundsException.getMessage()));
        }

        return null;
    }

    /**
     * Finds a random open pair session by app id. If no open pairing sessions are open than a new pairing session
     * is created and returned.
     *
     * @param   p   PairMessage
     * @return      Session
     */
    public Session pair(PairMessage p) {
        try{
            List<Session> sessions = this.getUnlockedSessionsByApplicationId(p.getApplicationId());
            // open sessions for pairing found
            if(!sessions.isEmpty()){
                // pick a random session from the collection
                Session pairedSession = sessions.get(0);
                pairedSession.addDevice(p.getDevice());
                pairedSession.setLocked(true);
                return this.update(pairedSession);
            }
        }
        catch(NoDocumentException | IndexOutOfBoundsException e){
            logger.error(String.format("Session not found. Error message: %s",e.getMessage()));
        }

        // if no sessions were found, create a new one
        Session pairingSession = new Session();
        pairingSession.setApplicationId(p.getApplicationId());
        pairingSession.setGeoLocation(p.getGeo());
        pairingSession.setInserted(new Date().getTime());
        pairingSession.setUpdated(pairingSession.getInserted());
        pairingSession.addDevice(p.getDevice());
        return this.create(pairingSession);
    }

}
