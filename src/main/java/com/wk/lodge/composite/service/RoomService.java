package com.wk.lodge.composite.service;

import java.util.HashMap;

public class RoomService {
    private HashMap<String,String[]> roomNames;
    public RoomService(){}
    public RoomService(HashMap<String, String[]> roomNames){
        this.roomNames = roomNames;
    }

    public String[] getRoomNamesForApplication(String applicationId){
        if(roomNames.containsKey(applicationId)){
            return roomNames.get(applicationId);
        }
        else{
            return null;
        }
    }
}
