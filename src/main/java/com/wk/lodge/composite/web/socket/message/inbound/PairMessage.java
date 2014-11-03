package com.wk.lodge.composite.web.socket.message.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.gson.Gson;

public class PairMessage extends MatchMessage {
    public static enum Types {exit, enter};
    private Types type;

    @JsonCreator
    public static PairMessage parseJson(String jsonString){
        Gson gson = new Gson();
        return gson.fromJson(jsonString, PairMessage.class);
    }

    public PairMessage(){}

    public Types getType() {
        return type;
    }

    public void setType(Types type) {
        this.type = type;
    }
}
