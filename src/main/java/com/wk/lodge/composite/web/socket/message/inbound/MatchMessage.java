
package com.wk.lodge.composite.web.socket.message.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.gson.Gson;
import com.wk.lodge.composite.model.Device;

public class MatchMessage {
    private String applicationId;
    private Device device;
    private float[] geo;
    private int[] point;
    private float[] vector;

    public MatchMessage(){}

    @JsonCreator
    public static MatchMessage parseJson(String jsonString){
        Gson gson = new Gson();
        return gson.fromJson(jsonString, MatchMessage.class);
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public Device getDevice() {
        return device;
    }

    public void setDevice(Device device) {
        this.device = device;
    }

    public float[] getGeo() {
        return geo;
    }

    public void setGeo(float[] geo) {
        this.geo = geo;
    }

    public int[] getPoint() {
        return point;
    }

    public void setPoint(int[] point) {
        this.point = point;
    }

    public float[] getVector() {
        return vector;
    }

    public void setVector(float[] vector) {
        this.vector = vector;
    }

}
