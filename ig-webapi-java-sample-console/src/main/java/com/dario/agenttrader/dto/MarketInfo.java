package com.dario.agenttrader.dto;

import com.dario.agenttrader.utility.Calculator;
import com.iggroup.webapi.samples.client.rest.dto.positions.getPositionsV2.Market;

import java.math.BigDecimal;
import java.util.Optional;

public class MarketInfo {
    public static final String MARKET_UPDATE_DAY_OPEN_MID_KEY = "MARKET_UPDATE_DAY_OPEN_MID_KEY";
    public static final String MARKET_UPDATE_DAY_LOW_KEY = "MARKET_UPDATE_DAY_LOW_KEY";
    public static final String MARKET_UPDATE_DAY_HIGH_KEY = "MARKET_UPDATE_DAY_HIGH_KEY";
    public static final String MARKET_UPDATE_LTP_KEY = "MARKET_UPDATE_LTP_KEY";
    public static final String MARKET_UPDATE_OFR_KEY = "MARKET_UPDATE_OFR_KEY";
    public static final String MARKET_UPDATE_BID_KEY = "MARKET_UPDATE_BID_KEY";
    public static final String MARKET_UPDATE_DAY_PERC_CHG_MID_KEY = "MARKET_UPDATE_DAY_PERC_CHG_MID_KEY";
    public static final String MARKET_UPDATE_LTV_KEY = "MARKET_UPDATE_LTV_KEY";
    public static final String MARKET_UPDATE_UTM_KEY = "MARKET_UPDATE_UTM_KEY";

    private UpdateEvent updateEvent;
    private BigDecimal minNormalStopLimitDistance = null;
    private BigDecimal minDealSize = null;
    private String marketName = null;

    public MarketInfo(UpdateEvent pUpdateEvent){
        this.updateEvent = pUpdateEvent;
    }

    public MarketInfo(){

    }

    public UpdateEvent getUpdateEvent() {
        return updateEvent;
    }

    public String getMarketUpdateDayOpenMid() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_DAY_OPEN_MID_KEY);
    }

    public String getMarketUpdateDayLow() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_DAY_LOW_KEY);
    }

    public String getMarketUpdateDayHigh() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_DAY_HIGH_KEY);
    }

    public String getMarketUpdateLtp() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_LTP_KEY);
    }

    public String getMarketUpdateOfr() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_OFR_KEY);
    }
    public Optional<BigDecimal> getMarketUpdateOfrBigDecimal() {
        Optional<String> offer = Optional.ofNullable(getMarketUpdateOfr());

        Optional<BigDecimal> bigDecimalOffer = Calculator.convertStrToBigDecimal(offer);
        return bigDecimalOffer;
    }

    public String getMarketUpdateBid() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_BID_KEY);
    }

    public Optional<BigDecimal> getMarketUpdateBidBigDecimal() {
        return Calculator.convertStrToBigDecimal(Optional.ofNullable(getMarketUpdateBid()));
    }

    public String getMarketUpdateDayPercChgMid() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_DAY_PERC_CHG_MID_KEY);
    }

    public String getMarketUpdateLtv() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_LTV_KEY);
    }

    public String getMarketUpdateUtm() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_UTM_KEY);
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
