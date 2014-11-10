package com.wk.lodge.composite.web.socket.message.inbound;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.google.gson.Gson;

public class JoinMessage extends MatchMessage{
    public static enum Types {exit, enter};
    private Types type;

    @JsonCreator
    public static JoinMessage parseJson(String jsonString){
        Gson gson = new Gson();
        return gson.fromJson(jsonString, JoinMessage.class);
    }

    public JoinMessage(){}

    public Types getType() {
        return type;
    }

    public void setType(Types type) {
        this.type = type;
    }
}
