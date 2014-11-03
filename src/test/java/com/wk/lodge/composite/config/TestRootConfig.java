package com.wk.lodge.composite.config;

import com.wk.lodge.composite.repository.DeviceLimiter;
import com.wk.lodge.composite.service.RoomService;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.*;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.simp.annotation.support.SimpAnnotationMethodMessageHandler;

import java.util.HashMap;
import java.util.List;

/**
 * Configuration class that un-registers MessageHandler's it finds in the
 * ApplicationContext from the message channels they are subscribed to...
 * except the message handler used to invoke annotated message handling methods.
 * The intent is to reduce additional processing and additional messages not
 * related to the test.
 */
@Configuration
@PropertySources(value={@PropertySource("classpath:/couchdb.properties"), @PropertySource("classpath:/application.properties")})
@ComponentScan(
        basePackages="com.wk.lodge.composite",
        excludeFilters = @ComponentScan.Filter(type= FilterType.ANNOTATION, value = Configuration.class)
)
public class TestRootConfig implements ApplicationListener<ContextRefreshedEvent> {
    @Value(value= "${appOne.applicationId}") private String appOneApplicationId;
    @Value(value = "${appOne.maxDevicesPerSession}") private int appOneMaxDevicesPerSession;
    @Value(value = "${appOne.roomNames}") private String[] appOneRoomNames;
    @Value(value= "${appTwo.applicationId}") private String appTwoApplicationId;
    @Value(value = "${appTwo.maxDevicesPerSession}") private int appTwoMaxDevicesPerSession;
    @Value(value = "${minDistanceThresholdBetweenDevices}") private float minDistanceThresholdBetweenDevices;
    @Value(value = "${maxDistanceThresholdBetweenDevices}") private float maxDistanceThresholdBetweenDevices;

    @Value(value = "${couchdb.host}") private String couchDbHostname;
    @Value(value = "${couchdb.port}") private int couchDbHostPort;
    @Value(value = "${couchdb.createdb.if-not-exist}") private boolean couchDbCreateIfNotExist;
    @Value(value = "${couchdb.protocol}") private String couchDbProtocol;
    @Value(value = "${couchdb.max.connections}") private int couchDbMaxConnections;
    @Value(value = "${couchdb.sessions.database.name}") private String sessionDatabaseName;
    @Value(value = "${couchdb.username}") private String couchDbAdminUser;
    @Value(value = "${couchdb.password}") private String couchDbAdminPassword;

    @Bean
    public static PropertySourcesPlaceholderConfigurer
    getPropertySourcesPlaceholderConfigurer(){
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean(destroyMethod = "shutdown")
    public CouchDbClient couchDbSessionClient(){
        CouchDbProperties couchDbProperties = new CouchDbProperties();
        couchDbProperties.setHost(couchDbHostname);
        couchDbProperties.setPort(couchDbHostPort);
        couchDbProperties.setCreateDbIfNotExist(couchDbCreateIfNotExist);
        couchDbProperties.setProtocol(couchDbProtocol);
        couchDbProperties.setMaxConnections(couchDbMaxConnections);
        couchDbProperties.setDbName(sessionDatabaseName);
        couchDbProperties.setUsername(couchDbAdminUser);
        couchDbProperties.setPassword(couchDbAdminPassword);
        return new CouchDbClient(couchDbProperties);
    }

    @Bean
    public DeviceLimiter deviceLimiter(){
        HashMap<String,Integer> maxDevicesPerSession = new HashMap<>();
        maxDevicesPerSession.put(appOneApplicationId, appOneMaxDevicesPerSession);
        maxDevicesPerSession.put(appTwoApplicationId, appTwoMaxDevicesPerSession);
        return new DeviceLimiter(maxDevicesPerSession,minDistanceThresholdBetweenDevices,maxDistanceThresholdBetweenDevices);
    }

    @Bean
    public RoomService roomService(){
        HashMap<String,String[]> roomNames = new HashMap<>();
        roomNames.put(appOneApplicationId, appOneRoomNames);
        return new RoomService(roomNames);
    }

    @Autowired
    Environment env;

    @Autowired
    private List<SubscribableChannel> channels;

    @Autowired
    private List<MessageHandler> handlers;

    @Override
    public void onApplicationEvent(ContextRefreshedEvent event) {
        for (MessageHandler handler : handlers) {
            if (handler instanceof SimpAnnotationMethodMessageHandler) {
                continue;
            }
            for (SubscribableChannel channel :channels) {
                channel.unsubscribe(handler);
            }
        }
    }

}