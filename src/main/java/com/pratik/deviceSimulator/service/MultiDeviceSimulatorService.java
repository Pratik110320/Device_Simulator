package com.pratik.deviceSimulator.service;

import com.pratik.deviceSimulator.config.SimulatorConfig;
import com.pratik.deviceSimulator.dto.DeviceResponseDto;
import com.pratik.deviceSimulator.dto.SensorRegistrationDto;
import com.pratik.deviceSimulator.model.SimulatedDevice;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.*;
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

    // keep this; your WebSocketClientService provides sendSensorData(dto) (your logs show it)
    private final WebSocketClientService webSocketClientService;

    public MultiDeviceSimulatorService(SimulatorConfig config,
                                       RestTemplate restTemplate,
                                       Random random,
                                       MeterRegistry meterRegistry,
                                       WebSocketClientService webSocketClientService) {
        this.config = Objects.requireNonNull(config, "SimulatorConfig required");
        this.restTemplate = Objects.requireNonNull(restTemplate, "RestTemplate required");
        this.random = Objects.requireNonNull(random, "Random required");
        this.meterRegistry = Objects.requireNonNull(meterRegistry, "MeterRegistry required");
        this.webSocketClientService = Objects.requireNonNull(webSocketClientService, "WebSocketClientService required");

        this.anomalyCounter = meterRegistry.counter("simulator.anomalies");
        this.sentDataCounter = meterRegistry.counter("simulator.data.sent");
        this.disconnectionCounter = meterRegistry.counter("simulator.disconnected");
        this.reconnectionCounter = meterRegistry.counter("simulator.reconnected");
    }

    private boolean waitForAnalyserUp(String healthUrl, int maxAttempts, long delayMs) {
        for (int i = 1; i <= maxAttempts; i++) {
            try {
                logger.info("Pinging analyser (attempt {}/{}): {}", i, maxAttempts, healthUrl);
                ResponseEntity<String> resp = restTemplate.getForEntity(healthUrl, String.class);
                if (resp != null && resp.getStatusCode().is2xxSuccessful()) {
                    logger.info("Analyser is up (200 OK).");
                    return true;
                }
            } catch (Exception e) {
                logger.debug("Analyser ping failed (attempt {}): {}", i, e.getMessage());
            }
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                logger.warn("Interrupted while waiting for analyser", ie);
                return false;
            }
        }
        return false;
    }

    private Long registerOneDevice(Long localDeviceId, String deviceType, String deviceRegistrationUrl) {
        Map<String, Object> body = new HashMap<>();
        body.put("deviceName", "Device-" + localDeviceId);
        body.put("deviceType", deviceType);

        int tries = 0;
        while (tries < 5) {
            tries++;
            try {
                logger.info("Registering device -> URL: {}, payload: {} (try {})", deviceRegistrationUrl, body, tries);
                ResponseEntity<DeviceResponseDto> resp = restTemplate.postForEntity(deviceRegistrationUrl, body, DeviceResponseDto.class);
                if (resp != null && resp.getStatusCode() == HttpStatus.CREATED && resp.getBody() != null) {
                    Long assigned = resp.getBody().getDeviceId();
                    logger.info("Device {} registration OK -> analyser-assigned id {}", localDeviceId, assigned);
                    return assigned;
                } else {
                    logger.warn("Device {} registration returned unexpected response: {}", localDeviceId, resp);
                }
            } catch (RestClientException e) {
                logger.warn("Device {} registration attempt {} failed: {}", localDeviceId, tries, e.toString());
            }

            try {
                Thread.sleep(400);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        logger.error("Device {} registration failed after {} tries", localDeviceId, tries);
        return null;
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

        // build urls (use getTargetUrl() - no extra getter required)
        final String deviceRegistrationUrl = (config.getDeviceRegistrationUrl() != null && !config.getDeviceRegistrationUrl().isBlank()) ? config.getDeviceRegistrationUrl() : config.getTargetUrl().replace("/sensor", "/device");
        final String healthUrl = config.getTargetUrl().replace("/sensor", "/actuator/health");

        if (!waitForAnalyserUp(healthUrl, 20, 500)) {
            throw new IllegalStateException("Analyser health endpoint not reachable at " + healthUrl);
        }

        // register devices and capture assigned IDs
        for (SimulatedDevice d : devices) {
            String deviceType = pickDeviceType(d.getSensorTypes());
            Long assigned = registerOneDevice(d.getId(), deviceType, deviceRegistrationUrl);
            if (assigned != null) {
                d.setRegisteredId(assigned); // store analyser id
            } else {
                logger.warn("Proceeding without assigned id for local device {}", d.getId());
            }
        }

        // WebSocket connect step is optional. Some projects don't expose a connect() method.
        // To avoid compile errors we do NOT call webSocketClientService.connect() here.
        // If your WebSocketClientService does provide connect(), add it back or implement it.
    }

    public void startSimulation() { this.simulationEnabled = true; }
    public void stopSimulation() { this.simulationEnabled = false; }
    public boolean isSimulationEnabled() { return simulationEnabled; }

    public void disconnectDevice(Long deviceId) {
        devices.stream().filter(d -> d.getId().equals(deviceId)).findFirst().ifPresent(d -> d.setConnected(false));
    }

    public void reconnectDevice(Long deviceId) {
        devices.stream().filter(d -> d.getId().equals(deviceId)).findFirst().ifPresent(d -> d.setConnected(true));
    }

    public void injectAnomalyToDevice(Long deviceId) {
        devices.stream()
                .filter(d -> d.getId().equals(deviceId) && d.isConnected())
                .findFirst()
                .ifPresent(d -> {
                    for (String type : d.getSensorTypes()) {
                        double anomaly = generateAnomalousValue(type);
                        Long targetDeviceId = d.getRegisteredId() != null ? d.getRegisteredId() : d.getId();
                        SensorRegistrationDto dto = new SensorRegistrationDto(targetDeviceId, anomaly, type, unitFor(type), true);
                        try {
                            restTemplate.postForEntity(config.getTargetUrl(), dto, Void.class);
                            logger.warn("[MANUAL ANOMALY] localId {} targetId {} Type {} => {}", d.getId(), targetDeviceId, type, anomaly);
                            anomalyCounter.increment();
                        } catch (Exception e) {
                            logger.error("Failed to send manual anomaly for device {}: {}", d.getId(), e.getMessage(), e);
                        }
                    }
                });
    }

    @Scheduled(fixedRateString = "${simulator.data-push-interval:5000}")
    public void pushSensorData() {
        if (!simulationEnabled) {
            logger.debug("Simulation paused");
            return;
        }
        logger.debug(">>> pushSensorData() @ {}", Instant.now());

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

            if (device.getSensorTypes() == null || device.getSensorTypes().isEmpty()) continue;

            String type = device.getSensorTypes().get(random.nextInt(device.getSensorTypes().size()));
            Long targetDeviceId = device.getRegisteredId() != null ? device.getRegisteredId() : device.getId();

            SensorRegistrationDto dto;
            if (device.isConnected()) {
                boolean injected = shouldInjectAnomaly();
                double value = injected ? generateAnomalousValue(type) : generateValue(type);
                dto = new SensorRegistrationDto(targetDeviceId, value, type, unitFor(type), true);
                if (injected) {
                    logger.warn("[ANOMALY] localId {} targetId {} - Type: {} - Value: {}", device.getId(), targetDeviceId, type, value);
                    anomalyCounter.increment();
                }
            } else {
                dto = new SensorRegistrationDto(targetDeviceId, Double.NaN, type, unitFor(type), false);
            }

            try {
                restTemplate.postForEntity(config.getTargetUrl(), dto, Void.class);
                // best-effort websocket broadcast (your WebSocketClientService must expose sendSensorData)
                try {
                    webSocketClientService.sendSensorData(dto);
                } catch (Throwable wsEx) {
                    // keep going if websocket fails - it's optional
                    logger.debug("WebSocket send error (non-fatal): {}", wsEx.getMessage());
                }
                sentDataCounter.increment();
                logger.debug("Sent sensor DTO for localId {} targetId {}: {}", device.getId(), targetDeviceId, dto);
            } catch (Exception e) {
                logger.error("Failed to send data for local device {} targetId {}: {}", device.getId(), targetDeviceId, e.getMessage());
            }
        }
    }

    /* helpers */
    private double generateValue(String type) {
        return switch (type) {
            case "TEMPERATURE" -> 20 + random.nextDouble() * 20;
            case "HUMIDITY" -> 30 + random.nextDouble() * 60;
            case "MOTION" -> random.nextDouble() < 0.2 ? 1.0 : 0.0;
            default -> 0;
        };
    }

    private String unitFor(String type) {
        return switch (type) {
            case "TEMPERATURE" -> "°C";
            case "HUMIDITY" -> "%";
            case "MOTION" -> "binary";
            default -> "";
        };
    }

    private boolean shouldDisconnect() { return random.nextDouble() < 0.1; }
    private boolean shouldReconnect() { return random.nextDouble() < 0.5; }
    private boolean shouldInjectAnomaly() { return random.nextDouble() < 0.05; }

    private double generateAnomalousValue(String type) {
        return switch (type) {
            case "TEMPERATURE" -> random.nextBoolean() ? -50.0 : 150.0;
            case "HUMIDITY" -> random.nextBoolean() ? 0.0 : 120.0;
            case "MOTION" -> 2.0;
            default -> 9999.0;
        };
    }

    private String pickDeviceType(List<String> sensorTypes) {
        if (sensorTypes == null || sensorTypes.isEmpty()) return "UNKNOWN";
        return sensorTypes.get(0);
    }
}
