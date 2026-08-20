package com.threatadvisor.ai.coderemediation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "validation_runs")
public class ValidationRunEntity {
    @Id
    private UUID id;
    @Column(name = "patch_execution_id", nullable = false)
    private UUID patchExecutionId;
    @Column(nullable = false, length = 1024)
    private String command;
    @Column(name = "exit_code")
    private Integer exitCode;
    @Column(name = "duration_ms")
    private Long durationMs;
    @Column(name = "stdout_summary")
    private String stdoutSummary;
    @Column(name = "stderr_summary")
    private String stderrSummary;
    @Column(nullable = false, length = 32)
    private String status;
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPatchExecutionId() { return patchExecutionId; }
    public void setPatchExecutionId(UUID patchExecutionId) { this.patchExecutionId = patchExecutionId; }
    public String getCommand() { return command; }
    public void setCommand(String command) { this.command = command; }
    public Integer getExitCode() { return exitCode; }
    public void setExitCode(Integer exitCode) { this.exitCode = exitCode; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
    public String getStdoutSummary() { return stdoutSummary; }
    public void setStdoutSummary(String stdoutSummary) { this.stdoutSummary = stdoutSummary; }
    public String getStderrSummary() { return stderrSummary; }
    public void setStderrSummary(String stderrSummary) { this.stderrSummary = stderrSummary; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
