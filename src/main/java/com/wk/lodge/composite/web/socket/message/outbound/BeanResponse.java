package com.wk.lodge.composite.web.socket.message.outbound;

import com.google.gson.annotations.Expose;

import java.util.Date;

public class BeanResponse {
    @Expose
    protected String type;
    @Expose
    protected Long serverTime;

    public BeanResponse() {
        this.serverTime = new Date().getTime();
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getServerTime() {
        return serverTime;
    }

    public void setServerTime(Long serverTime) {
        this.serverTime = serverTime;
    }
}
