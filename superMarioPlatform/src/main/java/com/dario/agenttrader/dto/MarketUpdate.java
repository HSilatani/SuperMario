package com.dario.agenttrader.dto;

import org.ta4j.core.TimeSeries;

public class MarketUpdate<T> {
    private T update;
    private MarketInfo marketInfo;
    private TimeSeries timeSeries;

    public MarketUpdate(T pUpdate){
        this(pUpdate,null,null);
    }

    public MarketUpdate(T pUpdate, MarketInfo pMarketInfo) {
        this(pUpdate,pMarketInfo,null);
    }

    public MarketUpdate(T pUpdate,MarketInfo pMarketInfo,TimeSeries pTimeSeries){
        marketInfo = pMarketInfo;
        update = pUpdate;
        timeSeries = pTimeSeries;
    }

    public TimeSeries getTimeSeries() {
        return timeSeries;
    }

    public void setTimeSeries(TimeSeries timeSeries) {
        this.timeSeries = timeSeries;
    }

    public T getUpdate() {
        return update;
    }

    public MarketInfo getMarketInfo() {
        return marketInfo;
    }

    public void setMarketInfo(MarketInfo marketInfo) {
        this.marketInfo = marketInfo;
    }
}
