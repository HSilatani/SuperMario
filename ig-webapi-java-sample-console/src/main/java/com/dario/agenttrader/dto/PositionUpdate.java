package com.dario.agenttrader.dto;

import com.lightstreamer.ls_client.UpdateInfo;

public class PositionUpdate {
    private UpdateInfo updateInfo;
    private String s;
    private int i;

    public PositionUpdate(UpdateInfo updateInfo, String s, int i) {
        this.updateInfo = updateInfo;
        this.s = s;
        this.i = i;
    }

    public UpdateInfo getUpdateInfo() {
        return updateInfo;
    }

    public String getS() {
        return s;
    }

    public int getI() {
        return i;
    }

    public String getItemName(){
        return updateInfo.getItemName();
    }
}
