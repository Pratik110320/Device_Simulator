package com.pratik.deviceSimulator.service;

import com.pratik.deviceSimulator.config.SimulatorConfig;
import com.pratik.deviceSimulator.dto.SensorRegistrationDto;
import com.pratik.deviceSimulator.model.SimulatedDevice;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class MultiDeviceSimulatorService {

    private static final Logger logger = LoggerFactory.getLogger(MultiDeviceSimulatorService.class);
    private final SimulatorConfig config;
    private final RestTemplate restTemplate;
    private final Random random;
    private final List<SimulatedDevice> devices = new CopyOnWriteArrayList<>();
    private final MeterRegistry meterRegistry;
    private final Counter anomalyCounter;
    private final Counter sentDataCounter;
    private final Counter disconnectionCounter;
    private final Counter reconnectionCounter;
    private volatile boolean simulationEnabled = true;

    private final WebSocketClientService webSocketClientService;

    public MultiDeviceSimulatorService(SimulatorConfig config,
                                       RestTemplate restTemplate,
                                       Random random,
                                       MeterRegistry meterRegistry,
                                       WebSocketClientService webSocketClientService) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.random = random;
        this.meterRegistry = meterRegistry;

        this.anomalyCounter = meterRegistry.counter("simulator.anomalies");
        this.sentDataCounter = meterRegistry.counter("simulator.data.sent");
        this.disconnectionCounter = meterRegistry.counter("simulator.disconnected");
        this.reconnectionCounter = meterRegistry.counter("simulator.reconnected");

        this.webSocketClientService = webSocketClientService;
    }

    @PostConstruct
    public void init() {
        logger.info(">>> init() called - setting up devices");
        if (config.getSensorTypes() == null || config.getSensorTypes().isEmpty()) {
            throw new IllegalStateException("Sensor types cannot be null or empty");
        }
        if (config.getTargetUrl() == null || config.getTargetUrl().isEmpty()) {
            throw new IllegalStateException("Target URL cannot be null or empty");
        }

        for (int i = 1; i <= config.getDeviceCount(); i++) {
            devices.add(new SimulatedDevice((long) i, config.getSensorTypes()));
        }
        logger.info(">>> Generated {} devices: {}", devices.size(), devices);

        String url = config.getTargetUrl().replace("/sensor", "/device");

        for (SimulatedDevice d : devices) {
            var payload = new DeviceRegistrationPayload(
                    d.getId(),
                    "Device-" + d.getId(),
                    pickDeviceType(d.getSensorTypes())
            );
            try {
                restTemplate.postForEntity(url, payload, Void.class);
                logger.info(">>> Registered Device {} with type {}", d.getId(), payload.deviceType());
            } catch (RestClientException e) {
                logger.error("Failed to register device {}: {}", d.getId(), e.getMessage());
            }
        }

        try {
            webSocketClientService.connect();
        } catch (Exception e) {
            logger.error("Failed to connect to WebSocket: {}", e.getMessage());
        }
    }

    public void startSimulation() {
        this.simulationEnabled = true;
    }

    public void stopSimulation() {
        this.simulationEnabled = false;
    }

    public boolean isSimulationEnabled() {
        return simulationEnabled;
    }

    public void disconnectDevice(Long deviceId) {
        devices.stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .ifPresent(d -> d.setConnected(false));
    }

    public void reconnectDevice(Long deviceId) {
        devices.stream()
                .filter(d -> d.getId().equals(deviceId))
                .findFirst()
                .ifPresent(d -> d.setConnected(true));
    }

    public void injectAnomalyToDevice(Long deviceId) {
        devices.stream()
                .filter(d -> d.getId().equals(deviceId) && d.isConnected())
                .findFirst()
                .ifPresent(d -> {
                    for (String type : d.getSensorTypes()) {
                        double anomaly = generateAnomalousValue(type);
                        SensorRegistrationDto dto = new SensorRegistrationDto(
                                d.getId(), anomaly, type, unitFor(type), true
                        );
                        restTemplate.postForEntity(config.getTargetUrl(), dto, Void.class);
                        logger.warn("[MANUAL ANOMALY] Device {} Type {} => {}", d.getId(), type, anomaly);
                    }
                });
    }

    @Scheduled(fixedRateString = "${simulator.data-push-interval:5000}")
    public void pushSensorData() {
        if (!simulationEnabled) {
            logger.info(">>> Simulation is paused");
            return;
        }
        logger.info(">>> pushSensorData() @ {}", Instant.now());

        for (SimulatedDevice device : devices) {
            if (!device.isConnected() && shouldReconnect()) {
                device.setConnected(true);
                logger.info("[RECONNECTED] Device {}", device.getId());
                reconnectionCounter.increment();
            }
            if (device.isConnected() && shouldDisconnect()) {
                device.setConnected(false);
                logger.info("[DISCONNECTED] Device {}", device.getId());
                disconnectionCounter.increment();
            }

            if (!device.getSensorTypes().isEmpty()) {
                String type = device.getSensorTypes()
                        .get(random.nextInt(device.getSensorTypes().size()));

                SensorRegistrationDto dto;
                if (device.isConnected()) {
                    double value = shouldInjectAnomaly()
                            ? generateAnomalousValue(type)
                            : generateValue(type);

                    dto = new SensorRegistrationDto(device.getId(), value, type, unitFor(type), true);

                    if (shouldInjectAnomaly()) {
                        logger.warn("[ANOMALY] Device {} - Type: {} - Value: {}", device.getId(), type, value);
                        anomalyCounter.increment();
                    }
                } else {
                    dto = new SensorRegistrationDto(device.getId(), Double.NaN, type, unitFor(type), false);
                }

                try {
                    restTemplate.postForEntity(config.getTargetUrl(), dto, Void.class);
                    webSocketClientService.sendSensorData(dto);
                    sentDataCounter.increment();
                    logger.debug("Sent: {}", dto);
                } catch (Exception e) {
                    logger.error("Failed to send data: {}", e.getMessage());
                }
            }
        }
    }

    private double generateValue(String type) {
        switch (type) {
            case "TEMPERATURE": return 20 + random.nextDouble() * 20; // 20–40°C
            case "HUMIDITY": return 30 + random.nextDouble() * 60; // 30–90%
            case "MOTION": return random.nextDouble() < 0.2 ? 1.0 : 0.0; // 20% chance
            default: return 0;
        }
    }

    private String unitFor(String type) {
        switch (type) {
            case "TEMPERATURE": return "°C";
            case "HUMIDITY": return "%";
            case "MOTION": return "binary";
            default: return "";
        }
    }

    private boolean shouldDisconnect() {
        return random.nextDouble() < 0.1;
    }

    private boolean shouldReconnect() {
        return random.nextDouble() < 0.5;
    }

    private boolean shouldInjectAnomaly() {
        return random.nextDouble() < 0.05;
    }

    private double generateAnomalousValue(String type) {
        switch (type) {
            case "TEMPERATURE": return random.nextBoolean() ? -50.0 : 150.0;
            case "HUMIDITY": return random.nextBoolean() ? 0.0 : 120.0;
            case "MOTION": return 2.0;
            default: return 9999.0;
        }
    }

    private String pickDeviceType(List<String> sensorTypes) {
        if (sensorTypes == null || sensorTypes.isEmpty()) return "UNKNOWN";
        return sensorTypes.get(0); // assign first type as "deviceType"
    }

    private record DeviceRegistrationPayload(
            Long deviceId,
            String deviceName,
            String deviceType
    ) {}
}
