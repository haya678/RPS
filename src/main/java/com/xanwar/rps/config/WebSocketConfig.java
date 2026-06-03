package com.xanwar.rps.config;

import com.xanwar.rps.websocket.RpsWebSocketHandler;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final RpsWebSocketHandler rpsWebSocketHandler;
    private final GameProperties gameProperties;

    public WebSocketConfig(RpsWebSocketHandler rpsWebSocketHandler, GameProperties gameProperties) {
        this.rpsWebSocketHandler = rpsWebSocketHandler;
        this.gameProperties = gameProperties;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(rpsWebSocketHandler, gameProperties.getWebsocket().getPath())
                .setAllowedOriginPatterns(
                        "https://*.onrender.com",
                        "https://*.torn.com",
                        "http://localhost:[*]",
                        "http://127.0.0.1:[*]"
                );
    }
}
