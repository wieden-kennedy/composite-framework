package com.wk.lodge.composite.service;

import com.wk.lodge.composite.registry.DeviceRegistry;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.UUID;

@Service
public class DeviceService {
    private static final Log logger = LogFactory.getLog(DeviceService.class);
    private DeviceRegistry deviceRegistry;
    private SessionService sessionService;
    @Value("${deleteUnhealthyDevices}")
    private boolean runScheduledTask;

    @Autowired
    public void setDeviceRegistry(DeviceRegistry deviceRegistry) {
        this.deviceRegistry = deviceRegistry;
    }

    @Autowired
    public void setSessonService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    /**
     * Manages deletion of unhealthy devices from sessions. An unhealthy device is defined as a device that hasn't sent
     * the service a ping inside of a certain time threshold.
     *
     * If the device is marked as unhealthy, and it is not successfully deleted from the session to which it belonged,
     * it is added back into the healthy device registry so that its deletion can be reattempted on the next pass. The
     * main reason it might fail to be deleted would be a thrown DocumentConflict exception when trying to update CouchDB.
     * <p>
     * Scheduled by default to run every 2.5 seconds
     */
    @Scheduled(fixedRate=2000)
    public void deleteUnhealthyDevices() {
        if(runScheduledTask) {
            Set<UUID> unhealthyDevices = this.deviceRegistry.getUnhealthyDevices();
            for(UUID uuid : unhealthyDevices) {
                if(!this.sessionService.deleteDeviceFromSessionByDeviceUuid(uuid.toString())) {
                    this.deviceRegistry.addHealthyDevice(uuid.toString());
                }
            }
            if(logger.isDebugEnabled()) logger.debug("Cleaning up unhealthy devices");
        }
    }
}
