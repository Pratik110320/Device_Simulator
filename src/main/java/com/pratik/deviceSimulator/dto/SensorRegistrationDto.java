package com.pratik.deviceSimulator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;

public class SensorRegistrationDto {
    private Long deviceId; // 🚨 Needed for mapping
    private Double value;
    private String sensorType;
    private String unit;
    private boolean connected; // optional, useful for diagnostics
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp; // optional but highly recommended

    public SensorRegistrationDto() {}
    @Override
    public String toString() {
        return "SensorRegistrationDto{" +
                "deviceId=" + deviceId +
                ", type=" + sensorType +
                ", value=" + value +
                '}';
    }

    public SensorRegistrationDto(Long deviceId, Double value, String sensorType, String unit, boolean connected) {
        this.deviceId = deviceId;
        this.value = value;
        this.sensorType = sensorType;
        this.unit = unit;
        this.connected = connected;
        this.timestamp = LocalDateTime.now(); // set upon creation
    }

    // Getters and Setters

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }

    public String getSensorType() {
        return sensorType;
    }

    public void setSensorType(String sensorType) {
        this.sensorType = sensorType;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
}


