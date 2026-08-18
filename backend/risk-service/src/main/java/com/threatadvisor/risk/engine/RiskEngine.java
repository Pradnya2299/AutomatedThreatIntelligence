package com.threatadvisor.risk.engine;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Phase 2D hackathon risk formula (no LLM).
 *
 * <pre>
 * riskScore =
 *     cvss              * 0.40
 *   + assetCriticality  * 0.25
 *   + internetExposure  * 0.15
 *   + exploitability    * 0.10
 *   + activeExploitation* 0.10
 * </pre>
 *
 * Each factor is 0–100. Result is rounded to two decimal places.
 */
public final class RiskEngine {

    public static final String FORMULA_VERSION = "v1";

    public static final BigDecimal WEIGHT_CVSS = new BigDecimal("0.40");
    public static final BigDecimal WEIGHT_CRITICALITY = new BigDecimal("0.25");
    public static final BigDecimal WEIGHT_EXPOSURE = new BigDecimal("0.15");
    public static final BigDecimal WEIGHT_EXPLOIT = new BigDecimal("0.10");
    public static final BigDecimal WEIGHT_ACTIVE = new BigDecimal("0.10");

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");
    private static final BigDecimal TEN = new BigDecimal("10");

    private RiskEngine() {
    }

    public static RiskResult calculate(RiskInput input) {
        BigDecimal cvss = cvss(input.cvssBase());
        BigDecimal criticality = assetCriticality(input.businessCriticality());
        BigDecimal exposure = internetExposure(input.internetExposed());
        BigDecimal exploit = exploitability(input.exploitAvailable());
        BigDecimal active = activeExploitation(input.activelyExploited());
        BigDecimal score = cvss.multiply(WEIGHT_CVSS)
                .add(criticality.multiply(WEIGHT_CRITICALITY))
                .add(exposure.multiply(WEIGHT_EXPOSURE))
                .add(exploit.multiply(WEIGHT_EXPLOIT))
                .add(active.multiply(WEIGHT_ACTIVE))
                .setScale(2, RoundingMode.HALF_UP);
        String level = riskLevel(score);
        return new RiskResult(
                score,
                level,
                cvss,
                criticality,
                exposure,
                exploit,
                active,
                explanation(score, level, input, exposure, exploit, active));
    }

    /**
     * Linear map of the stored CVSS base (0.0–10.0) onto 0–100.
     * Qualitative CVSS bands (0.0–3.9 / 4.0–6.9 / 7.0–8.9 / 9.0–10.0) are not extra buckets.
     */
    public static BigDecimal cvss(BigDecimal base) {
        if (base == null) {
            return ZERO;
        }
        return clamp(base.multiply(TEN).setScale(2, RoundingMode.HALF_UP));
    }

    public static BigDecimal assetCriticality(String value) {
        if (value == null) {
            return ZERO;
        }
        return switch (value.trim().toUpperCase()) {
            case "LOW" -> new BigDecimal("25.00");
            case "MEDIUM" -> new BigDecimal("50.00");
            case "HIGH" -> new BigDecimal("75.00");
            case "CRITICAL" -> new BigDecimal("100.00");
            default -> ZERO;
        };
    }

    /**
     * Inventory only has {@code assets.internet_exposure} boolean.
     * TRUE → 100 (INTERNET). FALSE → 0 (NOT_EXPOSED). INTERNAL=50 is unused.
     */
    public static BigDecimal internetExposure(boolean internetFacing) {
        return internetFacing ? HUNDRED : ZERO;
    }

    /**
     * Inventory only has {@code exploit_available} boolean.
     * TRUE → 75 (AVAILABLE). FALSE → 0 (NONE). PROVEN=100 is unused (active exploit is a separate factor).
     */
    public static BigDecimal exploitability(Boolean exploitAvailable) {
        return Boolean.TRUE.equals(exploitAvailable) ? new BigDecimal("75.00") : ZERO;
    }

    public static BigDecimal activeExploitation(Boolean activelyExploited) {
        return Boolean.TRUE.equals(activelyExploited) ? HUNDRED : ZERO;
    }

    public static String riskLevel(BigDecimal score) {
        BigDecimal value = score == null ? ZERO : score;
        if (value.compareTo(new BigDecimal("25.00")) < 0) {
            return "LOW";
        }
        if (value.compareTo(new BigDecimal("50.00")) < 0) {
            return "MEDIUM";
        }
        if (value.compareTo(new BigDecimal("75.00")) < 0) {
            return "HIGH";
        }
        return "CRITICAL";
    }

    public static String explanation(
            BigDecimal score,
            String level,
            RiskInput input,
            BigDecimal exposure,
            BigDecimal exploit,
            BigDecimal active) {
        String cvssText = input.cvssBase() == null
                ? "no CVSS score"
                : "a CVSS score of " + input.cvssBase().stripTrailingZeros().toPlainString();
        String criticality = input.businessCriticality() == null ? "UNKNOWN" : input.businessCriticality().toUpperCase();
        StringBuilder text = new StringBuilder();
        text.append("Risk is ").append(level).append(" (").append(score.toPlainString()).append(")")
                .append(" because the vulnerability has ").append(cvssText)
                .append(", affects a ").append(criticality).append(" business asset");
        if (exposure.compareTo(ZERO) > 0) {
            text.append(", the asset is internet-facing");
        } else {
            text.append(", the asset is not internet-facing");
        }
        if (exploit.compareTo(ZERO) > 0) {
            text.append(", exploits are available");
        }
        if (active.compareTo(ZERO) > 0) {
            text.append(", and active exploitation is known");
        }
        text.append(".");
        return text.toString();
    }

    private static BigDecimal clamp(BigDecimal value) {
        if (value.compareTo(ZERO) < 0) {
            return ZERO;
        }
        if (value.compareTo(HUNDRED) > 0) {
            return HUNDRED;
        }
        return value;
    }
}
