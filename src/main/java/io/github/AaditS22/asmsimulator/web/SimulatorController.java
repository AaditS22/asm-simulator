package io.github.AaditS22.asmsimulator.web;

import io.github.AaditS22.asmsimulator.web.dto.LoadResponseDto;
import io.github.AaditS22.asmsimulator.web.dto.RunResponseDto;
import io.github.AaditS22.asmsimulator.web.dto.StateDto;
import io.github.AaditS22.asmsimulator.web.dto.StepResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class SimulatorController {

    private final SimulatorService service;

    public SimulatorController(SimulatorService service) {
        this.service = service;
    }

    @PostMapping("/load")
    public ResponseEntity<LoadResponseDto> load(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new LoadResponseDto(false, "No code provided", null));
        }
        LoadResponseDto result = service.load(code);
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/step")
    public ResponseEntity<StepResponseDto> step() {
        StepResponseDto result = service.step();
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/run")
    public ResponseEntity<RunResponseDto> run() {
        RunResponseDto result = service.runToEnd();
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @PostMapping("/reset")
    public ResponseEntity<StateDto> reset() {
        return ResponseEntity.ok(service.reset());
    }

    @PostMapping("/input")
    public ResponseEntity<StepResponseDto> input(@RequestBody Map<String, String> body) {
        String value = body.getOrDefault("value", "");
        StepResponseDto result = service.provideInput(value);
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    @GetMapping("/state")
    public ResponseEntity<StateDto> getState() {
        return ResponseEntity.ok(service.getState());
    }
}