package com.dario.agenttrader.dto;


import java.util.Map;

public class PositionUpdate {
    private Map<String,String> updateInfo;
    private String s;
    private int i;

    public static final String DEAL_ID_KEY = "dealId";
    public static final String STOP_LEVE_KEY = "stopLevel";

    public PositionUpdate(Map<String,String> updateInfo, String s, int i) {
        this.updateInfo = updateInfo;
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
}
