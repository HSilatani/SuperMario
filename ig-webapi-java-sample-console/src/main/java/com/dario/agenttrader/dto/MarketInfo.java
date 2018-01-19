package com.dario.agenttrader.dto;

import com.dario.agenttrader.utility.Calculator;

import java.util.Map;
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

    public MarketInfo(UpdateEvent pUpdateEvent){
        this.updateEvent = pUpdateEvent;
    }

    public String getMarketUpdateDayOpenMid() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_DAY_OPEN_MID_KEY));
    }

    public String getMarketUpdateDayLow() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_DAY_LOW_KEY));
    }

    public String getMarketUpdateDayHigh() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_DAY_HIGH_KEY));
    }

    public String getMarketUpdateLtp() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_LTP_KEY));
    }

    public String getMarketUpdateOfr() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_OFR_KEY);
    }
    public Optional<Double> getMarketUpdateOfrDouble() {
        Optional<String> offer = Optional.ofNullable(getMarketUpdateOfr());
        Optional<Double> doubleOffer = Calculator.convertStrToDouble(offer);
        return doubleOffer;
    }

    public String getMarketUpdateBid() {
        return updateEvent.getUpdates().get(MARKET_UPDATE_BID_KEY);
    }

    public Optional<Double> getMarketUpdateBidDouble() {
        return Calculator.convertStrToDouble(Optional.ofNullable(getMarketUpdateBid()));
    }

    public String getMarketUpdateDayPercChgMid() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_DAY_PERC_CHG_MID_KEY));
    }

    public String getMarketUpdateLtv() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_LTV_KEY));
    }

    public String getMarketUpdateUtm() {
        return updateEvent.getUpdates().get(UpdateEvent.MARKET_UPDATE_KEYS.get(MARKET_UPDATE_UTM_KEY));
    }
}
