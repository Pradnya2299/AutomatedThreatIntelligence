package com.threatadvisor.correlation.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "correlation")
public class CorrelationProperties {

    /**
     * Persist MEDIUM confidence matches (vendor+product but incomplete version). Default false.
     */
    private boolean persistMediumConfidence = false;

    /**
     * Persist LOW confidence matches. Default false; the engine currently does not emit LOW.
     */
    private boolean persistLowConfidence = false;

    public boolean isPersistMediumConfidence() {
        return persistMediumConfidence;
    }

    public void setPersistMediumConfidence(boolean persistMediumConfidence) {
        this.persistMediumConfidence = persistMediumConfidence;
    }

    public boolean isPersistLowConfidence() {
        return persistLowConfidence;
    }

    public void setPersistLowConfidence(boolean persistLowConfidence) {
        this.persistLowConfidence = persistLowConfidence;
    }
}
