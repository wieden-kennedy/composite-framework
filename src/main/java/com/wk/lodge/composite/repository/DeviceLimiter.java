package com.wk.lodge.composite.repository;

import java.util.HashMap;

public class DeviceLimiter {
    private HashMap<String,Integer> maxDevicesPerSession;
    private float minDistanceThresholdBetweenDevices;
    private float maxDistanceThresholdBetweenDevices;

    public DeviceLimiter(HashMap<String,Integer> maxDevicesPerSession, float minDistanceThresholdBetweenDevices, float maxDistanceThresholdBetweenDevices){
        this.maxDevicesPerSession = maxDevicesPerSession;
        this.minDistanceThresholdBetweenDevices = minDistanceThresholdBetweenDevices;
        this.maxDistanceThresholdBetweenDevices = maxDistanceThresholdBetweenDevices;
    }
    public HashMap<String,Integer> getMaxDevicesPerSession() {
        return maxDevicesPerSession;
    }
    public float getMinDistanceThresholdBetweenDevices() {
        return minDistanceThresholdBetweenDevices;
    }
    public float getMaxDistanceThresholdBetweenDevices() {
        return maxDistanceThresholdBetweenDevices;
    }

    public int getMaxDevicesPerSessionForGame(String applicationId){
        return maxDevicesPerSession.get(applicationId);
    }

}
