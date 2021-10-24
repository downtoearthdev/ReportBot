package com.scorchedcode.ReportBot;

public enum ReportAction {
    WARN("Warned by"),
    BAN("Banned by"),
    LOCK("Locked by"),
    COMPLETE("Completed by"),
    DELETE("Message deleted by"),
    UNKNOWN("Under review");

    private String fancy;

    ReportAction(String fancy) {
        this.fancy = fancy;
    }

    public String getFancyPosessive() {
        return fancy;
    }
}
