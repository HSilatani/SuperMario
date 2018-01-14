package com.dario.agenttrader.dto;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class MarketUpdate {
    public static final String EPIV_KEY="epic";
    public static Map<String,Integer> MARKET_UPDATE_KEYS = new HashMap<>();
    {
        MARKET_UPDATE_KEYS.put("MARKET_UPDATE_DAY_OPEN_MID_KEY" ,5);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_DAY_LOW_KEY",8);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_DAY_HIGH_KEY",7);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_LTP_KEY",2);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_OFR_KEY",1);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_BID_KEY",0);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_DAY_PERC_CHG_MID_KEY",6);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_LTV_KEY",3);
        MARKET_UPDATE_KEYS.put( "MARKET_UPDATE_UTM_KEY",4);
        MARKET_UPDATE_KEYS = Collections.unmodifiableMap(MARKET_UPDATE_KEYS);
    }


    Map<String,String> updates;

    public MarketUpdate(Map<String,String> pupdates){
        this.updates = pupdates;
    }

    public Map<String, String> getUpdates() {
        return updates;
    }
}
