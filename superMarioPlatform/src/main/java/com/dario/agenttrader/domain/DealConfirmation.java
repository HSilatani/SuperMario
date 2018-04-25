package com.dario.agenttrader.domain;

public class DealConfirmation {
    public static final String DEAL_REFERENCE = "dealReference";
    public static final String DEAL_ID = "dealId";
    public static final String DEAL_STATUS = "dealStatus";
    public static final String DEAL_EPIC = "epic";
    public static final String STATUS_ACEPTED = "ACCEPTED";
    public static final String STATUS_REJECTED = "REJECTED";
    public static final String DEAL_REASON = "reason";

    private String dealRef;
    private String dealId;
    private String status;
    private String epic;
    public String reason;

    public DealConfirmation(String dealRef, String dealId, String status) {
        this("NA",dealRef,dealId,status);
    }
    public DealConfirmation(String epic,String dealRef, String dealId, String status) {
        this(epic,dealRef,dealId,status,"");
    }
    public DealConfirmation(String epic,String dealRef, String dealId, String status,String reason) {
        this.dealRef = dealRef;
        this.dealId = dealId;
        this.status = status;
        this.epic = epic;
        this.reason = reason;
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

    public String getEpic()
    {
        return epic;
    }

    public String getReason() {
        return reason;
    }

    public boolean isAccepted() {
        boolean isAccepted = STATUS_ACEPTED.equalsIgnoreCase(getStatus());
        return isAccepted;
    }

    @Override
    public String toString() {
        return "DealConfirmation{" +
                "dealRef='" + dealRef + '\'' +
                ", dealId='" + dealId + '\'' +
                ", status='" + status + '\'' +
                ", epic='" + epic + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
