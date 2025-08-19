package com.pratik.deviceSimulator.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.List;
@Service
public class WebSocketClientService {

    private final ObjectMapper objectMapper;
    private volatile StompSession session;

    public WebSocketClientService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper; // already configured in JacksonConfig
    }

    public void connect() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);

        WebSocketStompClient stompClient = new WebSocketStompClient(sockJsClient);

        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper); // ✅ Use the Spring-configured mapper
        stompClient.setMessageConverter(converter);

        StompSessionHandler handler = new StompSessionHandlerAdapter() {
            @Override
            public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
                WebSocketClientService.this.session = session;
                System.out.println("[WebSocket] Connected successfully.");
            }

            @Override
            public void handleTransportError(StompSession session, Throwable exception) {
                System.err.println("[WebSocket] Transport error: " + exception.getMessage());
            }
        };

        stompClient.connectAsync("ws://localhost:8080/ws-sensor-data", handler);
    }

    public void sendSensorData(Object dto) {
        if (session != null && session.isConnected()) {
            session.send("/app/sensorData", dto);
        } else {
            System.err.println("WebSocket not connected. Cannot send message.");
        }
    }
}
