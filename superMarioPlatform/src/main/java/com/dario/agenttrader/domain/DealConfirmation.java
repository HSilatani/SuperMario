package com.dario.agenttrader.domain;

public class DealConfirmation {
    public static final String DEAL_REFERENCE = "dealReference";
    public static final String DEAL_ID = "dealId";
    public static final String DEAL_STATUS = "dealStatus";
    public static final String DEAL_EPIC = "epic";
    public static final String STATUS_ACEPTED = "ACEPTED";
    public static final String STATUS_REJECTED = "REJECTED";

    private String dealRef;
    private String dealId;
    private String status;
    private String epic;

    public DealConfirmation(String dealRef, String dealId, String status) {
        this("NA",dealRef,dealId,status);
    }
    public DealConfirmation(String epic,String dealRef, String dealId, String status) {
        this.dealRef = dealRef;
        this.dealId = dealId;
        this.status = status;
        this.epic = epic;
    }

    public String getDealRef() {
        return dealRef;
    }

    public String getDealId() {
        return dealId;
    }

    public String getStatus() {
        return status;
    }

    public String getEpic() {
        return epic;
    }

    public boolean isAccepted() {
        boolean isAccepted = STATUS_ACEPTED.equalsIgnoreCase(getStatus());
        return isAccepted;
    }
}
