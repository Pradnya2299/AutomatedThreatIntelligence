package com.threatadvisor.ingestion.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "nvd_sync_state")
public class NvdSyncState {

    @Id
    @Column(length = 32)
    private String id = "default";

    @Column(name = "last_successful_sync_at")
    private Instant lastSuccessfulSyncAt;

    @Column(name = "last_mod_start_at")
    private Instant lastModStartAt;

    @Column(name = "last_mod_end_at")
    private Instant lastModEndAt;

    @Column(name = "last_attempt_at")
    private Instant lastAttemptAt;

    @Column(name = "last_status", length = 32)
    private String lastStatus;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "created_count", nullable = false)
    private int createdCount;

    @Column(name = "updated_count", nullable = false)
    private int updatedCount;

    @Column(name = "unchanged_count", nullable = false)
    private int unchangedCount;

    @Column(name = "page_count", nullable = false)
    private int pageCount;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Instant getLastSuccessfulSyncAt() { return lastSuccessfulSyncAt; }
    public void setLastSuccessfulSyncAt(Instant lastSuccessfulSyncAt) { this.lastSuccessfulSyncAt = lastSuccessfulSyncAt; }
    public Instant getLastModStartAt() { return lastModStartAt; }
    public void setLastModStartAt(Instant lastModStartAt) { this.lastModStartAt = lastModStartAt; }
    public Instant getLastModEndAt() { return lastModEndAt; }
    public void setLastModEndAt(Instant lastModEndAt) { this.lastModEndAt = lastModEndAt; }
    public Instant getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Instant lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public String getLastStatus() { return lastStatus; }
    public void setLastStatus(String lastStatus) { this.lastStatus = lastStatus; }
    public String getLastError() { return lastError; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public int getCreatedCount() { return createdCount; }
    public void setCreatedCount(int createdCount) { this.createdCount = createdCount; }
    public int getUpdatedCount() { return updatedCount; }
    public void setUpdatedCount(int updatedCount) { this.updatedCount = updatedCount; }
    public int getUnchangedCount() { return unchangedCount; }
    public void setUnchangedCount(int unchangedCount) { this.unchangedCount = unchangedCount; }
    public int getPageCount() { return pageCount; }
    public void setPageCount(int pageCount) { this.pageCount = pageCount; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
