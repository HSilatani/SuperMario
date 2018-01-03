package com.dario.agenttrader.dto;


import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class PositionInfo {
    private Map<String,String> updateInfo;
    private String s;
    private int i;

    public static final String DEAL_ID_KEY = "dealId";
    public static final String STOP_LEVE_KEY = "stopLevel";

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
        return updateInfo.get(STOP_LEVE_KEY);
    }

    public Map<String,String> getKeyValues(){
        return updateInfo;
    }
}
