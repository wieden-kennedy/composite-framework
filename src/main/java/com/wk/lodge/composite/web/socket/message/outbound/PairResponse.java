package com.wk.lodge.composite.web.socket.message.outbound;

import com.wk.lodge.composite.model.Device;

import java.util.ArrayList;

public class PairResponse extends MatchResponse {

    public PairResponse(String applicationId, String id, ArrayList<Device> devices) {
        super(applicationId, id, devices);
        this.type = "pair";
    }
}
