package com.pratik.deviceSimulator.controller;

import com.pratik.deviceSimulator.service.MultiDeviceSimulatorService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/simulator")
@CrossOrigin(origins = "*")
public class SimulatorAdminController {

    private final MultiDeviceSimulatorService simulatorService;

    public SimulatorAdminController(MultiDeviceSimulatorService simulatorService) {
        this.simulatorService = simulatorService;
    }

    @PostMapping("/start")
    public ResponseEntity<String> start() {
        simulatorService.startSimulation();
        return ResponseEntity.ok("Simulation started");
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stop() {
        simulatorService.stopSimulation();
        return ResponseEntity.ok("Simulation stopped");
    }

    @PostMapping("/disconnect/{id}")
    public ResponseEntity<String> disconnect(@PathVariable Long id) {
        simulatorService.disconnectDevice(id);
        return ResponseEntity.ok("Device " + id + " disconnected");
    }

    @PostMapping("/reconnect/{id}")
    public ResponseEntity<String> reconnect(@PathVariable Long id) {
        simulatorService.reconnectDevice(id);
        return ResponseEntity.ok("Device " + id + " reconnected");
    }

    @PostMapping("/inject-anomaly/{id}")
    public ResponseEntity<String> injectAnomaly(@PathVariable Long id) {
        simulatorService.injectAnomalyToDevice(id);
        return ResponseEntity.ok("Anomaly injected for Device " + id);
    }
}

