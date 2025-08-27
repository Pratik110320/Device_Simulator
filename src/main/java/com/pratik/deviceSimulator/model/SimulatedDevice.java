package com.pratik.deviceSimulator.model;

import java.util.List;

public class SimulatedDevice {

    private Long id;
    private Long registeredId;
    private List<String> sensorTypes;
    private boolean connected;
    public SimulatedDevice(Long id, List<String> sensorTypes) {
        this.id = id;
        this.sensorTypes = sensorTypes;
        this.connected = true;

    }

    public Long getRegisteredId() {
        return registeredId;
    }

    public void setRegisteredId(Long registeredId) {
        this.registeredId = registeredId;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setSensorTypes(List<String> sensorTypes) {
        this.sensorTypes = sensorTypes;
    }

    public Long getId() {
        return id;
    }

    public List<String> getSensorTypes() {
        return sensorTypes;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }

    @Override
    public String toString() {
        return "SimulatedDevice{id=" + id + ", sensorTypes=" + sensorTypes + ", connected=" + connected + '}';
    }
}
