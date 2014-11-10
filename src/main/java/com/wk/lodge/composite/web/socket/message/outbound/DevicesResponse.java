package com.wk.lodge.composite.web.socket.message.outbound;

import com.wk.lodge.composite.model.Device;

import java.util.ArrayList;

public class DevicesResponse extends BeanResponse {
    private ArrayList<Device> devices;

    public DevicesResponse(ArrayList<Device> devices) {
        super();
        this.type = "devices";
        this.devices = devices;
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    public void setDevices(ArrayList<Device> devices) {
        this.devices = devices;
    }
}
