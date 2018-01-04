package com.dario.agenttrader.dto;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PositionInfo {
    public static final String CREATED_DATE_KEY = "createdDate";
    public static final String CREATED_DATE_UTC_KEY = "createdDateUTC";
    public static final String CURRENCY_KEY = "currency";
    public static final String CONTRACT_SIZE_KEY ="contractSize" ;
    public static final String CONTROLLED_RISK_KEY = "controlledRisk";
    public static final String DIRECTION_KEY = "direction";
    public static final String LEVEL_KEY = "level";
    public static final String LIMIT_LEVEL_KEY = "limitLevel";
    public static final String SIZE_KEY = "size";
    public static final String STOP_LEVEL_KEY = "stopLevel";
    public static final String TRAILING_STEP_KEY = "trailingStep";
    public static final String TRAILING_STOP_DISTANCE_KEY = "trailingStopDistance";
    public static final String GUARANTEEDSTOP_KEY = "guaranteedStop";
    public static final String EXPIRY_KEY = "expiry";
    public static final String ORIGINAL_DEAL_ID_KEY = "dealIdOrigin";
    public static final String DEAL_ID_KEY = "dealId";
    public static final String DEAL_REF_KEY = "dealReference";
    public static final String EPIC_KEY = "epic";
    public static final String DEAL_STATUS_KEY = "dealStatus";
    public static final String STATUS_KEY = "status";
    public static final String CHANNEL_KEY = "channel";
    public static final String TIME_STAMP_KEY = "timestamp";







    private Map<String,String> updateInfo;
    private String s;
    private int i;


    public PositionInfo(Map<String,String> pupdateInfo, String s, int i) {
        Optional<Map<String,String>> optPUpdateInfo = Optional.ofNullable(pupdateInfo);

        this.updateInfo = Collections.unmodifiableMap(optPUpdateInfo.orElse(new HashMap<>()));
        this.s = s;
        this.i = i;
    }

    public String getValue(String key) {
        return updateInfo.get(key);
    }

    public String getDealId(){
        return updateInfo.get(DEAL_ID_KEY);
    }

    public String getS() {
        return s;
    }

    public int getI() {
        return i;
    }

    public String getStop() {
        return updateInfo.get(STOP_LEVEL_KEY);
    }

    public Map<String,String> getKeyValues(){
        return updateInfo;
    }

}
