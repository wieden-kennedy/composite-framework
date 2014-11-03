
package com.wk.lodge.composite.service;

import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.repository.SessionRepository;
import com.wk.lodge.composite.web.socket.message.inbound.PairMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class SessionService {

    private SessionRepository sessionRepository;

    public SessionService(){}

    @Autowired
    public void setSessionRepository(SessionRepository sessionRepository){
        this.sessionRepository = sessionRepository;
    }

    /**
     * Gets a session by its uuid
     *
     * @param   id    String representation of session's UUID
     * @return          Session that matches UUID
     *
     * @see     SessionRepository#find(String)
     */
    public Session get(String id)  {
        return this.sessionRepository.find(id);
    }

    /**
     * gets an existing, or creates a new, session from a device's join message to the service. The join message contains
     * geo information which is what is used to determine if a new session should be created, or if an existing one
     * is within geo proximity to the inbound device.
     *
     * @param   o   the join message sent by the inbound device
     * @return      an existing, or new, session that is within proximity of the inbound device
     *
     * @see     SessionRepository#getOrCreate(Object)
     */
    public Session get(Object o)  {
        return this.sessionRepository.getOrCreate(o);
    }

    /**
     * Gets an existing, or creates a new session for pairing two devices, from a devices pair message to the service.
     * @param p     Pair message
     * @return      The paired Session
     *
     * @see     SessionRepository#pair(PairMessage)
     */
    public Session pair(PairMessage p){
        return this.sessionRepository.pair(p);
    }

    /**
     * updates an existing session with new session information
     *
     * @param   session the session to update
     * @return          the updated session
     *
     * @see     SessionRepository#update(com.wk.lodge.composite.model.Session)
     */
    public Session update(Session session)  {
        return this.sessionRepository.update(session);
    }

    /**
     * deletes sessions marked as stale by warrant of the threshold passed into the method. Sessions that are considered
     * stale are either sessions that are locked and haven't been updated within the threshold in minutes, or are unlocked
     * and have ended prior to the threshold in minutes ago.
     *
     * @param   threshold   int, the threshold in minutes prior to which sessions are considered stale
     * @return              the count of sessions that were deleted
     *
     * @see     SessionRepository#findStale(int)
     * @see     SessionRepository#bulkUpdate(java.util.List)
     */
    public int deleteStale(int threshold)  {
        List<Session> staleSessions = this.sessionRepository.findStale(threshold);
        long thresholdMilliseconds = threshold*60000;
        int deleteCount = 0;
        if(staleSessions != null){
            long now = new Date().getTime();
            for(Session s: staleSessions){
                // if the game is still locked and the last update was more than 5 minutes ago, mark the session for deletion
                if(s.isLocked()){
                    if(now-s.getUpdated() > thresholdMilliseconds){
                        s.set_deleted(true);
                        deleteCount++;
                    }
                }
                // if the game is unlocked, and the gameEnded timestamp is more than (threshold) minutes ago,
                // or if the last update was more than (threshold) minutes ago, mark the session for deletion
                else{
                    if((now - s.getSessionEnded() > thresholdMilliseconds) || (now - s.getUpdated() > thresholdMilliseconds)){
                        s.set_deleted(true);
                        deleteCount++;
                    }
                }
            }
            this.sessionRepository.bulkUpdate(staleSessions);
        }

        return deleteCount;
    }

    /**
     * deletes a device from a session by the device UUID
     *
     * @param   uuid    String representation of the device's UUID
     * @return          boolean, whether the deletion completed successfully
     *
     * @see     SessionRepository#removeDeviceFromSessionByDeviceUuid(String)
     */
    public boolean deleteDeviceFromSessionByDeviceUuid(String uuid) {
        return this.sessionRepository.removeDeviceFromSessionByDeviceUuid(uuid);
    }

}
