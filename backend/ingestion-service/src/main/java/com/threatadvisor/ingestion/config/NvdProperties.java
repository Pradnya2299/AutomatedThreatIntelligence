package com.threatadvisor.ingestion.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nvd")
public class NvdProperties {

    private String apiBaseUrl = "https://services.nvd.nist.gov/rest/json/cves/2.0";
    private String apiKey = "";
    private boolean syncEnabled = false;
    private boolean lookupEnabled = true;
    private long syncIntervalMs = 3_600_000L;
    private int syncInitialLookbackHours = 24;
    private int resultsPerPage = 100;
    private int cacheMaxAgeHours = 24;
    private long requestTimeoutMs = 15_000L;
    private long minIntervalWithoutKeyMs = 6_000L;
    private long minIntervalWithKeyMs = 800L;

    public String getApiBaseUrl() {
        return apiBaseUrl;
    }

    public void setApiBaseUrl(String apiBaseUrl) {
        this.apiBaseUrl = apiBaseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isSyncEnabled() {
        return syncEnabled;
    }

    public void setSyncEnabled(boolean syncEnabled) {
        this.syncEnabled = syncEnabled;
    }

    public boolean isLookupEnabled() {
        return lookupEnabled;
    }

    public void setLookupEnabled(boolean lookupEnabled) {
        this.lookupEnabled = lookupEnabled;
    }

    public long getSyncIntervalMs() {
        return syncIntervalMs;
    }

    public void setSyncIntervalMs(long syncIntervalMs) {
        this.syncIntervalMs = syncIntervalMs;
    }

    public int getSyncInitialLookbackHours() {
        return syncInitialLookbackHours;
    }

    public void setSyncInitialLookbackHours(int syncInitialLookbackHours) {
        this.syncInitialLookbackHours = syncInitialLookbackHours;
    }

    public int getResultsPerPage() {
        return resultsPerPage;
    }

    public void setResultsPerPage(int resultsPerPage) {
        this.resultsPerPage = resultsPerPage;
    }

    public int getCacheMaxAgeHours() {
        return cacheMaxAgeHours;
    }

    public void setCacheMaxAgeHours(int cacheMaxAgeHours) {
        this.cacheMaxAgeHours = cacheMaxAgeHours;
    }

    public long getRequestTimeoutMs() {
        return requestTimeoutMs;
    }

    public void setRequestTimeoutMs(long requestTimeoutMs) {
        this.requestTimeoutMs = requestTimeoutMs;
    }

    public long getMinIntervalWithoutKeyMs() {
        return minIntervalWithoutKeyMs;
    }

    public void setMinIntervalWithoutKeyMs(long minIntervalWithoutKeyMs) {
        this.minIntervalWithoutKeyMs = minIntervalWithoutKeyMs;
    }

    public long getMinIntervalWithKeyMs() {
        return minIntervalWithKeyMs;
    }

    public void setMinIntervalWithKeyMs(long minIntervalWithKeyMs) {
        this.minIntervalWithKeyMs = minIntervalWithKeyMs;
    }

    public boolean hasApiKey() {
        return apiKey != null && !apiKey.isBlank();
    }

    public long minRequestIntervalMs() {
        return hasApiKey() ? minIntervalWithKeyMs : minIntervalWithoutKeyMs;
    }
}
