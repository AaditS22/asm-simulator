package io.github.AaditS22.asmsimulator.web;

import io.github.AaditS22.asmsimulator.web.dto.LoadResponseDto;
import io.github.AaditS22.asmsimulator.web.dto.RunResponseDto;
import io.github.AaditS22.asmsimulator.web.dto.StateDto;
import io.github.AaditS22.asmsimulator.web.dto.StepResponseDto;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
     * @param sessionId the session ID of the user
     * @param body the user code to load into the program
     * @return a LoadResponseDto with important information indicating success or failure
     */
    @PostMapping("/load")
    public ResponseEntity<LoadResponseDto> load(@RequestHeader("X-Session-Id") String sessionId,
                                                @RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(new LoadResponseDto(false, "No code provided", null));
        }
        LoadResponseDto result = service.load(sessionId, code);
        return result.success()
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Executes a single step of the program
     * @param sessionId the session ID of the user
     * @return a StepResponseDto with information about the current state of the program
     */
    @PostMapping("/step")
    public ResponseEntity<StepResponseDto> step(@RequestHeader("X-Session-Id") String sessionId) {
        StepResponseDto result = service.step(sessionId);
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Runs the program to completion
     * @param sessionId the session ID of the user
     * @return a RunResponseDto with information about the final state of the program
     */
    @PostMapping("/run")
    public ResponseEntity<RunResponseDto> run(@RequestHeader("X-Session-Id") String sessionId) {
        RunResponseDto result = service.runToEnd(sessionId);
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Resets the simulator to its initial state
     * @param sessionId the session ID of the user
     * @return a StateDto with the current state of the simulator
     */
    @PostMapping("/reset")
    public ResponseEntity<StateDto> reset(@RequestHeader("X-Session-Id") String sessionId) {
        return ResponseEntity.ok(service.reset(sessionId));
    }

    /**
     * Provides input to the program after a scanf call
     * @param sessionId the session ID of the user
     * @param body the input to provide
     * @return a StepResponseDto with information about the current state of the program
     */
    @PostMapping("/input")
    public ResponseEntity<StepResponseDto> input(@RequestHeader("X-Session-Id") String sessionId,
                                                 @RequestBody Map<String, String> body) {
        String value = body.getOrDefault("value", "");
        StepResponseDto result = service.provideInput(sessionId, value);
        return result.error() == null
                ? ResponseEntity.ok(result)
                : ResponseEntity.badRequest().body(result);
    }

    /**
     * Gets the current state of the simulator
     * @param sessionId the session ID of the user
     * @return a StateDto with the current state of the simulator
     */
    @GetMapping("/state")
    public ResponseEntity<StateDto> getState(@RequestHeader("X-Session-Id") String sessionId) {
        return ResponseEntity.ok(service.getState(sessionId));
    }

    /**
     * Scrolls the stack by the given direction
     * @param sessionId the session ID of the user
     * @param body the direction to scroll the stack
     * @return a StateDto with the updated state of the simulator
     */
    @PostMapping("/stack/scroll")
    public ResponseEntity<StateDto> scrollStack(@RequestHeader("X-Session-Id") String sessionId,
                                                @RequestBody Map<String, Integer> body) {
        int direction = body.getOrDefault("direction", 0);
        return ResponseEntity.ok(service.scrollStack(sessionId, direction));
    }

    /**
     * Tracks a memory address for manual inspection
     * @param sessionId the session ID of the user
     * @param body the memory address to track
     * @return a StateDto with the updated state of the simulator
     */
    @PostMapping("/memory/track")
    public ResponseEntity<StateDto> trackMemory(@RequestHeader("X-Session-Id") String sessionId,
                                                @RequestBody Map<String, String> body) {
        String address = body.getOrDefault("address", "");
        return ResponseEntity.ok(service.trackMemoryAddress(sessionId, address));
    }
}