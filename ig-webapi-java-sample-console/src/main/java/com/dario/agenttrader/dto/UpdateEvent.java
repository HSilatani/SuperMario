package com.dario.agenttrader.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class UpdateEvent {
    public static final String EPIV_KEY="epic";
    public static Map<String,Integer> MARKET_UPDATE_KEYS = new HashMap<>();

    static {
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_DAY_OPEN_MID_KEY,5);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_DAY_LOW_KEY,8);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_DAY_HIGH_KEY,7);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_LTP_KEY,2);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_OFR_KEY,1);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_BID_KEY,0);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_DAY_PERC_CHG_MID_KEY,6);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_LTV_KEY,3);
        MARKET_UPDATE_KEYS.put(MarketInfo.MARKET_UPDATE_UTM_KEY,4);
        MARKET_UPDATE_KEYS = Collections.unmodifiableMap(MARKET_UPDATE_KEYS);
    }


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
