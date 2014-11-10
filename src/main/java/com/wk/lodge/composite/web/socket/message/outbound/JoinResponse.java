package com.wk.lodge.composite.web.socket.message.outbound;

import com.wk.lodge.composite.model.Device;

import java.util.ArrayList;

public class JoinResponse extends MatchResponse {
    private String roomName;

    public JoinResponse(String applicationId, String id, ArrayList<Device> devices, String roomName) {
        super(applicationId, id, devices);
        this.type = "join";
        this.roomName = roomName;
    }
}
