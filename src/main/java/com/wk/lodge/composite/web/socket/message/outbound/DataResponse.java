
package com.wk.lodge.composite.web.socket.message.outbound;

public class DataResponse extends BeanResponse {
    private Object data;

    public DataResponse(Object data) {
        super();
        this.type = "data";
        this.data = data;
    }

    public Object getData() {
        return data;
    }

    public void setData(Object data) {
        this.data = data;
    }

}
