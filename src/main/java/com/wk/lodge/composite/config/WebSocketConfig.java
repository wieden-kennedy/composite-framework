package com.wk.lodge.composite.config;

import com.wk.lodge.composite.registry.DeviceRegistry;
import com.wk.lodge.composite.web.socket.server.support.CompositeHandshakeHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurationSupport;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.standard.ServletServerContainerFactoryBean;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableScheduling
@PropertySource("classpath:/rabbitmq.properties")
@ComponentScan(
        basePackages="com.wk.lodge.composite",
        excludeFilters = @ComponentScan.Filter(type = FilterType.ANNOTATION, value = Configuration.class)
)
public class WebSocketConfig extends WebSocketMessageBrokerConfigurationSupport {
    private @Value(value = "${rabbitmq.host}") String host;
    private @Value(value = "${rabbitmq.port}") Integer port;
    private @Value(value = "${rabbitmq.clientLogin}") String clientLogin;
    private @Value(value = "${rabbitmq.clientPasscode}") String clientPasscode;
    private @Value(value = "${rabbitmq.systemLogin}") String systemLogin;
    private @Value(value = "${rabbitmq.systemPasscode}") String systemPasscode;
    private @Value(value = "${rabbitmq.systemHeartbeatSendInterval}") long systemHeartbeatSendInterval;
    private @Value(value = "${rabbitmq.systemHeartbeatReceiveInterval}") long systemHeartbeatReceiveInterval;
    private @Value(value = "${rabbitmq.heartbeatTime}") long heartbeatTime;
    private @Value(value = "${rabbitmq.inboundChannelCorePoolSize}") int inboundChannelCorePoolSize;
    private @Value(value = "${rabbitmq.outboundChannelCorePoolSize}") int outboundChannelCorePoolSize;
    private @Value(value = "${rabbitmq.brokerChannelCorePoolSize}") int brokerChannelCorePoolSize;
    private @Value(value = "${rabbitmq.sendTimeLimit}") int sendTimeLimit;
    private @Value(value = "${rabbitmq.sendBufferSizeLimit}") int sendBufferSizeLimit;
    private @Value(value = "${rabbitmq.messageSizeLimit}") int messageSizeLimit;

    @Bean
    public static PropertySourcesPlaceholderConfigurer
    getPropertySourcesPlaceholderConfigurer(){
        return new PropertySourcesPlaceholderConfigurer();
    }

	@Override
	public void registerStompEndpoints(StompEndpointRegistry registry) {
		registry.addEndpoint("/composite").setHandshakeHandler(handshakeHandler())
                .withSockJS().setHeartbeatTime(heartbeatTime);
	}

	@Override
	public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(inboundChannelCorePoolSize);
	}

	@Override
	public void configureClientOutboundChannel(ChannelRegistration registration) {
        registration.taskExecutor().corePoolSize(outboundChannelCorePoolSize);
	}

	@Override
	public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue/", "/topic/")
                .setRelayHost(host).setRelayPort(port).setAutoStartup(true)
                .setClientLogin(clientLogin).setClientPasscode(clientPasscode)
                .setSystemLogin(systemLogin).setSystemPasscode(systemPasscode)
                .setSystemHeartbeatSendInterval(systemHeartbeatSendInterval)
                .setSystemHeartbeatReceiveInterval(systemHeartbeatReceiveInterval);
		registry.setApplicationDestinationPrefixes("/app");
        registry.configureBrokerChannel().taskExecutor().corePoolSize(brokerChannelCorePoolSize);
	}

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setSendTimeLimit(sendTimeLimit).setSendBufferSizeLimit(sendBufferSizeLimit * 1024);
        registration.setMessageSizeLimit(messageSizeLimit * 1024);
    }


    @Bean
    public DefaultHandshakeHandler handshakeHandler() {
        return new CompositeHandshakeHandler();
    }

    @Bean
    public DeviceRegistry deviceRegistry() {
        return new DeviceRegistry();
    }

    @Bean
    public ServletServerContainerFactoryBean createWebSocketContainer() {
        ServletServerContainerFactoryBean container = new ServletServerContainerFactoryBean();
        container.setMaxTextMessageBufferSize(8192);
        container.setMaxBinaryMessageBufferSize(8192);
        return container;
    }

}
