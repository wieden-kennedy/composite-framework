package com.wk.lodge.composite.model;

import javax.persistence.Column;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import java.util.ArrayList;
import java.util.UUID;

public class Session {
    @Id
    @GeneratedValue
    @Column(name="_id")
    private String _id;

    @GeneratedValue
    @Column(name="_rev")
    private String _rev;
    private boolean _deleted;

    private String applicationId;
    private ArrayList<Device> devices;
    private float[] geoLocation;
    private long inserted;
    private boolean locked;
    private String room;
    private long sessionStarted;
    private long sessionEnded;
    private long updated;
    private UUID uuid;

    public Session(){
        this.uuid = UUID.randomUUID();
        this.locked = false;
        this.devices = new ArrayList<Device>();
    }

    public String getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(String applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public ArrayList<Device> getDevices() {
        return devices;
    }

    public void setDevices(ArrayList<Device> devices) {
        this.devices = devices;
    }

    public long getInserted() {
        return inserted;
    }

    public void setInserted(long inserted) {
        this.inserted = inserted;
    }

    public boolean isLocked() {
        return locked;
    }

    public void setLocked(boolean locked) {
        this.locked = locked;
    }

    public long getUpdated() {
        return updated;
    }

    public void setUpdated(long updated) {
        this.updated = updated;
    }

    public long getSessionStarted() {
        return sessionStarted;
    }

    public void setSessionStarted(long sessionStarted) {
        this.sessionStarted = sessionStarted;
    }

    public long getSessionEnded() {
        return sessionEnded;
    }

    public void setSessionEnded(long sessionEnded) {
        this.sessionEnded = sessionEnded;
    }

    public float[] getGeoLocation() {
        return geoLocation;
    }

    public void setGeoLocation(float[] geoLocation) {
        this.geoLocation = geoLocation;
    }

    public void addDevice(Device device){
        if(this.devices == null){
            this.devices = new ArrayList<Device>();
        }
        this.devices.add(device);
    }

    public UUID removeDevice(Device device){
        UUID deviceUuid = device.getUuid();
        this.devices.remove(this.devices.indexOf(device));

        return deviceUuid;
    }

    public void removeDeviceByUuid(String deviceUuid){
        ArrayList<Device> devices = new ArrayList<Device>();
        for (Device d: this.getDevices()){
            if(!d.getUuid().toString().equals(deviceUuid)){
                devices.add(d);
            }
        }

        this.setDevices(devices);
    }

    public boolean deviceInSession(UUID deviceUuid){
        for(Device d: this.getDevices()){
            if(d.getUuid().toString().equals(deviceUuid.toString())){
                return true;
            }
        }

        return false;
    }

    public String getRoom() {
        return room;
    }

    public void setRoom(String room) {
        this.room = room;
    }

    // couch-specific getters/setters
    public boolean is_deleted() {
        return _deleted;
    }

    public void set_deleted(boolean _deleted) {
        this._deleted = _deleted;
    }

    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public String get_rev() {
        return _rev;
    }

    public void set_rev(String _rev) {
        this._rev = _rev;
    }

}
