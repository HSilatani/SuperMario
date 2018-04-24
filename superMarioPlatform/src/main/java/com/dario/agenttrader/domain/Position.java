package com.dario.agenttrader.domain;

import com.dario.agenttrader.domain.Direction;
import com.dario.agenttrader.utility.Calculator;

import java.time.Instant;

public final class Position {
    private String epic;
    private boolean confirmed;
    private String dealRef;
    private String dealId;
    private double size;
    private Direction direction;
    private Instant createdTime;

    public Position(String pepic, String dealRef, double size, Direction direction) {
       this(pepic,false,dealRef,null,size,direction,null);
    }
    public Position(String pepic, String dealRef, double size, Direction direction, String pCreatedDateTime) {
        this(pepic,false,dealRef,null,size,direction,pCreatedDateTime);
    }

    public Position(
            String pepic
            ,boolean confirmed
            , String dealRef
            , String dealId
            , double size
            , Direction direction
            , String strOpenUTM) {

        this.epic = pepic;
        this.confirmed = confirmed;
        this.dealRef = dealRef;
        this.dealId = dealId;
        this.size = size;
        this.direction = direction;
        createdTime = (strOpenUTM==null)?Instant.now(): Calculator.zonedDateTimeFromString(strOpenUTM).toInstant();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    public String getDealRef() {
        return dealRef;
    }

    public String getDealId() {
        return dealId;
    }

    public double getSize() {
        return size;
    }

    public Direction getDirection() {
        return direction;
    }

    public String getEpic() {
        return epic;
    }

    public Instant getCreatedTime() {
        return createdTime;
    }

    public void setConfirmed(boolean confirmed) {
        this.confirmed = confirmed;
    }

    public void setDealId(String dealId) {
        this.dealId = dealId;
    }

    @Override
    public String toString() {
        return "Position{" +
                "epic='" + epic + '\'' +
                ", confirmed=" + confirmed +
                ", dealRef='" + dealRef + '\'' +
                ", dealId='" + dealId + '\'' +
                ", size=" + size +
                ", direction=" + direction +
                ", createdTime=" + createdTime +
                '}';
    }
}
