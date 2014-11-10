package com.wk.lodge.composite.config;

import com.wk.lodge.composite.repository.DeviceLimiter;
import com.wk.lodge.composite.service.RoomService;
import org.lightcouch.CouchDbClient;
import org.lightcouch.CouchDbProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.context.annotation.PropertySources;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;

import java.util.HashMap;


@Configuration
@PropertySources(value={@PropertySource("classpath:/couchdb.properties"), @PropertySource("classpath:/application.properties")})
public class RootConfig {

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
    public DeviceLimiter getDeviceLimiter(){
        HashMap<String,Integer> maxDevicesPerSession = new HashMap<>();
        maxDevicesPerSession.put(appOneApplicationId, appOneMaxDevicesPerSession);
        maxDevicesPerSession.put(appTwoApplicationId, appTwoMaxDevicesPerSession);
        return new DeviceLimiter(maxDevicesPerSession,minDistanceThresholdBetweenDevices,maxDistanceThresholdBetweenDevices);
    }

    @Bean
    public RoomService getRoomService(){
        HashMap<String,String[]> roomNames = new HashMap<>();
        roomNames.put(appOneApplicationId, appOneRoomNames);
        return new RoomService(roomNames);
    }

}
