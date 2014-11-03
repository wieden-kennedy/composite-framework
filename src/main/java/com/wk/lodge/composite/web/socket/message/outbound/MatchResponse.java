

package com.wk.lodge.composite.web.socket.message.outbound;

import com.wk.lodge.composite.model.Device;

import java.util.ArrayList;

public class MatchResponse extends BeanResponse {
    private ArrayList<Device> devices;
    private String id;
    private String applicationId;

    public MatchResponse(String applicationId, String id, ArrayList<Device> devices) {
        super();
        this.applicationId = applicationId;
        this.id = id;
        this.devices = devices;
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    public void setDevices(ArrayList<Device> devices) {
        this.devices = devices;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getApplicationId() { return applicationId; }

    public void setApplicationId(String applicationId) { this.applicationId = applicationId; }
}
