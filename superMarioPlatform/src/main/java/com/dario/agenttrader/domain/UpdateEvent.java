package com.dario.agenttrader.domain;

import java.util.Map;

public class UpdateEvent {
    public static final String EPIV_KEY="epic";


    Map<String,String> updates;
    public static final String POSITION_UPDATE="positionUpdate";
    public static final String MARKET_UPDATE="marketUpdate";

    private String updateType;

    public UpdateEvent(Map<String,String> pupdates,String pupdateType){
        this.updates = pupdates;
        updateType = pupdateType;
    }

    public boolean isPositionUpdate(){
        return POSITION_UPDATE.equalsIgnoreCase(updateType);
    }

    public boolean isMarketUpdate(){
        return MARKET_UPDATE.equalsIgnoreCase(updateType);
    }


    public Map<String, String> getUpdates() {
        return updates;
    }
}
