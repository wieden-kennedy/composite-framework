
package com.wk.lodge.composite.web.socket.message.inbound;

import java.util.HashMap;

public class MapMessage extends HashMap<String, Object> {

    public String getType() {
        return (String) this.get("type");
    }

    public void setType(String type) {
        this.put("type", type);
    }

    public Object getData() {
        return this.get("data");
    }

    public void setData(Object data) {
        this.put("data", data);
    }
}
