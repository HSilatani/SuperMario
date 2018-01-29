package com.dario.agenttrader.dto;

public class MarketUpdate<T> {
    private T update;
    private MarketInfo marketInfo;

    public MarketUpdate(T pUpdate){
        this(pUpdate,null);
    }

    public MarketUpdate(T pUpdate, MarketInfo pMarketInfo) {
        marketInfo = pMarketInfo;
        update = pUpdate;
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
