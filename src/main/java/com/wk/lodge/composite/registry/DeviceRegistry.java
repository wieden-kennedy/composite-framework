package com.wk.lodge.composite.registry;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.util.Assert;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;

public class DeviceRegistry {
    private static final Log logger = LogFactory.getLog(DeviceRegistry.class);
    private final Set<UUID> healthyDevices = new CopyOnWriteArraySet<UUID>();
    private final Set<UUID> unhealthyDevices = new CopyOnWriteArraySet<UUID>();
    private final Object lock = new Object();

    /**
     * returns a set of unhealthy devices every time the scheduled task defined by DeviceService.deleteUnhealthyDevices
     * runs. All devices formerly considered healthy are added to the unhealthy device registry after unhealthy devices
     * are added to a temp set, then cleared from the unhealthy device registry. As devices ping the server, they are
     * added back into the healthy device registry using the below method, addHealthyDevice.
     *
     * @return      the set of unhealthy device UUIDs
     *
     * @see         com.wk.lodge.composite.service.DeviceService#deleteUnhealthyDevices()
     */
    public Set<UUID> getUnhealthyDevices() {
        synchronized(this.lock) {
            if(logger.isDebugEnabled())
                logger.debug(String.format("UnhealthyDevices: %d, HealthyDevices: %d",
                        unhealthyDevices.size(), healthyDevices.size()));
            Set<UUID> tempDevices = new CopyOnWriteArraySet<UUID>();
            tempDevices.addAll(unhealthyDevices);
            unhealthyDevices.clear();
            unhealthyDevices.addAll(healthyDevices);
            healthyDevices.clear();
            return tempDevices;
        }
    }

    /**
     * adds a device UUID to the healthy device registry each time the device pings the server. Devices will always
     * be added to the healthy device registry unless they fail to ping the server within the periodic window at which
     * the DeviceService.deleteUnhealthyDevices method is called.
     *
     * @param   uuidStr     string-form UUID for the device that should be added to the healthy device registry.
     *
     * @see     com.wk.lodge.composite.service.DeviceService#deleteUnhealthyDevices()
     */
    public void addHealthyDevice(String uuidStr) {
        Assert.notNull(uuidStr, "UUID must not be null");
        UUID uuid = UUID.fromString(uuidStr);
        synchronized(this.lock) {
            healthyDevices.add(uuid);
            unhealthyDevices.remove(uuid);
        }
    }

    /**
     * checks to see whether a device is healthy or not by looking up the device UUID in the healthy device registry
     *
     * @param   uuidStr     string-form UUID for the device to be checked
     * @return              boolean, whether the device is healthy
     */
    public boolean isHealthyDevice(String uuidStr) {
        Assert.notNull(uuidStr, "UUID must not be null");
        synchronized(this.lock) {
            return healthyDevices.contains(UUID.fromString(uuidStr));
        }
    }
}
