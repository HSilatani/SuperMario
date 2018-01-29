package com.dario.agenttrader.dto;

import java.math.BigDecimal;

public class MarketInfo {


    private BigDecimal minNormalStopLimitDistance = null;
    private BigDecimal minDealSize = null;
    private String marketName = null;

    public MarketInfo(){

    }


    public BigDecimal getMinNormalStopLimitDistance() {
        return minNormalStopLimitDistance;
    }

    public BigDecimal getMinDealSize() {
        return minDealSize;
    }

    public String getMarketName() {
        return marketName;
    }

    public void setMinNormalStopLimitDistance(BigDecimal minNormalStopLimitDistance) {
        this.minNormalStopLimitDistance = minNormalStopLimitDistance;
    }

    public void setMinDealSize(BigDecimal minDealSize) {
        this.minDealSize = minDealSize;
    }

    public void setMarketName(String marketName) {
        this.marketName = marketName;
    }


}
