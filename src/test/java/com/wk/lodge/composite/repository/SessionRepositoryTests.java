package com.wk.lodge.composite.repository;


import com.wk.lodge.composite.config.TestRootConfig;
import com.wk.lodge.composite.config.TestWebSocketConfig;
import com.wk.lodge.composite.model.Device;
import com.wk.lodge.composite.model.Session;
import com.wk.lodge.composite.service.RoomService;
import com.wk.lodge.composite.service.SessionService;
import com.wk.lodge.composite.web.socket.message.inbound.JoinMessage;
import com.wk.lodge.composite.web.socket.message.inbound.PairMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.lightcouch.CouchDbClient;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.core.MessageSendingOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.*;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { TestWebSocketConfig.class, TestRootConfig.class })
public class SessionRepositoryTests {
    static final Log logger = LogFactory.getLog(SessionRepositoryTests.class);
    private HashMap<String,Integer> maxDevicesPerSession;
    private SessionRepository repo;
    private SessionService service;
    @Mock
    private MessageSendingOperations<String> messagingTemplate;
    @Autowired
    private CouchDbClient couchDbClient;

    private void flushDatabase(){
        List<Session> sessions = couchDbClient.view("app/uuid")
            .includeDocs(true)
            .query(Session.class);
        for(Session s: sessions){
            s.set_deleted(true);
        }

        this.repo.bulkUpdate(sessions);
    }

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        maxDevicesPerSession = new HashMap<>();
        maxDevicesPerSession.put("test-application-identifier",8);
        maxDevicesPerSession.put("test-cant-add-same-device",8);
        maxDevicesPerSession.put("test-max-devices-per-session",20);
        maxDevicesPerSession.put("test-new-session-if-locked",8);
        maxDevicesPerSession.put("test-find-by-device-uuid",8);
        maxDevicesPerSession.put("test-find-by-geohash",8);
        maxDevicesPerSession.put("test-remove-device-by-uuid",8);
        maxDevicesPerSession.put("test-delete-session",8);
        maxDevicesPerSession.put("appOne",8);
        maxDevicesPerSession.put("appTwo",2);


        DeviceLimiter dl = new DeviceLimiter(maxDevicesPerSession,3,100);


        HashMap<String,String[]> roomNames = new HashMap<>();
        roomNames.put("appOne",new String[]{"test-room"});
        RoomService roomService = new RoomService(roomNames);

