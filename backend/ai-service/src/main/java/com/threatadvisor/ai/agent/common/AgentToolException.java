package com.threatadvisor.ai.agent.common;

public class AgentToolException extends RuntimeException {
    private final String code;

    public AgentToolException(String code, String message) {
        super(message);
        this.code = code;
    }

    public AgentToolException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
