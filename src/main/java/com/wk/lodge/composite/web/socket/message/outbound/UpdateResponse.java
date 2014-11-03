
package com.wk.lodge.composite.web.socket.message.outbound;

public class UpdateResponse extends BeanResponse {
    private Object data;

    public UpdateResponse(Object data) {
        super();
        this.type = "update";
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
