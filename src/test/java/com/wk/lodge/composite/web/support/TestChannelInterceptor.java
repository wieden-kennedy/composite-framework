
package com.wk.lodge.composite.web.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;


/**
 * A ChannelInterceptor that caches mesages.
 */
public class TestChannelInterceptor extends ChannelInterceptorAdapter {

    private final BlockingQueue<Message<?>> messages = new ArrayBlockingQueue<>(100);

    private final List<String> destinationPatterns = new ArrayList<>();

    private final PathMatcher matcher = new AntPathMatcher();

    private volatile boolean isRecording;


    /**
     * @param autoStart whether to start recording messages removing the need to
     * 	call {@link #startRecording()} explicitly
     */
    public TestChannelInterceptor(boolean autoStart) {
        this.isRecording = autoStart;
    }

    public void setIncludedDestinations(String... patterns) {
        this.destinationPatterns.addAll(Arrays.asList(patterns));
    }

    public void startRecording() {
        this.isRecording = true;
    }

    public void stopRecording() {
        this.isRecording = false;
    }

    /**
     * @return the next received message or {@code null} if the specified time elapses
     */
    public Message<?> awaitMessage(long timeoutInSeconds) throws InterruptedException {
        return this.messages.poll(timeoutInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {

        if (this.isRecording) {
            if (this.destinationPatterns.isEmpty()) {
                this.messages.add(message);
            }
            else {
                StompHeaderAccessor headers = StompHeaderAccessor.wrap(message);
                if (headers.getDestination() != null) {
                    for (String pattern : this.destinationPatterns) {
                        if (this.matcher.match(pattern, headers.getDestination())) {
                            this.messages.add(message);
                            break;
                        }
                    }
                }
            }
        }

        return message;
    }

}