        this.service = new SessionService();
        this.repo = new SessionRepository(couchDbClient, dl, roomService, messagingTemplate);
        this.service.setSessionRepository(this.repo);
    }

    @Test
    public void testAddSession() throws Exception{
        Session session = new Session();

        Session test = this.repo.create(session);

        assertNotNull(test);
        assertEquals(session.getUuid(), test.getUuid());

        this.flushDatabase();
    }

    @Test
    public void testGetOrCreateSessionForDevice() throws Exception{
        JoinMessage s = new JoinMessage();
        Device d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        s.setApplicationId("test-application-identifier");
        s.setDevice(d);
        s.setGeo(new float[]{(float)33.98954687177575,(float)-118.46242952171569});
        s.setPoint(new int[]{0,0});
        s.setVector(new float[]{1,1});

        Session session = this.repo.getOrCreate(s);
        assertNotNull(session);

        JoinMessage ss = new JoinMessage();
        Device dd = new Device();
        dd.setWidth(1000);
        dd.setHeight(800);
        ss.setApplicationId("test-application-identifier");
        ss.setDevice(dd);
        ss.setGeo(new float[]{(float)33.98951030000000,(float)-118.46252170000000});
        ss.setPoint(new int[]{1,1});
        ss.setVector(new float[]{0,0});

        Session session2 = this.repo.getOrCreate(ss);

        assertEquals(session.getApplicationId(),session2.getApplicationId());
        assertNotNull(session2);
        assertEquals(2,session2.getDevices().size());

        this.flushDatabase();

    }

    @Test
    public void testCantAddSameDeviceTwice() throws Exception{
        JoinMessage j = new JoinMessage();
        Device d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        j.setDevice(d);
        j.setApplicationId("test-cant-add-same-device");
        j.setGeo(new float[]{(float) 43.7198276, (float) 39.7087364});
        j.setPoint(new int[]{0,0});
        j.setVector(new float[]{1,1});

        JoinMessage jj = new JoinMessage();
        Device dd = d;
        d.setWidth(1282);
        d.setHeight(849);
        jj.setDevice(d);
        jj.setApplicationId("test-cant-add-same-device");
        jj.setGeo(new float[]{(float) 43.7198274, (float) 39.7087344});
        jj.setPoint(new int[]{0,0});
        jj.setVector(new float[]{1,1});


        Session s = this.repo.getOrCreate(j);
        assertNotNull(s);
        Session s2 = this.repo.getOrCreate(jj);

        //assertNotEquals(s.getUuid(),s2.getUuid());
        assertEquals(1,s2.getDevices().size());

        this.flushDatabase();

    }

    @Test
    public void testMaxDevicesPerSession() throws Exception{
        List<JoinMessage> appOneJoins = new ArrayList<JoinMessage>();
        Session appOneSession = new Session();

        // appOne --> max == 8
        int deviceCount = 8;
        while(deviceCount > 0){
            JoinMessage j = new JoinMessage();
            Device d = new Device();
            d.setWidth(1282);
            d.setHeight(849);
            j.setDevice(d);
            j.setApplicationId("appOne");
            j.setGeo(new float[]{(float) 43.7198276, (float) 39.7087364});
            j.setPoint(new int[]{0,0});
            j.setVector(new float[]{1,1});

            appOneJoins.add(j);

            deviceCount--;
        }

        assertEquals(8, appOneJoins.size());

        for(JoinMessage join:appOneJoins){
            appOneSession = this.repo.getOrCreate(join);
        }
        assertEquals(8, appOneSession.getDevices().size());

        JoinMessage jj = new JoinMessage();
        Device d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        jj.setDevice(d);
        jj.setApplicationId("appOne");
        jj.setGeo(new float[]{(float) 43.7198276, (float) 39.7087364});
        jj.setPoint(new int[]{0,0});
        jj.setVector(new float[]{1,1});

        Session appOneSession2 = this.repo.getOrCreate(jj);

        assertEquals(1, appOneSession2.getDevices().size());

        // appTwo --> max == 2
        Session appTwoSession = new Session();
        List<JoinMessage> appTwoJoins = new ArrayList<>();
        deviceCount = 2;
        while(deviceCount > 0){
            JoinMessage j = new JoinMessage();
            d = new Device();
            d.setWidth(1282);
            d.setHeight(849);
            j.setDevice(d);
            j.setApplicationId("appTwo");
            j.setGeo(new float[]{(float) 43.7198276, (float) 39.7087364});
            j.setPoint(new int[]{0,0});
            j.setVector(new float[]{1,1});

            appTwoJoins.add(j);

            deviceCount--;
        }

        assertEquals(2, appTwoJoins.size());

        for(JoinMessage join:appTwoJoins){
            appTwoSession = this.repo.getOrCreate(join);
        }
        assertEquals(2, appTwoSession.getDevices().size());

        jj = new JoinMessage();
        d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        jj.setDevice(d);
        jj.setApplicationId("appTwo");
        jj.setGeo(new float[]{(float) 43.7198276, (float) 39.7087364});
        jj.setPoint(new int[]{0,0});
        jj.setVector(new float[]{1,1});

        Session appTwoSession2 = this.repo.getOrCreate(jj);

        assertEquals(1, appTwoSession2.getDevices().size());

        this.flushDatabase();
    }

    @Test
    public void testNewSessionIfSessionLocked() throws Exception{
        JoinMessage s = new JoinMessage();
        Device d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        s.setDevice(d);
        s.setApplicationId("test-new-session-if-locked");
        s.setGeo(new float[]{(float)43.7198276,(float)39.7087364});
        s.setPoint(new int[]{0,0});
        s.setVector(new float[]{1,1});

        Session session = this.repo.getOrCreate(s);

        JoinMessage ss = new JoinMessage();
        Device dd = new Device();
        dd.setWidth(1000);
        dd.setHeight(800);
        ss.setDevice(dd);
        ss.setApplicationId("test-new-session-if-locked");
        ss.setGeo(s.getGeo());
        ss.setPoint(new int[]{1,1});
        ss.setVector(new float[]{0,0});

        Session sharedSession = this.repo.getOrCreate(ss);

        Session lockedSession = this.repo.find(session.getUuid().toString());
        assertEquals(lockedSession.getUuid(), session.getUuid());
        assertEquals(2, lockedSession.getDevices().size());

        lockedSession.setLocked(true);
        assertTrue(lockedSession.isLocked());

        this.repo.update(lockedSession);


        JoinMessage sss = new JoinMessage();
        Device ddd = new Device();
        ddd.setWidth(1000);
        ddd.setHeight(800);
        sss.setDevice(dd);
        sss.setApplicationId("test-new-session-if-locked");
        sss.setGeo(s.getGeo());
        sss.setPoint(new int[]{1,1});
        sss.setVector(new float[]{0,0});

        Session newSession = this.repo.getOrCreate(sss);

        //assertNotEqual(newSession.getUuid().toString(), lockedSession.getUuid().toString());
        assertEquals(2, lockedSession.getDevices().size());
        assertEquals(1, newSession.getDevices().size());

        this.flushDatabase();

    }

    @Test
    public void testFindSession() throws Exception{
        Session s = new Session();
        this.repo.create(s);
        Session ss = this.repo.find(s.getUuid().toString());
        assertEquals(s.getUuid(), ss.getUuid());

        this.flushDatabase();
    }

    @Test
    public void testFindByDeviceUuid() throws Exception{
        JoinMessage s = new JoinMessage();
        Device d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        s.setDevice(d);
        s.setApplicationId("test-find-by-device-uuid");
        s.setGeo(new float[]{(float)13.7198276,(float)79.7087364});
        s.setPoint(new int[]{0,0});
        s.setVector(new float[]{1,1});

        Session session = this.repo.getOrCreate(s);

        Session session2 = this.repo.findByDeviceUuid(d.getUuid().toString());

        assertNotNull(session2);
        assertEquals(session.getUuid(), session2.getUuid());

        this.flushDatabase();
    }

    @Test
    public void testFindByGeohash() throws Exception{
        JoinMessage s = new JoinMessage();
        Device d = new Device();
        d.setWidth(1282);
        d.setHeight(849);
        s.setDevice(d);
        s.setGeo(new float[]{(float)18.7198276,(float)39.7087364});
        s.setPoint(new int[]{0,0});
        s.setApplicationId("test-find-by-geohash");
        s.setVector(new float[]{1,1});

        Session session = this.repo.getOrCreate(s);

        this.flushDatabase();
    }

    @Test
    public void testUpdateSession() throws Exception{
        Session session = new Session();
        UUID uuid = session.getUuid();
        session = this.repo.create(session);

        Device d = new Device();
        d.setHeight(1000);
        d.setWidth(800);

        session.addDevice(d);

        session = this.repo.update(session);

        assertNotNull(session);
        assertEquals(session.getUuid(), uuid);
        assertEquals(1,session.getDevices().size());

        this.flushDatabase();
    }

    @Test
    public void testRemoveDeviceFromSessionByDeviceUuid() throws Exception {
        Device d = new Device();
        Device dd = new Device();

        JoinMessage s = new JoinMessage();
        s.setDevice(d);
        s.setGeo(new float[]{(float)15.7198276,(float)1.7087364});
        s.setPoint(new int[]{0,0});
        s.setApplicationId("test-remove-device-by-uuid");
        s.setVector(new float[]{1,1});

        JoinMessage ss = new JoinMessage();
        ss.setGeo(new float[]{(float)15.7198276,(float)1.7087364});
        ss.setPoint(new int[]{0,0});
        ss.setApplicationId("test-remove-device-by-uuid");
        ss.setVector(new float[]{1,1});
        ss.setDevice(dd);

        Session session = this.repo.getOrCreate(s);
        Session session2 = this.repo.getOrCreate(ss);

        boolean sessionUpdated = this.repo.removeDeviceFromSessionByDeviceUuid(d.getUuid().toString());

        Session session3 = this.repo.find(session.getUuid().toString());
        assertTrue(sessionUpdated);
        assertEquals(1,session3.getDevices().size());
        this.flushDatabase();
    }

    @Test
    public void testDeleteSession() throws Exception{
        Device d = new Device();
        JoinMessage s = new JoinMessage();
        s.setDevice(d);
        s.setGeo(new float[]{(float)62.7198276,(float)39.7087364});
        s.setPoint(new int[]{0,0});
        s.setVector(new float[]{1,1});
        s.setApplicationId("test-delete-session");

        Session session = this.repo.getOrCreate(s);

        this.repo.delete(session.getUuid().toString());

        Session session2 = this.repo.find(session.getUuid().toString());
        assertNull(session2);

        this.flushDatabase();
    }

    @Test
    public void testDeleteStaleSessions(){
        // session that was created, but never got anywhere (inserted == updated)
        int count = 0;
        Session s = null;
        while(count < 10){
            s = new Session();
            s.addDevice(new Device());
            s.setInserted(new Date().getTime() - (6*60000));
            s.setUpdated(s.getInserted());
            s.setRoom("vin diesel");
            s = this.repo.create(s);
            count++;
        }

        int deleteCount = this.service.deleteStale(5);
        try {
            Thread.sleep(1500);
            assertTrue(deleteCount == 10);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        count = 0;
        // locked, where updated time is greater than 5 minutes ago
        while(count < 20){
            s = new Session();
            s.setLocked(true);
            s.setInserted(new Date().getTime() - (10*60000));
            s.setUpdated(new Date().getTime() - (6*60000));
            s.setRoom("vin diesel");
            this.repo.create(s);
            count++;
        }

        deleteCount = this.service.deleteStale(5);
        try {
            Thread.sleep(1500);
            assertTrue(deleteCount == 20);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        count = 0;
        // unlocked, session ended more than 5 minutes ago
        while(count < 30){
            s = new Session();
            s.setLocked(false);
            s.setInserted(new Date().getTime() - (10*60000));
            s.setUpdated(new Date().getTime() - (6*60000));
            s.setSessionStarted(new Date().getTime() - (7 * 60000));
            s.setSessionEnded(new Date().getTime() - (6 * 60000));
            s.setRoom("vin diesel");
            this.repo.create(s);
            count++;
        }

        deleteCount = this.service.deleteStale(5);
        try {
            Thread.sleep(1500);
            assertTrue(deleteCount == 30);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        try {
            Thread.sleep(1500);
            List<Session> sessions = this.repo.getAll();
            assertNull(sessions);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // now create some sessions that shouldn't be deleted, and expect them to stick around
        count=0;
        while(count < 10){
            s = new Session();
            s.setLocked(true);
            s.setInserted(new Date().getTime());
            s.setUpdated(s.getInserted());
            s.setRoom("chaka khan");
            this.repo.create(s);
            count++;
        }

        deleteCount = this.service.deleteStale(5);
        assertTrue(deleteCount == 0);

        try{
            Thread.sleep(120);
            List<Session> sessions = this.repo.getAll();
            assertTrue(sessions.size() == 10);
            this.flushDatabase();
        }
        catch(InterruptedException e){
            e.printStackTrace();
        }

    }

    @Test
    public void testPairSessions(){
        PairMessage pm1 = new PairMessage();
        Device d1 = new Device();
        d1.setUuid(UUID.randomUUID());
        d1.setWidth(760);
        d1.setHeight(1020);
        pm1.setApplicationId("appTwo");
        pm1.setType(PairMessage.Types.exit);
        pm1.setDevice(d1);

        Session s1 = this.repo.pair(pm1);

        PairMessage pm2 = new PairMessage();
        Device d2 = new Device();
        d2.setUuid(UUID.randomUUID());
        d2.setWidth(1000);
        d2.setHeight(30);
        pm2.setApplicationId("appTwo");
        pm2.setType(PairMessage.Types.exit);
        pm2.setDevice(d2);

        Session s2 = this.repo.pair(pm2);

        assertEquals(s1.getUuid(),s2.getUuid());
        assertTrue(s2.isLocked());
        assertEquals(s2.getDevices().size(),2);

        this.flushDatabase();
    }

}
