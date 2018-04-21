package com.dario.agenttrader.domain;


import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.PositionsItem;

import java.math.BigDecimal;
import java.time.ZonedDateTime;

public class PositionSnapshot{

    private  PositionsItem positionsItem;
    private  BigDecimal marketMoveToday;

    BigDecimal profitLoss = null;
    ZonedDateTime snapshotTime = null;

    public PositionSnapshot(PositionsItem p){
        this.positionsItem = p;
        this.snapshotTime = ZonedDateTime.now();
    }

    public String getPositionId(){
        return positionsItem.getPosition().getDealId();
    }

    public PositionsItem getPositionsItem() {
        return positionsItem;
    }

    public BigDecimal getProfitLoss() {
        return profitLoss;
    }

    public void setProfitLoss(BigDecimal profitLoss) {
        this.profitLoss = profitLoss;
    }

    public void setPositionsItem(PositionsItem positionsItem) {
        this.positionsItem = positionsItem;
    }

    public BigDecimal getMarketMoveToday() {
        return marketMoveToday;
    }

    public void setMarketMoveToday(BigDecimal marketMoveToday) {
        this.marketMoveToday = marketMoveToday;
    }

    public ZonedDateTime getSnapshotTime() {
        return snapshotTime;
    }
}
