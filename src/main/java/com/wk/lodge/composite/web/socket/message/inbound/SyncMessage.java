package com.wk.lodge.composite.web.socket.message.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.gson.Gson;

public class SyncMessage extends BeanMessage {
    private Long time;

    @JsonCreator
    public static SyncMessage parseJson(String jsonString){
        Gson gson = new Gson();
        return gson.fromJson(jsonString, SyncMessage.class);
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
