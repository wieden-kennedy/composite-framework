

package com.wk.lodge.composite.web.socket.message.outbound;

public class InitResponse extends BeanResponse {
    private String uuid;

    public InitResponse(String uuid) {
        super();
        this.type = "init";
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }
}
