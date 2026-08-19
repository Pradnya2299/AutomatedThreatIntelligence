package com.threatadvisor.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    private boolean demoMode = true;
    private String apiKey = "";
    private String chatModel = "gpt-4o-mini";
    private String embeddingModel = "text-embedding-3-small";
    private int topK = 5;
    private String promptVersion = "v1";
    private long cacheTtlSeconds = 3600;
    private boolean ingestOnStartup = true;
    private int chunkSizeChars = 2000;
    private int chunkOverlapChars = 200;
    private String correlationBaseUrl = "http://localhost:8082";
    private String riskBaseUrl = "http://localhost:8083";
    /**
     * Maximum orchestrator loop iterations (Phase 5B). Reaching the cap yields
     * {@code REVIEW_REQUIRED} rather than an infinite agent loop.
     */
    private int maxAgentIterations = 10;
    private Git git = new Git();
    private CodeRemediation codeRemediation = new CodeRemediation();

    public boolean isDemoMode() {
        return demoMode;
    }

    public void setDemoMode(boolean demoMode) {
        this.demoMode = demoMode;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getChatModel() {
        return chatModel;
    }

    public void setChatModel(String chatModel) {
        this.chatModel = chatModel;
    }

    public String getEmbeddingModel() {
        return embeddingModel;
    }

    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public int getTopK() {
        return topK;
    }

    public void setTopK(int topK) {
        this.topK = topK;
    }

    public String getPromptVersion() {
        return promptVersion;
    }

    public void setPromptVersion(String promptVersion) {
        this.promptVersion = promptVersion;
    }

    public long getCacheTtlSeconds() {
        return cacheTtlSeconds;
    }

    public void setCacheTtlSeconds(long cacheTtlSeconds) {
        this.cacheTtlSeconds = cacheTtlSeconds;
    }

    public boolean isIngestOnStartup() {
        return ingestOnStartup;
    }

    public void setIngestOnStartup(boolean ingestOnStartup) {
        this.ingestOnStartup = ingestOnStartup;
    }

    public int getChunkSizeChars() {
        return chunkSizeChars;
    }

    public void setChunkSizeChars(int chunkSizeChars) {
        this.chunkSizeChars = chunkSizeChars;
    }

    public int getChunkOverlapChars() {
        return chunkOverlapChars;
    }

    public void setChunkOverlapChars(int chunkOverlapChars) {
        this.chunkOverlapChars = chunkOverlapChars;
    }

    public String getCorrelationBaseUrl() {
        return correlationBaseUrl;
    }

    public void setCorrelationBaseUrl(String correlationBaseUrl) {
        this.correlationBaseUrl = correlationBaseUrl;
    }

    public String getRiskBaseUrl() {
        return riskBaseUrl;
    }

    public void setRiskBaseUrl(String riskBaseUrl) {
        this.riskBaseUrl = riskBaseUrl;
    }

    public int getMaxAgentIterations() {
        return maxAgentIterations;
    }

    public void setMaxAgentIterations(int maxAgentIterations) {
        this.maxAgentIterations = maxAgentIterations;
    }

    public Git getGit() {
        return git;
    }

    public void setGit(Git git) {
        this.git = git;
    }

    public CodeRemediation getCodeRemediation() {
        return codeRemediation;
    }

    public void setCodeRemediation(CodeRemediation codeRemediation) {
        this.codeRemediation = codeRemediation;
    }

    public static class Git {
        private boolean enabled = false;
        private String apiUrl = "https://api.github.com";
        private String token = "";
        private String organization = "";

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getApiUrl() { return apiUrl; }
        public void setApiUrl(String apiUrl) { this.apiUrl = apiUrl; }
        public String getToken() { return token; }
        public void setToken(String token) { this.token = token; }
        public String getOrganization() { return organization; }
        public void setOrganization(String organization) { this.organization = organization; }
    }

    public static class CodeRemediation {
        private int maxChangedFiles = 20;
        private int maxChangedLines = 400;
        private int maxPatchAttempts = 3;
        private boolean skipHostBuilds = true;

        public int getMaxChangedFiles() { return maxChangedFiles; }
        public void setMaxChangedFiles(int maxChangedFiles) { this.maxChangedFiles = maxChangedFiles; }
        public int getMaxChangedLines() { return maxChangedLines; }
        public void setMaxChangedLines(int maxChangedLines) { this.maxChangedLines = maxChangedLines; }
        public int getMaxPatchAttempts() { return maxPatchAttempts; }
        public void setMaxPatchAttempts(int maxPatchAttempts) { this.maxPatchAttempts = maxPatchAttempts; }
        public boolean isSkipHostBuilds() { return skipHostBuilds; }
        public void setSkipHostBuilds(boolean skipHostBuilds) { this.skipHostBuilds = skipHostBuilds; }
    }
}
