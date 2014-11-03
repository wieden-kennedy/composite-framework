
package com.wk.lodge.composite.web.socket.message.outbound;

public class SyncResponse extends BeanResponse {
    private Long time;

    public SyncResponse(Long time) {
        super();
        this.type = "sync";
        this.time = time;
    }

    public Long getTime() {
        return time;
    }

    public void setTime(Long time) {
        this.time = time;
    }
}
