package com.pratik.deviceSimulator.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pratik.deviceSimulator.config.SimulatorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.stereotype.Service;

import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.slf4j.*;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.*;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Type;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class WebSocketClientService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketClientService.class);

    private final SimulatorConfig config; // holds websocket URL like ws://iotanalyser:8080/ws-sensor-data
    private WebSocketStompClient stompClient;
    private StompSession stompSession;

    public WebSocketClientService(SimulatorConfig config) {
        this.config = config;
        initClient();
    }

    private void initClient() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        this.stompClient = new WebSocketStompClient(sockJsClient);
        this.stompClient.setMessageConverter(new org.springframework.messaging.converter.MappingJackson2MessageConverter());
    }
    public synchronized boolean connectWithRetries(int maxAttempts, long baseDelayMs) {
        int attempt = 0;
        String url = config.getWebsocketUrl();
        log.info("WebSocket connect attempt {}/{} to {}", attempt, maxAttempts, url);
        if (url == null) {
            log.warn("Websocket URL is null - check config and env");
            return false;
        }

        while (attempt++ < maxAttempts) {
            try {
                log.info("WebSocket connect attempt {}/{} -> {}", attempt, maxAttempts, url);

                StompSessionHandler handler = new StompSessionHandlerAdapter() {
                    @Override
                    public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                        log.info("WebSocket connected");
                    }
                    @Override
                    public void handleException(StompSession session, StompCommand command,
                                                StompHeaders headers, byte[] payload, Throwable exception) {
                        log.warn("WebSocket exception: {}", exception.getMessage());
                    }
                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        log.warn("WebSocket transport error: {}", exception.getMessage());
                    }
                };

                CompletableFuture<StompSession> future = stompClient.connectAsync(url, handler);
                StompSession session = future.get(5, TimeUnit.SECONDS);

                if (session != null && session.isConnected()) {
                    this.stompSession = session;
                    return true;
                }
            } catch (Exception ex) {
                log.warn("WebSocket connect failed: {}", ex.getMessage());
            }
            try { Thread.sleep(baseDelayMs * attempt); } catch (InterruptedException ignored) {}
        }
        log.error("Unable to connect websocket after {} attempts", maxAttempts);
        return false;
    }

    public synchronized void disconnect() {
        try {
            if (stompSession != null && stompSession.isConnected()) {
                stompSession.disconnect();
            }
        } catch (Exception ignored) {}
        stompSession = null;
    }

    public synchronized void sendSensorData(Object dto) {
        if (stompSession == null || !stompSession.isConnected()) {
            log.warn("WebSocket not connected. Cannot send message.");
            // optionally attempt quick reconnect:
            boolean ok = connectWithRetries(3, 500);
            if (!ok) return;
        }
        stompSession.send("/topic/sensorData", dto);
    }
}
