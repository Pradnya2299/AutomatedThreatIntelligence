package com.threatadvisor.ai.coderemediation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.util.UUID;

@Entity
@Table(name = "patch_changes")
public class PatchChangeEntity {
    @Id
    private UUID id;
    @Column(name = "patch_execution_id", nullable = false)
    private UUID patchExecutionId;
    @Column(name = "file_path", nullable = false, length = 1024)
    private String filePath;
    @Column(name = "change_type", nullable = false, length = 32)
    private String changeType;
    @Column(name = "before_excerpt")
    private String beforeExcerpt;
    @Column(name = "after_excerpt")
    private String afterExcerpt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getPatchExecutionId() { return patchExecutionId; }
    public void setPatchExecutionId(UUID patchExecutionId) { this.patchExecutionId = patchExecutionId; }
    public String getFilePath() { return filePath; }
    public void setFilePath(String filePath) { this.filePath = filePath; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public String getBeforeExcerpt() { return beforeExcerpt; }
    public void setBeforeExcerpt(String beforeExcerpt) { this.beforeExcerpt = beforeExcerpt; }
    public String getAfterExcerpt() { return afterExcerpt; }
    public void setAfterExcerpt(String afterExcerpt) { this.afterExcerpt = afterExcerpt; }
}
