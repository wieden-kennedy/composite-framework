package com.wk.lodge.composite.config;

import com.wk.lodge.composite.registry.DeviceRegistry;
import com.wk.lodge.composite.web.socket.server.support.CompositeHandshakeHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.*;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.AbstractWebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.server.standard.TomcatRequestUpgradeStrategy;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

@Configuration
@EnableScheduling
@PropertySource("classpath:/rabbitmq.properties")
@ComponentScan(
        basePackages="com.wk.lodge.composite",
        excludeFilters = @ComponentScan.Filter(type= FilterType.ANNOTATION, value = Configuration.class)
)
@EnableWebSocketMessageBroker
public class TestWebSocketConfig extends AbstractWebSocketMessageBrokerConfigurer {
    private @Value(value = "${rabbitmq.host}") String relayHost;
    private @Value(value = "${rabbitmq.port}") Integer relayPort;
    private @Value(value = "${rabbitmq.clientLogin}") String clientLogin;
    private @Value(value = "${rabbitmq.clientPasscode}") String clientPasscode;
    private @Value(value = "${rabbitmq.systemLogin}") String systemLogin;
    private @Value(value = "${rabbitmq.systemPasscode}") String systemPasscode;

    @Bean
    public DefaultHandshakeHandler handshakeHandler() {
        return new CompositeHandshakeHandler(new TomcatRequestUpgradeStrategy());
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer
    getPropertySourcesPlaceholderConfigurer(){
        return new PropertySourcesPlaceholderConfigurer();
    }

    @Bean
    public DeviceRegistry deviceRegistry() {
        return new DeviceRegistry();
    }

    @Autowired
    Environment env;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/composite").setHandshakeHandler(handshakeHandler())
                .withSockJS();
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableStompBrokerRelay("/queue/", "/topic/")
                .setRelayHost(relayHost).setRelayPort(relayPort).setAutoStartup(true)
                .setClientLogin(clientLogin).setClientPasscode(clientPasscode)
                .setSystemLogin(systemLogin).setSystemPasscode(systemPasscode);
        registry.setApplicationDestinationPrefixes("/app");
    }
}
