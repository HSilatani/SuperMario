package com.dario.agenttrader.dto;

public class DealConfirmation {
    private String dealRef;
    private String dealId;
    private String status;

    public DealConfirmation(String dealRef, String dealId, String status) {
        this.dealRef = dealRef;
        this.dealId = dealId;
        this.status = status;
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
}
