package com.wk.lodge.composite.web.support.client;


import org.springframework.messaging.Message;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;


public interface StompMessageHandler {

	void afterConnected(StompSession session, StompHeaderAccessor headers);

	void handleMessage(Message<byte[]> message);

	void handleReceipt(String receiptId);

	void handleError(Message<byte[]> message);

	void afterDisconnected();

}
