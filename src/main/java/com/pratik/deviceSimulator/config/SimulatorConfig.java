package com.pratik.deviceSimulator.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@ConfigurationProperties(prefix = "simulator")
public class SimulatorConfig {

    private int deviceCount;
    private List<String> sensorTypes;
    private String targetUrl;
    private String websocketUrl;
    public SimulatorConfig() {
    }

    public SimulatorConfig(int deviceCount, List<String> sensorTypes, String targetUrl) {
        this.deviceCount = deviceCount;
        this.sensorTypes = sensorTypes;
        this.targetUrl = targetUrl;
    }
    private String deviceRegistrationUrl; // optional override

    public String getDeviceRegistrationUrl() { return deviceRegistrationUrl; }
    public void setDeviceRegistrationUrl(String deviceRegistrationUrl) { this.deviceRegistrationUrl = deviceRegistrationUrl; }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public int getDeviceCount() {
        return deviceCount;
    }

    public void setDeviceCount(int deviceCount) {
        this.deviceCount = deviceCount;
    }

    public List<String> getSensorTypes() {
        return sensorTypes;
    }

    public void setSensorTypes(List<String> sensorTypes) {
        this.sensorTypes = sensorTypes;
    }

    public String getWebsocketUrl() {
        return websocketUrl;
    }

    public void setWebsocketUrl(String websocketUrl) {
        this.websocketUrl = websocketUrl;
    }
}
