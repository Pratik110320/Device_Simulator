package com.pratik.deviceSimulator.dto;



import com.pratik.deviceSimulator.model.Device;

import java.time.LocalDateTime;


public class DeviceResponseDto {
    private Long deviceId;
    private String deviceName;
    private String deviceType;
    private LocalDateTime registeredAt;
    private LocalDateTime lastActiveAt;
    private Device.Status status;

    public Long getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(Long deviceId) {
        this.deviceId = deviceId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }

    public LocalDateTime getLastActiveAt() {
        return lastActiveAt;
    }

    public void setLastActiveAt(LocalDateTime lastActiveAt) {
        this.lastActiveAt = lastActiveAt;
    }

    public Device.Status getStatus() {
        return status;
    }

    public void setStatus(Device.Status status) {
        this.status = status;
    }
}
