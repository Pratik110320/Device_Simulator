package com.pratik.deviceSimulator.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity

@Table(name = "device")
public class Device {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long deviceId;
    private String deviceName;
    private String deviceType;
    private LocalDateTime registeredAt;
    private LocalDateTime lastActiveAt;

    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status{
        ONLINE, OFFLINE, DISCONNECTED , UNKNOWN
    }
    protected Device() {
    }
    public Device(Long deviceId, String deviceName, String deviceType, LocalDateTime registeredAt, LocalDateTime lastActiveAt, Status status) {
        this.deviceId = deviceId;
        this.deviceName = deviceName;
        this.deviceType = deviceType;
        this.registeredAt = registeredAt;
        this.lastActiveAt = lastActiveAt;
        this.status = status;
    }

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

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }
}
