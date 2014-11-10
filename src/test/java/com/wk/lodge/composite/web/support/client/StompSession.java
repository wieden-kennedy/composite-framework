package com.wk.lodge.composite.web.support.client;


public interface StompSession {

	void subscribe(String destination, String receiptId);

	void send(String destination, Object payload);

	void disconnect();

}
