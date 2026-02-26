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

    /**
     * Loads a new program into the simulator
     * @param body the user code to load into the program
     * @return a LoadResponseDto with important information indicating success or failure
     */
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

    /**
     * Executes a single step of the program
     * @return a StepResponseDto with information about the current state of the program
     */
    @PostMapping("/step")
    public ResponseEntity<StepResponseDto> step() {
        StepResponseDto result = service.step();
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Runs the program to completion
     * @return a RunResponseDto with information about the final state of the program
     */
    @PostMapping("/run")
    public ResponseEntity<RunResponseDto> run() {
        RunResponseDto result = service.runToEnd();
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Resets the simulator to its initial state
     * @return a StateDto with the current state of the simulator
     */
    @PostMapping("/reset")
    public ResponseEntity<StateDto> reset() {
        return ResponseEntity.ok(service.reset());
    }

    /**
     * Provides input to the program after a scanf call
     * @param body the input to provide
     * @return a StepResponseDto with information about the current state of the program
     */
    @PostMapping("/input")
    public ResponseEntity<StepResponseDto> input(@RequestBody Map<String, String> body) {
        String value = body.getOrDefault("value", "");
        StepResponseDto result = service.provideInput(value);
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Gets the current state of the simulator
     * @return a StateDto with the current state of the simulator
     */
    @GetMapping("/state")
    public ResponseEntity<StateDto> getState() {
        return ResponseEntity.ok(service.getState());
    }

    /**
     * Scrolls the stack by the given direction
     * @param body the direction to scroll the stack
     * @return a StateDto with the updated state of the simulator
     */
    @PostMapping("/stack/scroll")
    public ResponseEntity<StateDto> scrollStack(@RequestBody Map<String, Integer> body) {
        int direction = body.getOrDefault("direction", 0);
        return ResponseEntity.ok(service.scrollStack(direction));
    }

    /**
     * Tracks a memory address for manual inspection
     * @param body the memory address to track
     * @return a StateDto with the updated state of the simulator
     */
    @PostMapping("/memory/track")
    public ResponseEntity<StateDto> trackMemory(@RequestBody Map<String, String> body) {
        String address = body.getOrDefault("address", "");
        return ResponseEntity.ok(service.trackMemoryAddress(address));
    }
}