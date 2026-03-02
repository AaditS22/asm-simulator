package io.github.AaditS22.asmsimulator.web;

import io.github.AaditS22.asmsimulator.web.dto.*;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class SimulatorService {

    private final Map<String, SimulatorSession> sessions = Collections.synchronizedMap(
            new LinkedHashMap<>(100, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, SimulatorSession> eldest) {
                    return size() > 1000;
                }
            }
    );

    private SimulatorSession getSession(String sessionId) {
        synchronized (sessions) {
            return sessions.computeIfAbsent(sessionId, k -> new SimulatorSession());
        }
    }

    public LoadResponseDto load(String sessionId, String code) {
        return getSession(sessionId).load(code);
    }

    public StepResponseDto step(String sessionId) {
        return getSession(sessionId).step();
    }

    public RunResponseDto runToEnd(String sessionId) {
        return getSession(sessionId).runToEnd();
    }

    public StateDto reset(String sessionId) {
        return getSession(sessionId).reset();
    }

    public StepResponseDto provideInput(String sessionId, String value) {
        return getSession(sessionId).provideInput(value);
    }

    public StateDto getState(String sessionId) {
        return getSession(sessionId).getState();
    }

    public StateDto scrollStack(String sessionId, int direction) {
        return getSession(sessionId).scrollStack(direction);
    }

    public StateDto trackMemoryAddress(String sessionId, String addressStr) {
        return getSession(sessionId).trackMemoryAddress(addressStr);
    }
}